/*
 *  Horn Portfolio - CHC (Constrained Horn Clauses) verification
 *  
 *  This portfolio runs multiple CHC solvers as fallbacks.
 */

import hu.bme.mit.theta.portfolio.*

// Types
data class HornConfig(val solver: String, val timeoutMs: Long)
data class HornResult(val isSafe: Boolean, val solver: String, val message: String)

// Node classes
class EldaricaNode {
    fun execute(config: HornConfig): HornResult {
        println("Running Eldarica: ${config.solver}")
        return HornResult(false, "eldarica", "Timeout/failed")
    }
}

class GolemNode {
    fun execute(config: HornConfig): HornResult {
        println("Running Golem: ${config.solver}")
        return HornResult(false, "golem", "Timeout/failed")
    }
}

class Z3NativeNode {
    fun execute(config: HornConfig): HornResult {
        println("Running Z3 native: ${config.solver}")
        return HornResult(true, "Z3:new", "Safe")
    }
}

class Z3Node {
    fun execute(config: HornConfig): HornResult {
        println("Running Z3: ${config.solver}")
        return HornResult(true, "z3:4.15.3", "Safe")
    }
}

class OutputNode {
    fun execute(result: HornResult): Unit {
        println("=== Horn Result ===")
        println("Solver: ${result.solver}")
        println("Safe: ${result.isSafe}")
        println("Message: ${result.message}")
    }
}

// Create nodes
val eldarica = EldaricaNode()
val golem = GolemNode()
val z3native = Z3NativeNode()
val z3 = Z3Node()
val output = OutputNode()

// Configs
val eldaricaConfig = HornConfig("eldarica:2.2", 500_000)
val golemConfig = HornConfig("golem:0.9.0", 300_000)
val z3nativeConfig = HornConfig("Z3:new", 100_000)
val z3Config = HornConfig("z3:4.15.3", 100_000)

// Create graph - sequential fallbacks
val graph = PortfolioGraph()

graph.connectSequence(eldarica, golem, z3native, z3, output, 
    trigger = hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

mapOf(
    "graph" to graph,
    "startNode" to eldarica,
    "configs" to listOf(eldaricaConfig, golemConfig, z3nativeConfig, z3Config)
)
