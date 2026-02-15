/*
 *  Termination Portfolio - Termination verification
 *  
 *  Uses liveness checking (ASG-CEGAR) and bounded model checking
 *  with different solvers based on data types.
 */

import hu.bme.mit.theta.portfolio.*

// Types
data class TerminationConfig(val solver: String, val timeoutMs: Long, val domain: String)
data class TerminationResult(val isTerminating: Boolean, val message: String)

// Node classes
class LivelinessCegarNode {
    fun execute(config: TerminationConfig): TerminationResult {
        println("Running Liveliness CEGAR: ${config.solver} (${config.domain})")
        return TerminationResult(false, "Timeout")
    }
}

class BmcNode {
    fun execute(config: TerminationConfig): TerminationResult {
        println("Running BMC: ${config.solver}")
        return TerminationResult(true, "Terminating")
    }
}

class KindNode {
    fun execute(config: TerminationConfig): TerminationResult {
        println("Running KIND: ${config.solver}")
        return TerminationResult(true, "Terminating")
    }
}

class ImcNode {
    fun execute(config: TerminationConfig): TerminationResult {
        println("Running IMC: ${config.solver}")
        return TerminationResult(false, "Timeout")
    }
}

class OutputNode {
    fun execute(result: TerminationResult): Unit {
        println("=== Termination Result ===")
        println("Terminating: ${result.isTerminating}")
        println("Message: ${result.message}")
    }
}

// Nodes
val livelinessExpl = LivelinessCegarNode()
val livelinessPred = LivelinessCegarNode()
val bmc = BmcNode()
val kind = KindNode()
val imc = ImcNode()
val output = OutputNode()

// Configs
val configZ3 = TerminationConfig("Z3:4.13", 100_000, "EXPL")
val configCvc5 = TerminationConfig("cvc5:1.0.8", 100_000, "EXPL")
val configMs = TerminationConfig("mathsat:5.6.10", 300_000, "EXPL")

// Graph
val graph = PortfolioGraph()

// Sequence: Liveliness -> BMC -> KIND -> IMC
graph.connectSequence(livelinessExpl, bmc, kind, imc, output,
    trigger = hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

mapOf(
    "graph" to graph,
    "startNode" to livelinessExpl,
    "configs" to listOf(configZ3, configCvc5, configMs)
)
