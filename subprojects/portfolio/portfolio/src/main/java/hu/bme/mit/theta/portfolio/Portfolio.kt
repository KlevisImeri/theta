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
package hu.bme.mit.theta.portfolio

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.isSubtypeOf


object NodeReflector {
    
    fun analyze(node: Any): NodeDefinition {
        val kClass = node::class
        
        val executeFun = kClass.memberFunctions
            .find { it.name == "execute" }
            ?: error("Node ${kClass.simpleName} must have an 'execute' function")
        
        val inputType = executeFun.parameters.drop(1).lastOrNull()?.type 
        val outputType = executeFun.returnType 
        val hasOutput = !isUnit(outputType)
        
        return NodeDefinition(
            name = kClass.simpleName!!,
            inputType = inputType,
            outputType = outputType,
            hasInput = inputType != null,
            hasOutput = hasOutput,
            node = node,
            executeFun = executeFun
        )
    }
    
    private fun isUnit(kType: KType): Boolean {
        return (kType.classifier as? KClass<*>) == Unit::class
    }
    
    data class NodeDefinition(
        val name: String,
        val inputType: KType?,
        val outputType: KType,
        val hasInput: Boolean,
        val hasOutput: Boolean,
        val node: Any,
        val executeFun: KFunction<*>
    )
}


fun compatible(from: NodeReflector.NodeDefinition, to: NodeReflector.NodeDefinition): Boolean {
    return from.outputType.isSubtypeOf(to.inputType!!)
}

private fun formatType(type: KType?): String {
    return type?.toString()?.replace("kotlin.", "") ?: "Unit"
}

sealed class EdgeTrigger {
    object AnyError : EdgeTrigger()
    object Timeout : EdgeTrigger()
    object SolverError : EdgeTrigger()
    data class OnException(val exceptionClass: String) : EdgeTrigger()
    data class OnErrorCode(val code: Int) : EdgeTrigger()
    data class OnResult(val predicate: (Any) -> Boolean) : EdgeTrigger()
}

data class Edge(
    val from: Node,
    val to: Node,
    val trigger: EdgeTrigger = EdgeTrigger.AnyError,
    val label: String? = null
)

class Node(
    val instance: Any,
    val definition: NodeReflector.NodeDefinition = NodeReflector.analyze(instance)
) {
    val incomingEdges = mutableListOf<Edge>()
    val outgoingEdges = mutableListOf<Edge>()
    
    val name: String get() = definition.name
    val inputType: KType? get() = definition.inputType
    val outputType: KType get() = definition.outputType
    val hasInput: Boolean get() = definition.hasInput
    val hasOutput: Boolean get() = definition.hasOutput
    
    override fun equals(other: Any?): Boolean = other is Node && other.instance === instance
    override fun hashCode(): Int = instance.hashCode()
    override fun toString(): String = name
}

class PortfolioGraph {
    private val _nodes = mutableMapOf<Any, Node>()
    
    val nodes: Collection<Node> get() = _nodes.values
    val connections: List<Edge> get() = _nodes.values.flatMap { it.outgoingEdges }
    
    fun getOrCreateNode(instance: Any): Node {
        return _nodes.getOrPut(instance) { Node(instance) }
    }
    
    fun getNode(instance: Any): Node? = _nodes[instance]
    
    fun getIncomingEdges(node: Any): List<Edge> = getNode(node)?.incomingEdges ?: emptyList()
    fun getOutgoingEdges(node: Any): List<Edge> = getNode(node)?.outgoingEdges ?: emptyList()
    
    fun connect(from: Any, to: Any, trigger: EdgeTrigger = EdgeTrigger.AnyError, label: String? = null): Edge {
        val fromNode = getOrCreateNode(from)
        val toNode = getOrCreateNode(to)
        
        val edge = Edge(fromNode, toNode, trigger, label)
        fromNode.outgoingEdges.add(edge)
        toNode.incomingEdges.add(edge)
        return edge
    }
    
