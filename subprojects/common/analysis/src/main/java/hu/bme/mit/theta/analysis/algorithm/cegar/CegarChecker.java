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
package hu.bme.mit.theta.analysis.algorithm.cegar;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Stopwatch;
import hu.bme.mit.theta.analysis.Cex;
import hu.bme.mit.theta.analysis.Prec;
import hu.bme.mit.theta.analysis.algorithm.Proof;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.SafetyResult;
import hu.bme.mit.theta.analysis.runtimemonitor.MonitorCheckpoint;
import hu.bme.mit.theta.analysis.utils.ProofVisualizer;
import hu.bme.mit.theta.common.Utils;
import hu.bme.mit.theta.common.exception.NotSolvableException;
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.common.logging.Logger.Level;
import hu.bme.mit.theta.common.logging.NullLogger;
import hu.bme.mit.theta.common.visualization.writer.JSONWriter;
import hu.bme.mit.theta.common.visualization.writer.WebDebuggerLogger;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Counterexample-Guided Abstraction Refinement (CEGAR) loop implementation, that uses an Abstractor
 * to explore the abstract state space and a Refiner to check counterexamples and refine them if
 * needed. It also provides certain statistics about its execution.
 */
public final class CegarChecker<P extends Prec, Pr extends Proof, C extends Cex>
        implements SafetyChecker<Pr, C, P> {

    private final Abstractor<P, Pr> abstractor;
    private final Refiner<P, Pr, C> refiner;
    private final Logger logger;
    private final Pr proof;
    private final ProofVisualizer<? super Pr> proofVisualizer;
    private final Boolean computePartialResult;

    private CegarChecker(
            final Abstractor<P, Pr> abstractor,
            final Refiner<P, Pr, C> refiner,
            final Logger logger,
            final ProofVisualizer<? super Pr> proofVisualizer,
            final Boolean computePartialResult) {
        this.abstractor = checkNotNull(abstractor);
        this.refiner = checkNotNull(refiner);
        this.logger = checkNotNull(logger);
        proof = abstractor.createProof();
        this.proofVisualizer = checkNotNull(proofVisualizer);
        this.computePartialResult = checkNotNull(computePartialResult);
    }

    public static <P extends Prec, Pr extends Proof, C extends Cex> CegarChecker<P, Pr, C> create(
            final Abstractor<P, Pr> abstractor,
            final Refiner<P, Pr, C> refiner,
            final ProofVisualizer<Pr> proofVisualizer,
            final Boolean computePartialResult) {
        return create(
                abstractor,
                refiner,
                NullLogger.getInstance(),
                proofVisualizer,
                computePartialResult);
    }

    public static <P extends Prec, Pr extends Proof, C extends Cex> CegarChecker<P, Pr, C> create(
            final Abstractor<P, Pr> abstractor,
            final Refiner<P, Pr, C> refiner,
            final Logger logger,
            final ProofVisualizer<? super Pr> proofVisualizer,
            final Boolean computePartialResult) {
        return new CegarChecker<>(
                abstractor, refiner, logger, proofVisualizer, computePartialResult);
    }

    public Pr getProof() {
        return proof;
    }

    private static class StatsHolder {
        long abstractorTime = 0;
        long refinerTime = 0;
        int iteration = 0;
    }

    @Override
    public SafetyResult<Pr, C> check(final P initPrec) {
        logger.write(Level.INFO, "Configuration: %s%n", this);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final StatsHolder statsHolder = new StatsHolder();
        final Supplier<CegarStatistics> getStats =
                () -> {
                    stopwatch.stop();
                    return new CegarStatistics(
                            stopwatch.elapsed(TimeUnit.MILLISECONDS),
                            statsHolder.abstractorTime,
                            statsHolder.refinerTime,
                            statsHolder.iteration);
                };
        RefinerResult<P, C> refinerResult = null;
        AbstractorResult abstractorResult;
        P prec = initPrec;
        WebDebuggerLogger wdl = WebDebuggerLogger.getInstance();
        try {
        do {
            statsHolder.iteration++;

            logger.write(Level.MAINSTEP, "Iteration %d%n", statsHolder.iteration);
            logger.write(Level.MAINSTEP, "| Checking abstraction...%n");
            final long abstractorStartTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            abstractorResult = abstractor.check(proof, prec);
            statsHolder.abstractorTime +=
                    stopwatch.elapsed(TimeUnit.MILLISECONDS) - abstractorStartTime;
            logger.write(
                    Level.MAINSTEP, "| Checking abstraction done, result: %s%n", abstractorResult);

            if (WebDebuggerLogger.enabled()) {
                String argGraph =
                        JSONWriter.getInstance().writeString(proofVisualizer.visualize(proof));
                String precString = prec.toString();
                wdl.addIteration(statsHolder.iteration, argGraph, precString);
            }

            if (abstractorResult.isUnsafe()) {
                try {
                    MonitorCheckpoint.Checkpoints.execute("CegarChecker.unsafeARG");
                } catch (NotSolvableException e) {
                    if (computePartialResult) {
                        logger.write(
                                Level.MAINSTEP, "----Infinit Loop Detected by CexMonitor----%n");
                        abstractor.unroll(proof, prec);
                        logger.write(Level.MAINSTEP, "Abstractor unrolled successfully!%n");
                        return SafetyResult.partial(proof, getStats.get());
                    } else {
                        throw e;
                    }
                }

                P lastPrec = prec;
                logger.write(Level.MAINSTEP, "| Refining abstraction...%n");
                final long refinerStartTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
                refinerResult = refiner.refine(proof, prec);
                statsHolder.refinerTime +=
                        stopwatch.elapsed(TimeUnit.MILLISECONDS) - refinerStartTime;
                logger.write(
                        Level.MAINSTEP, "Refining abstraction done, result: %s%n", refinerResult);

                if (refinerResult.isSpurious()) {
                    prec = refinerResult.asSpurious().getRefinedPrec();
                }

                if (lastPrec.equals(prec)) {
                    logger.write(
                            Level.MAINSTEP,
                            "! Precision did NOT change in this iteration"
                                    + System.lineSeparator());
                } else {
                    logger.write(
                            Level.MAINSTEP,
                            "! Precision DID change in this iteration" + System.lineSeparator());
                }
            }

        } while (!abstractorResult.isSafe() && !refinerResult.isUnsafe());
        } catch (RuntimeException e) {
           if (computePartialResult) {
              logger.write(
                      Level.MAINSTEP, "%n-------------Some Solver Error-------------%n");
              abstractor.unroll(proof, prec);
              logger.write(Level.MAINSTEP, "Abstractor unrolled successfully!%n");
              return SafetyResult.partial(proof, getStats.get());
          } else {
              throw e;
          }
        }

        SafetyResult<Pr, C> cegarResult = null;
        final CegarStatistics stats = getStats.get();

        assert abstractorResult.isSafe() || refinerResult.isUnsafe();

        if (abstractorResult.isSafe()) {
            cegarResult = SafetyResult.safe(proof, stats);
        } else if (refinerResult.isUnsafe()) {
            cegarResult = SafetyResult.unsafe(refinerResult.asUnsafe().getCex(), proof, stats);
        }

        assert cegarResult != null;
        logger.write(Level.RESULT, "%s%n", cegarResult);
        logger.write(Level.INFO, "%s%n", stats);
        return cegarResult;
    }

    @Override
    public String toString() {
        return Utils.lispStringBuilder(getClass().getSimpleName())
                .add(abstractor)
                .add(refiner)
                .toString();
    }
}
