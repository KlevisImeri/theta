/*
 *  Copyright 2025 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.analysis.algorithm.cegar

import com.google.common.base.Preconditions.checkNotNull
import com.google.common.base.Stopwatch
import hu.bme.mit.theta.analysis.Cex
import hu.bme.mit.theta.analysis.Prec
import hu.bme.mit.theta.analysis.algorithm.AlgorithmTimeoutException
import hu.bme.mit.theta.analysis.algorithm.Proof
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker
import hu.bme.mit.theta.analysis.algorithm.SafetyResult
import hu.bme.mit.theta.analysis.runtimemonitor.MonitorCheckpoint
import hu.bme.mit.theta.analysis.utils.ProofVisualizer
import hu.bme.mit.theta.common.Utils
import hu.bme.mit.theta.common.exception.NotSolvableException
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.common.logging.Logger.Level
import hu.bme.mit.theta.common.logging.NullLogger
import hu.bme.mit.theta.common.visualization.writer.JSONWriter
import hu.bme.mit.theta.common.visualization.writer.WebDebuggerLogger
import java.time.Clock
import java.time.Duration
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import hu.bme.mit.theta.analysis.algorithm.predictors.ExpRLSPredictor1D
import kotlin.concurrent.schedule
import hu.bme.mit.theta.ui.TUI.warn
import hu.bme.mit.theta.ui.GUI.rlj
import hu.bme.mit.theta.ui.GUI
import hu.bme.mit.theta.ui.DEBUG
import com.raylib.java.core.Color
import com.raylib.java.core.input.Keyboard
import hu.bme.mit.theta.analysis.utils.ArgVisualizer;
import java.io.FileWriter
import hu.bme.mit.theta.common.visualization.writer.GraphvizWriter;
import hu.bme.mit.theta.analysis.algorithm.arg.ARG
import java.util.concurrent.CountDownLatch
import hu.bme.mit.theta.ui.DEBUG.debug
import kotlin.time.TimeMark
import kotlin.time.DurationUnit;

/**
 * Counterexample-Guided Abstraction Refinement (CEGAR) loop implementation, that uses an Abstractor
 * to explore the abstract state space and a Refiner to check counterexamples and refine them if
 * needed. It also provides certain statistics about its execution.
 */
