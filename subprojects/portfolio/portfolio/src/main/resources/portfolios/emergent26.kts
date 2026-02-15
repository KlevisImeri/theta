/*
 *  Emergent26 Portfolio - Adaptive verification strategy
 *  
 *  Similar to complex26 but with different default solvers and
 *  more aggressive use of IC3 and MDD.
 */

import hu.bme.mit.theta.portfolio.*

// Types
data class EmergentConfig(val solver: String, val domain: String, val timeoutMs: Long)
data class EmergentResult(val isSafe: Boolean, val message: String)

// Node classes
class CegarNode {
    fun execute(config: EmergentConfig): EmergentResult {
        println("Running CEGAR: ${config.solver} (${config.domain})")
        return EmergentResult(false, "Timeout")
    }
}

class Ic3Node {
    fun execute(config: EmergentConfig): EmergentResult {
        println("Running IC3: ${config.solver}")
        return EmergentResult(true, "Safe")
    }
}

class Ic3CegarNode {
    fun execute(config: EmergentConfig): EmergentResult {
        println("Running IC3-CEGAR: ${config.solver}")
        return EmergentResult(false, "Timeout")
    }
}

class MddNode {
    fun execute(config: EmergentConfig): EmergentResult {
        println("Running MDD: ${config.solver}")
        return EmergentResult(true, "Safe")
    }
}

class MddCegarNode {
    fun execute(config: EmergentConfig): EmergentResult {
        println("Running MDD-CEGAR: ${config.solver}")
        return EmergentResult(false, "Timeout")
    }
}

class OutputNode {
    fun execute(result: EmergentResult): Unit {
        println("=== Emergent Result ===")
        println("Safe: ${result.isSafe}")
        println("Message: ${result.message}")
    }
}

// Nodes
val cegarMs = CegarNode()
val cegarZ3 = CegarNode()
val ic3Ms = Ic3Node()
val ic3Z3 = Ic3Node()
val ic3CegarMs = Ic3CegarNode()
val ic3CegarZ3 = Ic3CegarNode()
val mddMs = MddNode()
val mddZ3 = MddNode()
val mddCegarMs = MddCegarNode()
val mddCegarZ3 = MddCegarNode()
val output = OutputNode()

// Configs
val configMs = EmergentConfig("mathsat:5.6.12", "EXPL_PRED_STMT", 200_000)
val configZ3 = EmergentConfig("Z3", "EXPL_PRED_STMT", 200_000)
val configCvc5 = EmergentConfig("cvc5:1.2.0", "EXPL_PRED_STMT", 200_000)

// Graph - MathSAT chain then Z3 chain
val graph = PortfolioGraph()

// MathSAT primary
graph.connectSequence(cegarMs, ic3Ms, mddMs, output,
    trigger = hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

// Z3 fallback
graph.connectSequence(cegarZ3, ic3Z3, mddZ3, output,
    trigger = hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

// Connect solvers
graph.connect(cegarMs, cegarZ3, hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)
graph.connect(ic3Ms, ic3Z3, hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)
graph.connect(mddMs, mddZ3, hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

mapOf(
    "graph" to graph,
    "startNode" to cegarMs,
    "configs" to listOf(configMs, configZ3, configCvc5)
)
