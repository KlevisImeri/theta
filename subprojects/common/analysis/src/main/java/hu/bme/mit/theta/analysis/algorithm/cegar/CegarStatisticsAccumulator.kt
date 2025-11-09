package hu.bme.mit.theta.analysis.algorithm.cegar

import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.common.logging.Logger.Level
import kotlin.time.TimeMark
import kotlin.time.TimeSource


fun Long.msToSecStr(): String {
  val seconds = this / 1000.0
  return String.format("%.3f", seconds)
}

internal data class CegarStatisticsAccumulator(
    var abstractorTime: Long = 0,
    var refinerTime: Long = 0,
    var iteration: Int = 0,
    val iterationTimes: MutableList<Long> = mutableListOf(0),
    private val startMark: TimeMark = TimeSource.Monotonic.markNow(), 
) {
  fun nowMs(): Long {
    return startMark.elapsedNow().inWholeMilliseconds
  }

  fun newIterationTime(logger: Logger) { 
    val currentTotalMs = nowMs()
    iterationTimes.add(currentTotalMs);

    logger.write(
        Level.MAINSTEP, 
        "Iteration finished at ${iterationTimes.last().msToSecStr()}s! %n"
    )
  }

  fun getStats(): CegarStatistics {
    return CegarStatistics(
      nowMs(),
      abstractorTime,
      refinerTime,
      iteration
    )
  }
}
