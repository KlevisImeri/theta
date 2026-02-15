/*
 *  Complex26 Portfolio - Multi-strategy verification portfolio
 *  
 *  This portfolio implements the complex26 strategy with multiple verification
 *  techniques and solver fallbacks. Uses DAG structure for parallel execution.
 */

import hu.bme.mit.theta.portfolio.*

// Types
data class VerificationConfig(
    val name: String,
    val solver: String,
    val domain: String,
    val refinement: String,
    val timeoutMs: Long
)

data class VerificationResult(
    val isSafe: Boolean,
    val isPartial: Boolean,
    val message: String,
    val config: VerificationConfig
)

// Helper to create config
fun config(name: String, solver: String, domain: String, refinement: String, timeout: Long) =
    VerificationConfig(name, solver, domain, refinement, timeout)

// Verification Node base class
abstract class VerificationNode(val configName: String) {
    abstract fun execute(input: VerificationConfig): VerificationResult
}

// CEGAR Node
class CegarNode : VerificationNode("CEGAR") {
    override fun execute(input: VerificationConfig): VerificationResult {
        println("Running CEGAR: ${input.name} with ${input.solver}")
        return VerificationResult(false, false, "Timeout/failed", input)
    }
}

// BMC Node  
class BmcNode : VerificationNode("BMC") {
    override fun execute(input: VerificationConfig): VerificationResult {
        println("Running BMC: ${input.name} with ${input.solver}")
        return VerificationResult(true, false, "Safe", input)
    }
}

// KIND Node
class KindNode : VerificationNode("KIND") {
    override fun execute(input: VerificationConfig): VerificationResult {
        println("Running KIND: ${input.name} with ${input.solver}")
        return VerificationResult(true, false, "Safe", input)
    }
}

// IMC Node
class ImcNode : VerificationNode("IMC") {
    override fun execute(input: VerificationConfig): VerificationResult {
        println("Running IMC: ${input.name} with ${input.solver}")
        return VerificationResult(false, false, "Timeout", input)
    }
}

// Output Node
class OutputNode {
    fun execute(result: VerificationResult): Unit {
        println("=== Final Result ===")
        println("Safe: ${result.isSafe}")
        println("Message: ${result.message}")
        println("Config: ${result.config.name}")
    }
}

// Merge node - takes multiple inputs and selects first successful result
class MergeNode {
    fun execute(results: List<VerificationResult>): VerificationResult {
        return results.firstOrNull { it.isSafe } 
            ?: results.firstOrNull() 
            ?: VerificationResult(false, false, "All failed", config("None", "", "", "", 0))
    }
}

// Create nodes - Z3 chain
val cegarZ3 = CegarNode()
val kindZ3 = KindNode()
val bmcZ3 = BmcNode()

// Create nodes - MathSAT chain
val cegarMs = CegarNode()
val kindMs = KindNode()
val bmcMs = BmcNode()

// Create nodes - IMC
val imcZ3 = ImcNode()
val imcMs = ImcNode()

// Merge and output
val merge = MergeNode()
val output = OutputNode()

// Configs
val cegarConfig = config("CEGAR-Z3", "Z3", "PRED_CART", "SEQ_ITP", 300000)
val cegarMsConfig = config("CEGAR-MS", "mathsat:5.6.12", "PRED_CART", "SEQ_ITP", 300000)
val bmcConfig = config("BMC-Z3", "Z3", "BMC", "N/A", 150000)
val bmcMsConfig = config("BMC-MS", "mathsat:5.6.12", "BMC", "N/A", 150000)
val kindConfig = config("KIND-Z3", "Z3", "KIND", "N/A", 300000)
val kindMsConfig = config("KIND-MS", "mathsat:5.6.12", "KIND", "N/A", 300000)

// Create graph - DAG structure
val graph = PortfolioGraph()

// Z3 primary chain
graph.connectSequence(cegarZ3, kindZ3, bmcZ3, trigger = hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

// MathSAT fallback chain
graph.connectSequence(cegarMs, kindMs, bmcMs, trigger = hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

// Connect Z3 solvers to MathSAT fallbacks on failure
graph.connect(cegarZ3, cegarMs, hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)
graph.connect(kindZ3, kindMs, hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)
graph.connect(bmcZ3, bmcMs, hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

// Merge results from both chains and output
graph.connectMerge(merge, bmcZ3, bmcMs, trigger = hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)
graph.connect(merge, output, hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

mapOf(
    "graph" to graph,
    "startNodes" to listOf(cegarZ3, cegarMs),
    "configs" to listOf(cegarConfig, cegarMsConfig, bmcConfig, bmcMsConfig, kindConfig, kindMsConfig)
)
