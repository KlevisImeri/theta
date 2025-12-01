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

class StateSpaceExplosionException : RuntimeException("State space explosion predicted by heuristic")

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

    init {
        checkNotNull(abstractor)
        checkNotNull(refiner)
        checkNotNull(logger)
        checkNotNull(proofVisualizer)
        checkNotNull(cegarParams)
        if (cegarParams.softTimeoutMs > 0 && cegarParams.afterTimeOut == {}) {
          logger.write(Level.INFO, warn(
            "You have your soft timeout set but the function `afterTimeOut()`" +
            "is set to {} (doesNothing), you probably want to do something after" +
            "the timeout finishes!\n"
          ))
        }
        if(cegarParams.softTimeoutMs>0 && cegarParams.hardTimeoutMs < cegarParams.softTimeoutMs) {
          logger.write(Level.INFO, warn("You have a soft timeout but it is smaller then the hard timeout so it doesn't have any effect!\n"))
        }
        // if(cegarParams.softTimeoutMs>0 && !cegarParams.computePartialResult) {
        //    logger.write(Level.INFO, warn("2025-10-06 there was no reason to have soft timeout on if you don't compute the partialResults!\n"))
        // }
    }

    companion object {
        data class CegarParams(
            val computePartialResult: Boolean = false,
            val softTimeoutMs: Long = 0L,
            val hardTimeoutMs: Long = 750*1000L,
            val afterTimeOut: () -> Unit = {},
            val iterationTimeHeuristic: Boolean = false,
            val forgettingFactor: Double = 0.96,
            val explosionMultiplier: Int = 10,
            val getVisualizer: (Proof,Prec) -> (() -> Unit) = {_,_ -> {}} 
        )

        @JvmOverloads
        fun <P : Prec, Pr : Proof, C : Cex> create(
            abstractor: Abstractor<P, Pr>,
            refiner: Refiner<P, Pr, C>,
            logger: Logger = NullLogger.getInstance(),
            proofVisualizer: ProofVisualizer<in Pr>,
            cegarParams: CegarParams = CegarParams()
        ): CegarChecker<P, Pr, C> {
            return CegarChecker(abstractor, refiner, logger, proofVisualizer, cegarParams)
        }
    }

    private data class StatsHolder(
        var abstractorTime: Long = 0,
        var refinerTime: Long = 0,
        var iteration: Int = 0,
        val iterationTimes: MutableList<Duration> = mutableListOf()
    )

    override fun check(initPrec: P): SafetyResult<Pr, C> {
        logger.write(Level.INFO, "Configuration: %s%n", this)
        val stopwatch = Stopwatch.createStarted()
        val statsHolder = StatsHolder()

        val getStats = {
            stopwatch.stop()
            CegarStatistics(
                stopwatch.elapsed(TimeUnit.MILLISECONDS),
                statsHolder.abstractorTime,
                statsHolder.refinerTime,
                statsHolder.iteration
            )
        }

        var refinerResult: RefinerResult<P, C>? = null
        lateinit var abstractorResult: AbstractorResult
        var prec: P = initPrec
        val wdl = WebDebuggerLogger.getInstance()

        val timer = Timer(true) // WARN: needs optimization
        println("TimeoutMs In Cegar:" + cegarParams.softTimeoutMs)
        val solverInterrupted = AtomicBoolean(false)
        if (cegarParams.softTimeoutMs > 0) {
            timer.schedule(cegarParams.softTimeoutMs) {
                solverInterrupted.set(true)
                cegarParams.afterTimeOut()
            }
        }

        val startIterationTime = Clock.systemUTC().instant()
        var iterationTime = startIterationTime
        val exponentialPredictor = ExpRLSPredictor1D(
            cegarParams.forgettingFactor, 
            initialWeight= 1.7 //this value has correlation with the branching factor 
        )
        var predictedTimeMs = 200.0 // WARN: Problamatic 
        var lastPrec = prec
        var lastLastPrec = lastPrec
        val explosionCheck = {
          val now = Duration.between(startIterationTime, Clock.systemUTC().instant()).toMillis().toDouble();
          // if(statsHolder.iteration > 4  // INFO: you have to at least learn the weights a bit 
          //   //  2^8 = 256 complete states is quite small if you are using whole
          //   && now > cegarParams.explosionMultiplier*predictedTimeMs) {
          //   throw StateSpaceExplosionException();
          // }

          // logger.write(Level.INFO, "Checking for state explosion!");
          exponentialPredictor.update(predictedTimeMs, now)
          val continiousPrediction = exponentialPredictor.predict(now)
          if (continiousPrediction > cegarParams.softTimeoutMs.toDouble()) {
            exponentialPredictor.undoOnce()
            throw StateSpaceExplosionException()
          }
          exponentialPredictor.undoOnce()
        }
        
        if(GUI.enabled) { 
          GUI.start();
        }
        try {
            do {
                statsHolder.iteration++


                logger.write(Level.MAINSTEP, "Iteration %d%n", statsHolder.iteration)
                logger.write(Level.MAINSTEP, "| Checking abstraction...%n")
                val abstractorStartTime = stopwatch.elapsed(TimeUnit.MILLISECONDS)
                if(cegarParams.iterationTimeHeuristic && cegarParams.explosionMultiplier > 0) {
                  abstractorResult = abstractor.check(proof, prec, explosionCheck)
                } else {
                  abstractorResult = abstractor.check(proof, prec)
                }
                statsHolder.abstractorTime +=
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) - abstractorStartTime
                logger.write(
                    Level.MAINSTEP, "| Checking abstraction done, result: %s%n", abstractorResult
                )

                if (WebDebuggerLogger.enabled()) {
                    val argGraph =
                        JSONWriter.getInstance().writeString(proofVisualizer.visualize(proof))
                    val precString = prec.toString()
                    wdl.addIteration(statsHolder.iteration, argGraph, precString)
                }

                if (abstractorResult.isUnsafe) {
                    MonitorCheckpoint.Checkpoints.execute("CegarChecker.unsafeARG")

                    logger.write(Level.MAINSTEP, "| Refining abstraction...%n")
                    val refinerStartTime = stopwatch.elapsed(TimeUnit.MILLISECONDS)
                    refinerResult = refiner.refine(proof, prec)

                    statsHolder.refinerTime +=
                        stopwatch.elapsed(TimeUnit.MILLISECONDS) - refinerStartTime
                    logger.write(
                        Level.MAINSTEP, "Refining abstraction done, result: %s%n", refinerResult
                    )

                    if (refinerResult.isSpurious) {
                        prec = refinerResult.asSpurious().refinedPrec
                    }

                    if (lastPrec == prec) {
                        logger.write(Level.MAINSTEP,"! Precision did NOT change in this iteration%n")
                    } else {
                        logger.write(Level.MAINSTEP,"! Precision DID change in this iteration%n")
                    }
                }
                val newIterationTime = Clock.systemUTC().instant()
                val iterationTimeDiff = Duration.between(iterationTime, newIterationTime)
                logger.write(Level.MAINSTEP, "Iteration took ${iterationTimeDiff.toSeconds()}s to run! %n")
                statsHolder.iterationTimes.add(iterationTimeDiff)

                if (cegarParams.iterationTimeHeuristic) {
                    val timeSinceStartMs = Duration.between(startIterationTime, newIterationTime).toMillis().toDouble();
                    val oldWeight = exponentialPredictor.weight;
                    exponentialPredictor.update(predictedTimeMs, timeSinceStartMs)
                    logger.write(Level.INFO, 
                        "ExplPreictor: Predicted ${String.format("%.3f", predictedTimeMs / 1000.0)}s, " +
                        "Real ${String.format("%.3f", timeSinceStartMs / 1000.0)}s \n"
                    );
                    logger.write(Level.INFO, "              Weight $oldWeight -> ${exponentialPredictor.weight} \n");
                    predictedTimeMs = exponentialPredictor.predict(timeSinceStartMs)


                    if (predictedTimeMs > cegarParams.softTimeoutMs.toDouble()) {
                        logger.write(Level.MAINSTEP, "--------Iteration time heuristic predicts timeout--------%n")
                        try {
                            abstractor.unroll(proof, lastPrec)
                        } catch (e2: RuntimeException) {
                            logger.write(Level.MAINSTEP, "Could not unroll abstractor because ${e2.message} !%n")
                            throw e2
                        }
                        return SafetyResult.partial(proof, getStats())
                    }
                }
                lastLastPrec = lastPrec
                lastPrec = prec
                debug("$prec")
                iterationTime = newIterationTime

                if(GUI.enabled) {
                  val tempProof = abstractor.createProof();
                  abstractorResult = abstractor.unroll(tempProof, prec)
                  val drawfn = cegarParams.getVisualizer(tempProof, prec);
                  val mainthread = Thread.currentThread()
                  val latch = CountDownLatch(1)
                  
                  GUI.draw {
                    // rlj.text.DrawText("Your iteration is ${statsHolder.iteration}!", 10, 10, 20, Color.BLACK);
                    drawfn();
                    if(rlj.core.IsKeyPressed(Keyboard.KEY_ENTER)){
                      latch.countDown()
                    } 
                  }

                  latch.await();
                }
                
                // if(cegarParams.computePartialResult && statsHolder.iteration==1) { print("I ITERUPPTED THIS MY SELF A THE END OF THE CEGAR!"); throw AlgorithmTimeoutException("END OF LOOP");} // WARN: REMOVE

            } while (!abstractorResult.isSafe && refinerResult?.isUnsafe != true)
        } catch (e: RuntimeException) {
            if (cegarParams.computePartialResult) {
                when {
                    e is AlgorithmTimeoutException -> {
                        logger.write(Level.MAINSTEP, "%n----------Timeout Exceeded & Main Thread Interrupted (%d ms)----------%n", cegarParams.softTimeoutMs)
                    }
                    e is NotSolvableException -> {
                        logger.write(Level.MAINSTEP, "%n----Infinite Loop Detected by CexMonitor----%n")
                    }
                    e is StateSpaceExplosionException -> {
                        logger.write(Level.MAINSTEP, "%n----Iteration time heuristic predicts state space explosion----%n")
                        throw e
                        // TODO: its probably a good idea that you try another solver to unroll
                        // You also probably have to try to fin the right precision to unroll everything
                        // you need somthign like unrolling heuristic
                        // but i have to like predict with fullexloration hwo larg the grapth is becomming
                        // but its like doing full exploration to much how to do it cleverly
                        // {
                          // for worse case senario
                          // cegarParams.softTimeoutMs / averageQeryTimePerArgNode
                          // 2^x fits that time and takt that precision
                          //
                          //
                        //}
                    }
                    solverInterrupted.get() -> {
                        logger.write(Level.MAINSTEP, "%n----------Timeout Exceeded & Solver Interrupted (%d ms)----------%n", cegarParams.softTimeoutMs)
                        // throw AlgorithmTimeoutException("Timeout Exceeded: Solver Interrupted after ${cegarParams.softTimeoutMs} ms")
                    }
                    else -> {
                        logger.write(Level.MAINSTEP, "%n--------------Some Solver Error-------------%n")
                        throw e
                        // TODO: its probably a good idea that you try another solver to unroll

                        // FIX: Probably unrolling with last prec gives you not sound partial results 
                        // prec = lastLastPrec;
                    }
                }

                try {
                    logger.write(Level.MAINSTEP, "| Unrolling...%n")
                    abstractor.unroll(proof, prec)
                } catch (e2: RuntimeException) {
                    logger.write(Level.MAINSTEP, "Could not unroll abstractor because ${e2.message} !%n")
                    throw e
                }
                logger.write(Level.MAINSTEP, "| Abstractor unrolled successfully!%n")
                return SafetyResult.partial(proof, getStats())
            } else {
                throw e
            }
        } finally {
            if (cegarParams.softTimeoutMs > 0) {
                timer.cancel()
            }
        }


        lateinit var cegarResult: SafetyResult<Pr, C>
        val stats = getStats()

        assert(abstractorResult.isSafe || refinerResult?.isUnsafe == true)

        if (solverInterrupted.get()) { // WARN: this shoudl be only a temporary fix
            logger.write(Level.MAINSTEP, "%n----------Timeout Exceeded & Solver Interrupted (%d ms)----------%n", cegarParams.softTimeoutMs)
            abstractor.unroll(proof, prec)
            logger.write(Level.MAINSTEP, "Abstractor unrolled successfully!%n")
            cegarResult = SafetyResult.partial(proof, stats)
        } else if (abstractorResult.isSafe) {
            cegarResult = SafetyResult.safe(proof, stats)
        } else if (refinerResult?.isUnsafe == true) {
            cegarResult = SafetyResult.unsafe(refinerResult.asUnsafe().cex, proof, stats)
        } else {
            throw IllegalStateException("CEGAR loop terminated in an unexpected state.")
        }

        logger.write(Level.RESULT, "%s%n", cegarResult)
        logger.write(Level.INFO, "%s%n", stats)
        return cegarResult
    }

    override fun toString(): String {
        return Utils.lispStringBuilder(javaClass.simpleName)
            .add(abstractor)
            .add(refiner)
            .toString()
    }


}
