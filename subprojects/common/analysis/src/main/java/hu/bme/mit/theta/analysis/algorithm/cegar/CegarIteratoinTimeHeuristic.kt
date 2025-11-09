package hu.bme.mit.theta.analysis.algorithm.cegar

import hu.bme.mit.theta.analysis.algorithm.predictors.ExpRLSPredictor1D
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarChecker.Companion.CegarParams

internal class PreemptiveAbortion : RuntimeException("Timeout predicted by the IteratoinTimeHeuristic ")
internal class StateSpaceExplosionException : RuntimeException("State space explosion predicted by IteratoinTimeHeuristic!")

internal class CegarIteratoinTimeHeuristic(
    val cegarParams: CegarParams,
    private val stats: CegarMetricsAccumulator,
    private val logger: Logger,
    private val expPred: ExpRLSPredictor1D = ExpRLSPredictor1D(cegarParams.forgettingFactor)
) {
    private var predIterTimeMs: Double = expPred.predict(200.0)

    fun update() {
        val realIterTimeMs = stats.iterationTimes.last().toDouble()

        expPred.update(predIterTimeMs, realIterTimeMs)

        logger.write(Logger.Level.INFO, "Predicted iteration finish at ${predIterTimeMs.toLong().msToSecStr()}s.\n")
        logger.write(Logger.Level.INFO, "Weight ${expPred.prevWeight} -> ${expPred.weight} \n")

        predIterTimeMs = expPred.predict(realIterTimeMs)
    }

    fun check() {
        if (predIterTimeMs > cegarParams.softTimeoutMs.toDouble()) {
            throw PreemptiveAbortion()
        }
    }

    fun explosionCheck() {
        if (stats.iteration < cegarParams.rlPredictorWarmup) return

        val currentTotalMs = stats.nowMs()
        val lastRealIterTimeMs = stats.iterationTimes.last().toDouble()

        expPred.update(predIterTimeMs, currentTotalMs)
        val tempPredTime = expPred.predict(currentTotalMs)

        if (tempPredTime > cegarParams.softTimeoutMs.toDouble()) {
            expPred.undoOnce()
            throw StateSpaceExplosionException()
        }
        expPred.undoOnce()
    }
}
