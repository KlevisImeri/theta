/*
 *  Multithread Portfolio - Verification of concurrent programs
 *  
 *  Uses POR (Partial Order Reduction) and specific concurrent
 *  verification techniques.
 */

import hu.bme.mit.theta.portfolio.*

// Types
data class MTConfig(val solver: String, val timeoutMs: Long, val por: String)
data class MTResult(val isSafe: Boolean, val message: String)

// Node classes
class MddNode {
    fun execute(config: MTConfig): MTResult {
        println("Running MDD: ${config.solver} (POR: ${config.por})")
        return MTResult(true, "Safe")
    }
}

class BmcNode {
    fun execute(config: MTConfig): MTResult {
        println("Running BMC: ${config.solver}")
        return MTResult(true, "Safe")
    }
}

class CegarNode {
    fun execute(config: MTConfig): MTResult {
        println("Running CEGAR: ${config.solver}")
        return MTResult(false, "Timeout")
    }
}

class OutputNode {
    fun execute(result: MTResult): Unit {
        println("=== Multithread Result ===")
        println("Safe: ${result.isSafe}")
        println("Message: ${result.message}")
    }
}

// Nodes
val mddZ3 = MddNode()
val mddMs = MddNode()
val bmcZ3 = BmcNode()
val bmcMs = BmcNode()
val cegarZ3 = CegarNode()
val cegarMs = CegarNode()
val output = OutputNode()

// Configs
val configZ3 = MTConfig("Z3", 600_000, "SPOR")
val configMs = MTConfig("mathsat:5.6.12", 600_000, "SPOR")

// Graph
val graph = PortfolioGraph()

// Z3 chain
graph.connectSequence(mddZ3, bmcZ3, cegarZ3, output,
    trigger = hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

// MathSAT fallback
graph.connectSequence(mddMs, bmcMs, cegarMs, output,
    trigger = hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

// Connect fallbacks
graph.connect(mddZ3, mddMs, hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)
graph.connect(bmcZ3, bmcMs, hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

mapOf(
    "graph" to graph,
    "startNode" to mddZ3,
    "configs" to listOf(configZ3, configMs)
)
