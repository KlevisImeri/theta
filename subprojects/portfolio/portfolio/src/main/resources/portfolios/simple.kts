/*
 *  Simple example portfolio demonstrating the PortfolioGraph DAG architecture
 *  
 *  This portfolio shows a DAG where one node can output to multiple targets,
 *  and multiple nodes can feed into a merge node - similar to Blender's node system.
 */

import hu.bme.mit.theta.portfolio.*

// Define types for this portfolio
data class XcfaConfig(val name: String, val timeout: Long)
data class VerificationResult(val isSafe: Boolean, val message: String)

// Define node classes
class CegarNode {
    fun execute(input: XcfaConfig): VerificationResult {
        println("Running CEGAR: ${input.name} (timeout: ${input.timeout}ms)")
        return VerificationResult(false, "Cegar timeout/failed")
    }
}

class BmcNode {
    fun execute(input: XcfaConfig): VerificationResult {
        println("Running BMC: ${input.name} (timeout: ${input.timeout}ms)")
        return VerificationResult(true, "BMC succeeded")
    }
}

class KindNode {
    fun execute(input: XcfaConfig): VerificationResult {
        println("Running KIND: ${input.name} (timeout: ${input.timeout}ms)")
        return VerificationResult(true, "KIND succeeded")
    }
}

// Merge node - takes multiple inputs and returns first successful result
class MergeNode {
    fun execute(results: List<VerificationResult>): VerificationResult {
        return results.firstOrNull { it.isSafe } 
            ?: results.firstOrNull() 
            ?: VerificationResult(false, "All failed")
    }
}

class OutputNode {
    fun execute(result: VerificationResult): Unit {
        println("Final result: ${result.message}, Safe: ${result.isSafe}")
    }
}

// Create node instances
val cegar = CegarNode()
val bmc = BmcNode()
val kind = KindNode()
val merge = MergeNode()
val output = OutputNode()

// Create the graph - DAG structure
val graph = PortfolioGraph()

// BROADCAST: CEGAR outputs to both BMC and KIND (parallel execution)
graph.connectBroadcast(cegar, bmc, kind, trigger = hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

// MERGE: Both BMC and KIND feed into merge node
graph.connectMerge(merge, bmc, kind, trigger = hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

// Output the result
graph.connect(merge, output, hu.bme.mit.theta.portfolio.EdgeTrigger.AnyError)

mapOf(
    "graph" to graph,
    "entryNodes" to listOf(cegar),
    "exitNodes" to listOf(output)
)