class CegarChecker<P : Prec, Pr : Proof, C : Cex> private constructor(
    private val abstractor: Abstractor<P, Pr>,
    private val refiner: Refiner<P, Pr, C>,
    private val logger: Logger,
    private val proofVisualizer: ProofVisualizer<in Pr>,
    private val cegarParams: CegarParams
) : SafetyChecker<Pr, C, P> {

    val proof: Pr = abstractor.createProof()
    val softTimeoutActive = cegarParams.softTimeoutMs > 0 && (cegarParams.softTimeoutMs < cegarParams.hardTimeoutMs)

    init {
      checkNotNull(abstractor)
      checkNotNull(refiner)
      checkNotNull(logger)
      checkNotNull(proofVisualizer)
      checkNotNull(cegarParams)
      logConfigurationWarnings()
    }

    companion object {
        data class CegarParams(
            val computePartialResult: Boolean = false,
            val softTimeoutMs: Long = 900 * 1000L,
            val hardTimeoutMs: Long = 900 * 1000L,
            val afterTimeOut: () -> Unit = {},
            val iterationTimeHeuristic: Boolean = false,
            val rlPredictorWarmup: Int = 4,
            val forgettingFactor: Double = 0.96,
            val getVisualizer: (Proof, Prec) -> (() -> Unit) = { _, _ -> {} }
        ) {
            val softTimeoutActive: Boolean
                get() = softTimeoutMs > 0 && softTimeoutMs < hardTimeoutMs
        }

        @JvmOverloads
        fun <P : Prec, Pr : Proof, C : Cex> create(
            abstractor: Abstractor<P, Pr>,
            refiner: Refiner<P, Pr, C>,
            logger: Logger = NullLogger.getInstance(),
            proofVisualizer: ProofVisualizer<in Pr>,
            cegarParams: CegarParams = CegarParams()
        ): CegarChecker<P, Pr, C> {
            return CegarChecker(
              abstractor,
              refiner,
              logger,
              proofVisualizer,
              cegarParams
            )
        }
    }

    override fun check(initPrec: P): SafetyResult<Pr, C> {
        logger.write(Level.INFO, "Configuration: %s%n", this)

        var refinerResult: RefinerResult<P, C>? = null
        var abstractorResult: AbstractorResult? = null
        var partialSafetyResult: SafetyResult<Pr, C>? = null
        val newPrec: P = initPrec 
        var nowPrec: P = newPrec
        var lastPrec: P = nowPrec

        val wdl = WebDebuggerLogger.getInstance()
        val stats = CegarStatisticsAccumulator()
        val iterationTimeHeuristic = CegarIteratoinTimeHeuristic(cegarParams, stats) 
        GUI.start();

        do {
            stats.iteration++
            (abstractorResult, partialSafetyResult) = runAbstractor(proof, prec, IteratoinTimeHeuristic, stats);
            if(partialSafetyResult!=null) return partialSafetyResult;
            treat abstractorResult as not null;
            wdlAddIteration();
            if (abstractorResult.isUnsafe) {
                partialSafetyResult = executeMotnitors();
                if(partialSafetyResult!=null) return partialSafetyResult; 
            }
            if (abstractorResult.isUnsafe) {
                  (refinerResult, newPrec, partialSafetyResult) =  runRefiner(proof, nowPrec, lastPrec, stats)
                  if(partialSafetyResult!=null) return partialSafetyResult; 
            }
            drawStateSpace() 
            stats.newIterationTime()
            partialSafetyResult = checkIterationTimeHeuristic(iterationTimeHeuristic);
            if(partialSafetyResult!=null) return partialSafetyResult; 
            lastPrec = prec
            prec = newPrec
            // debug("$prec")
            // if(cegarParams.computePartialResult && stats.iteration==1) throw NotSolvableException(); // WARN: REMOVE
        } while (!abstractorResult.isSafe && refinerResult?.isUnsafe != true)

        assert(abstractorResult.isSafe || refinerResult?.isUnsafe == true)

        return createCegarResult(proof, refinerResult, stats);

    }

    private fun createCegarResult(
      proof: Pr,
      refinerResult: RefinerResult<P, C>,
      stats: CegarMetricsAccumulator
    ): SafetyResult<Pr, C> { 
        lateinit var cegarResult: SafetyResult<Pr, C>
        if (abstractorResult.isSafe) {
            cegarResult = SafetyResult.safe(proof, stats.getStats())
        } else if (refinerResult?.isUnsafe == true) {
            cegarResult = SafetyResult.unsafe(refinerResult.asUnsafe().cex, proof, stats.getStats())
        } else {
            error("CEGAR loop terminated in an unexpected state.")
        }

        logger.write(Level.RESULT, "%s%n", cegarResult)
        logger.write(Level.INFO, "%s%n", stats)
        return cegarResult
    }

    private fun unroll(prec: Prec, proof: Proof): SafetyResult { 
      try {
          logger.write(Level.MAINSTEP, "| Unrolling...%n")
          abstractor.unroll(proof, prec)
      } catch (e2: RuntimeException) {
          logger.write(Level.MAINSTEP, "Could not unroll abstractor because ${e2.message} !%n")
          throw e
      }
      logger.write(Level.MAINSTEP, "| Abstractor unrolled successfully!%n")
      return SafetyResult.partial(proof, getStats())
    }


    private fun runAbstractor(
        proof: Pr,
        prec: P,
        iterationTimeHeuristic: IteratoinTimeHeuristic,
        stats: S
    ): Pair<AbstractorResult?, SafetyResult.Partial?> { 

        logger.write(Level.MAINSTEP, "Iteration %d%n", stats.iteration)
        logger.write(Level.MAINSTEP, "| Checking abstraction...%n")
        val abstractorStartTotalTime = stats.nowMs()
        val abstractorResult = null;
        val partialSafetyResult = null;

      try { 
        abstractorResult = if (cegarParams.iterationTimeHeuristic) {
            abstractor.check(proof, prec, iterationTimeHeuristic::explosionCheck)
        } else {
            abstractor.check(proof, prec)
        }
      } catch(e: RuntimeException) { 
        partialSafetyResult = when { 
          e is StateSpaceExplosionException -> abstractor.undoOnce() abstractor.unroll(lastPrec);
          else abstractor.undoOnce() abstractor.unroll(lastPrec)
        }
      }

        stats.abstractorTime += stats.nowMs() - abstractorStartTotalTime
        logger.write(
            Level.MAINSTEP, "| Checking abstraction done, result: %s%n", result
        )

        assert(!(abstractorResult==null && partialSafetyResult==null))
        return (abstractorResult, partialSafetyResult)
    }

    private fun wdlAddIteration(proof: Pr, prec: Prec) {
      if (WebDebuggerLogger.enabled()) {
          val argGraph =
              JSONWriter.getInstance().writeString(proofVisualizer.visualize(proof))
          val precString = prec.toString()
          wdl.addIteration(stats.iteration, argGraph, precString)
      }
    }

    fun executeMotnitors(): SafetyResult.Partial? {
      try { 
        MonitorCheckpoint.Checkpoints.execute("CegarChecker.unsafeARG") 
      } catch(e: RuntimeException) { 
        return when {
          e is NotSolvableException -> unroll(prec);
          else -> unroll(prec)
        }
      }
      return null;
    }


   private fun runRefiner(
        proof: Pr,
        prec: P,
        lastPrec: P,
        stats: S 
  ): Tuple<RefinerResult<P, C>, P, SafetyResult.Partial?>{ 

        logger.write(Level.MAINSTEP, "| Refining abstraction...%n")
        val refinerStartTotalTime = stats.nowMs()

        try {
          val refinerResult: RefinerResult<P, C> = refiner.refine(proof, prec) 
        } catch (e: RuntimeException) {
            refiner.undoOnce() //if not pruned it woudl do nothing
            partialSafetyResult =  unroll(prec);
        }

        stats.refinerTime += stats.nowMs() - refinerStartTotalTime 
        logger.write(
            Level.MAINSTEP, "Refining abstraction done, result: %s%n", refinerResult
        )

        val newPrec: P = if (refinerResult.isSpurious) {
            refinerResult.asSpurious().refinedPrec
        } else {
            prec 
        }

        if (lastPrec == newPrec) {
            logger.write(Level.MAINSTEP,"! Precision did NOT change in this iteration%n")
        } else {
            logger.write(Level.MAINSTEP,"! Precision DID change in this iteration%n")
        }
        
        return Pair(refinerResult, newPrec, partialSafetyResult)
    }
    
    private fun drawStateSpace() {
        if(GUI.enabled) {
          val tempProof = abstractor.createProof();
          abstractorResult = abstractor.unroll(tempProof, prec)
          val drawfn = cegarParams.getVisualizer(tempProof, prec);
          val mainthread = Thread.currentThread()
          val latch = CountDownLatch(1)
          
          GUI.draw {
            // rlj.text.DrawText("Your iteration is ${stats.iteration}!", 10, 10, 20, Color.BLACK);
            drawfn();
            if(rlj.core.IsKeyPressed(Keyboard.KEY_ENTER)){
              latch.countDown()
            } 
          }

          latch.await();
        }
    }
    
    fun checkIterationTimeHeuristic(iterationTimeHeuristic: CegarIteratoinTimeHeuristic): SafetyResult.Partial? {
      iterationTimeHeuristic.update()
      try { 
        iterationTimeHeuristic.check()
      } catch(e: RuntimeException) {
        e is PreemptiveAbortion -> { 
          refiner.undoOnce();
          return unroll(prec);
        }
      }
      return null;
    }

    private fun logConfigurationWarnings() {
        if (cegarParams.softTimeoutMs > 0 && cegarParams.hardTimeoutMs < cegarParams.softTimeoutMs) {
            logger.write(Level.INFO, warn("Soft timeout is smaller than hard timeout, so it has no effect!\n"))
        }
         
        if (softTimeoutActive && cegarParams.afterTimeOut == null) {
            logger.write(Level.INFO, warn(
                "Soft timeout is set but afterTimeOut() is null. " +
                "You probably want to execute something when timeout finishes!\n"
            ))
        }
        
        if (softTimeoutActive && !cegarParams.computePartialResult) {
            logger.write(Level.INFO, warn(
                "Soft timeout is enabled but computePartialResult is false. " +
                "There's no reason to have soft timeout without computing partial results!\n"
            ))
        }
    }

    override fun toString(): String {
        return Utils.lispStringBuilder(javaClass.simpleName)
            .add(abstractor)
            .add(refiner)
            .add(cegarParams)
            .toString()
    }


}