    fun connectSequence(vararg nodes: Any, trigger: EdgeTrigger = EdgeTrigger.AnyError) {
        for (i in 0 until nodes.size - 1) {
            connect(nodes[i], nodes[i + 1], trigger)
        }
    }
    
    fun connectMerge(merger: Any, vararg sources: Any, trigger: EdgeTrigger = EdgeTrigger.AnyError) {
        sources.forEach { source ->
            connect(source, merger, trigger)
        }
    }
    
    fun connectBroadcast(source: Any, vararg targets: Any, trigger: EdgeTrigger = EdgeTrigger.AnyError) {
        targets.forEach { target ->
            connect(source, target, trigger)
        }
    }
    
    fun connectFallback(from: Any, vararg fallbacks: Any) {
        fallbacks.forEach { to ->
            connect(from, to, EdgeTrigger.AnyError)
        }
    }
    
    fun connectIf(condition: Any, ifTrue: Any, ifFalse: Any) {
        connect(condition, ifTrue, EdgeTrigger.OnResult { it == true })
        connect(condition, ifFalse, EdgeTrigger.OnResult { it == false })
    }
    
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        // Check for cycles
        val visited = mutableSetOf<Node>()
        val visiting = mutableSetOf<Node>()
        
        fun hasCycle(node: Node): Boolean {
            if (node in visiting) return true
            if (node in visited) return false
            
            visiting.add(node)
            
            node.outgoingEdges.forEach { edge ->
                if (hasCycle(edge.to)) return true
            }
            
            visiting.remove(node)
            visited.add(node)
            return false
        }
        
        _nodes.values.forEach { node ->
            if (hasCycle(node)) {
                errors.add("Cycle detected starting from ${node.name}")
            }
        }
        
        // Validate: every node with hasInput=true must have at least one incoming connection
        // and parent output types must be compatible with node input types
        // and parent output types must be unique (no duplicates)
        val nodesToCheck = topologicalSort().reversed()
        for (node in nodesToCheck) {
            if (!node.hasInput) continue
            
            if (node.incomingEdges.isEmpty()) {
                errors.add("Node ${node.name} requires input but has no incoming connections")
                continue
            }
            
            // Collect all parent output types
            val parentOutputTypes = node.incomingEdges.map { it.from.outputType }
            
            // Check for duplicate output types from parents
            val uniqueOutputTypes = parentOutputTypes.toSet()
            if (uniqueOutputTypes.size != parentOutputTypes.size) {
                val duplicates = parentOutputTypes.groupBy { it }.filter { it.value.size > 1 }.keys
                val dupStr = duplicates.joinToString(", ") { formatType(it) }
                errors.add(
                    "Node ${node.name} has multiple parents with the same output type ($dupStr). " +
                    "Each parent must have a unique output type."
                )
            }
            
            // Check each input type has at least one compatible parent output
            val inputType = node.inputType
            if (inputType != null) {
                val hasCompatibleInput = parentOutputTypes.any { parentType ->
                    try {
                        parentType.isSubtypeOf(inputType)
                    } catch (e: Exception) {
                        false
                    }
                }
                
                if (!hasCompatibleInput) {
                    val parentTypesStr = parentOutputTypes.joinToString(", ") { formatType(it) }
                    errors.add(
                        "Node ${node.name} input type (${formatType(inputType)}) " +
                        "is not compatible with any parent output types ($parentTypesStr). " +
                        "At least one parent must output a subtype of what ${node.name} expects."
                    )
                }
            }
        }
        
