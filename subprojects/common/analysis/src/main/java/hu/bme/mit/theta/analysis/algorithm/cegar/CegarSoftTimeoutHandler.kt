package hu.bme.mit.theta.analysis.algorithm.cegar

import hu.bme.mit.theta.analysis.algorithm.cegar.CegarChecker.Companion.CegarParams
import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.schedule

internal class CegarSoftTimeoutHandler(
    val cegarParams: CegarParams
) {
    val solverInterrupted = AtomicBoolean(false)

    private val timer: Timer? = if (cegarParams.softTimeoutActive) {
        Timer(true).apply {
            schedule(cegarParams.softTimeoutMs) {
                try { 
                    solverInterrupted.set(true)
                    cegarParams.afterTimeOut()
                } finally { 
                    this.cancel() 
                }
            }
        }
    } else {
        null
    }

    fun cancel() {
        timer?.cancel()
    }
}