        return errors
    }
    
    fun getEntryNodes(): List<Node> = _nodes.values.filter { it.incomingEdges.isEmpty() }
    fun getExitNodes(): List<Node> = _nodes.values.filter { it.outgoingEdges.isEmpty() }
    
    fun topologicalSort(): List<Node> {
        val result = mutableListOf<Node>()
        val visited = mutableSetOf<Node>()
        val inDegree = mutableMapOf<Node, Int>()
        
        _nodes.values.forEach { node ->
            inDegree[node] = node.incomingEdges.size
        }
        
        val queue = ArrayList(inDegree.filter { it.value == 0 }.keys)
        
        while (queue.isNotEmpty()) {
            val node = queue.removeAt(0)
            if (node in visited) continue
            visited.add(node)
            result.add(node)
            
            node.outgoingEdges.forEach { edge ->
                val newDegree = (inDegree[edge.to] ?: 0) - 1
                inDegree[edge.to] = newDegree
                if (newDegree == 0) queue.add(edge.to)
            }
        }
        
        return result
    }
    
    override fun toString(): String {
        return connections.joinToString("\n") { edge ->
            "${edge.from.name} -> ${edge.to.name} [${edge.trigger}]"
        }
    }
    
    fun visualize(): String {
        val sb = StringBuilder()
        sb.append("digraph Portfolio {\n")
        sb.append("  rankdir=LR;\n")
        sb.append("  node [shape=box];\n\n")
        
        _nodes.values.forEach { node ->
            sb.append("  \"${node.name}\" [label=\"${node.name}\\n${formatType(node.inputType)} -> ${formatType(node.outputType)}\"];\n")
        }
        
        connections.forEach { edge ->
            val style = when (edge.trigger) {
                is EdgeTrigger.AnyError -> "solid"
                is EdgeTrigger.Timeout -> "dashed"
                is EdgeTrigger.SolverError -> "dotted"
                else -> "solid"
            }
            sb.append("  \"${edge.from.name}\" -> \"${edge.to.name}\" [style=$style, label=\"${edge.trigger}\"];\n")
        }
        
        sb.append("}")
        return sb.toString()
    }
    
    fun execute(): Any? {
        val sortedNodes = topologicalSort()
        val results = mutableMapOf<Node, Any?>()
        
        for (node in sortedNodes) {
            val result = try {
                executeNode(node, results)
            } catch (e: Throwable) {
                handleError(node, e, results)
            }
            results[node] = result
            
            if (!node.hasOutput && node.outgoingEdges.isEmpty()) {
                return result
            }
        }
        
        return results[sortedNodes.lastOrNull { it.hasOutput }]
    }
    
    private fun executeNode(node: Node, results: Map<Node, Any?>): Any? {
        val executeFun = node.definition.executeFun
        
        return if (!node.hasInput) {
            executeFun.call(node.instance)
        } else {
            val input = gatherInputs(node, results)
            executeFun.call(node.instance, input)
        }
    }
    
    private fun gatherInputs(node: Node, results: Map<Node, Any?>): Any? {
        val incoming = node.incomingEdges
        
        return if (incoming.isEmpty()) {
            null
        } else if (incoming.size == 1) {
            results[incoming.first().from]
        } else {
            incoming.map { results[it.from] }
        }
    }
    
    private fun handleError(node: Node, error: Throwable, results: Map<Node, Any?>): Any? {
        val fallbackEdges = node.outgoingEdges.filter { edge ->
            when (val trigger = edge.trigger) {
                is EdgeTrigger.AnyError -> true
                is EdgeTrigger.Timeout -> error is java.util.concurrent.TimeoutException
                is EdgeTrigger.SolverError -> error.message?.contains("solver", ignoreCase = true) == true
                is EdgeTrigger.OnException -> trigger.exceptionClass == error::class.simpleName
                is EdgeTrigger.OnResult -> false
                else -> false
            }
        }
        
        if (fallbackEdges.isNotEmpty()) {
            val fallbackNode = fallbackEdges.first().to
            return try {
                executeNode(fallbackNode, results)
            } catch (e: Throwable) {
                handleError(fallbackNode, e, results)
            }
        }
        
        throw error
    }
}
