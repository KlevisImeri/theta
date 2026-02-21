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

import kotlinx.coroutines.*
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
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
        
        val inputTypes = executeFun.parameters.drop(1).map { it.type }
        val outputType = executeFun.returnType 
        val hasOutput = !isUnit(outputType)
        
        return NodeDefinition(
            name = kClass.simpleName!!,
            inputTypes = inputTypes,
            outputType = outputType,
            hasInput = inputTypes.isNotEmpty(),
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
        val inputTypes: List<KType>,
        val outputType: KType,
        val hasInput: Boolean,
        val hasOutput: Boolean,
        val node: Any,
        val executeFun: KFunction<*>
    )
}


fun compatible(from: NodeReflector.NodeDefinition, to: NodeReflector.NodeDefinition): Boolean {
    return to.inputTypes.any { inputType ->
        try {
            from.outputType.isSubtypeOf(inputType)
        } catch (e: Exception) {
            false
        }
    }
}

private fun formatType(type: KType?): String {
    return type?.toString()?.replace("kotlin.", "") ?: "Unit"
}

typealias EdgeGuard = (Any?) -> Boolean

data class Edge(
    val from: Node,
    val to: Node,
    val guard: EdgeGuard = { _: Any? -> true },
    val label: String? = null
)

class Node(
    val instance: Any,
    val definition: NodeReflector.NodeDefinition = NodeReflector.analyze(instance)
) {
    val incomingEdges = mutableListOf<Edge>()
    val outgoingEdges = mutableListOf<Edge>()
    
    val name: String get() = definition.name
    val inputTypes: List<KType> get() = definition.inputTypes
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
    
    fun connect(from: Any, to: Any, label: String? = null, guard: EdgeGuard = { _: Any? -> true }): Edge {
        val fromNode = getOrCreateNode(from)
        val toNode = getOrCreateNode(to)
        
        val edge = Edge(fromNode, toNode, guard, label)
        fromNode.outgoingEdges.add(edge)
        toNode.incomingEdges.add(edge)
        return edge
    }

    fun connectBroadcast(source: Any, vararg targets: Any, guard: EdgeGuard = { _: Any? -> true }) {
        targets.forEach { target ->
            connect(source, target, guard = guard)
        }
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
        val nodesToCheck = topologicalSort().asReversed()
        for (node in nodesToCheck) {
            if (!node.hasInput) continue
            
            if (node.incomingEdges.isEmpty()) {
                val inputTypesStr = node.inputTypes.joinToString(", ") { formatType(it) }
                errors.add(
                    "Node ${node.name} requires input types ($inputTypesStr) " +
                    "but has no incoming connections. " +
                    "Connect at least one parent node that outputs a compatible type."
                )
                continue
            }
            
            val parentOutputTypes = node.incomingEdges.map { it.from.outputType }

            val inputTypes = node.inputTypes
            if (inputTypes.isNotEmpty()) {
                if (inputTypes.size > 1) {
                    val availableParents = parentOutputTypes.toMutableList()
                    val unmatchedInputs = mutableListOf<String>()

                    for (paramType in inputTypes) {
                        val matchedIndex = availableParents.indexOfFirst { parentType ->
                            try {
                                parentType.isSubtypeOf(paramType)
                            } catch (e: Exception) {
                                false
                            }
                        }

                        if (matchedIndex >= 0) {
                            availableParents.removeAt(matchedIndex)
                        } else {
                            unmatchedInputs.add(formatType(paramType))
                        }
                    }

                    if (unmatchedInputs.isNotEmpty()) {
                        val parentTypesStr = parentOutputTypes.joinToString(", ") { formatType(it) }
                        val unmatchedStr = unmatchedInputs.joinToString(", ")
                        errors.add(
                            "Node ${node.name} has unmatched input types ($unmatchedStr). " +
                            "Available parent output types: ($parentTypesStr). " +
                            "Each input parameter must be connected to a parent with a compatible output type."
                        )
                    }
                } else {
                    val inputType = inputTypes.first()
                    if (inputType.toString().startsWith("kotlin.collections.List")) {
                        val elementType = inputType.arguments.firstOrNull()?.type
                        if (elementType != null) {
                            val hasAllCompatibleInputs = parentOutputTypes.all { parentType ->
                                try {
                                    parentType.isSubtypeOf(elementType)
                                } catch (e: Exception) {
                                    false
                                }
                            }

                            if (!hasAllCompatibleInputs || parentOutputTypes.size < 2) {
                                val parentTypesStr = parentOutputTypes.joinToString(", ") { formatType(it) }
                                errors.add(
                                    "Node ${node.name} expects List<${formatType(elementType)}>, " +
                                    "but parent nodes do not all output compatible types. " +
                                    "Parent output types: ($parentTypesStr). " +
                                    "At least two parents must output subtypes of ${formatType(elementType)}."
                                )
                            }
                        }
                    } else {
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
                                "Node ${node.name} expects input type ${formatType(inputType)}, " +
                                "but none of its parent nodes output a compatible type. " +
                                "Parent output types: ($parentTypesStr)."
                            )
                        }
                    }
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
            "${edge.from.name} -> ${edge.to.name}"
        }
    }
    
    fun visualize(): String {
        val sb = StringBuilder()
        sb.append("digraph Portfolio {\n")
        sb.append("  rankdir=LR;\n")
        sb.append("  node [shape=box];\n\n")
        
        _nodes.values.forEach { node ->
            val inputTypesStr = if (node.inputTypes.isEmpty()) "Unit" else node.inputTypes.joinToString(", ") { formatType(it) }
            sb.append("  \"${node.name}\" [label=\"${node.name}\\n($inputTypesStr) -> ${formatType(node.outputType)}\"];\n")
        }
        
        connections.forEach { edge ->
            sb.append("  \"${edge.from.name}\" -> \"${edge.to.name}\";\n")
        }
        
        sb.append("}")
        return sb.toString()
    }

    fun execute(): Unit = runBlocking {
        executeAsync()
    }

    suspend fun executeAsync(): Unit = coroutineScope {
        val nodeStates = ConcurrentHashMap<Node, NodeState>()
        
        _nodes.values.forEach { node ->
            nodeStates[node] = NodeState(
                node = node,
                todoEdges = node.incomingEdges.toMutableSet(),
                doneEdges = Collections.newSetFromMap(ConcurrentHashMap()),
                parentResults = ConcurrentHashMap(),
                executed = AtomicBoolean(false),
                dead = AtomicBoolean(false),
                result = AtomicReference(null)
            )
        }
        
        nodeStates.values.filter { it.todoEdges.isEmpty() }.forEach { state ->
            launch(Dispatchers.Default) {
                executeNodeAtomic(state, nodeStates, this@coroutineScope)
            }
        }
    }

    private suspend fun executeNodeAtomic(
        state: NodeState,
        nodeStates: ConcurrentHashMap<Node, NodeState>,
        scope: CoroutineScope
    ) {
        val node = state.node
        val input = prepareInputAtomic(state)
        val result = runNode(node, input)
        
        state.result.set(result)
        state.executed.set(true)
        
        for (edge in node.outgoingEdges) {
            val edgeTaken = try {
                edge.guard(result)
            } catch (e: Exception) {
                false
            }
            
            nodeStates.compute(edge.to) { _, targetState ->
                if (targetState == null) return@compute null
                
                if (targetState.dead.get() || targetState.executed.get()) {
                    return@compute targetState
                }
                
                val removed = targetState.todoEdges.remove(edge)
                if (!removed) {
                    return@compute targetState
                }
                
                if (edgeTaken) {
                    targetState.doneEdges.add(edge)
                    targetState.parentResults[edge] = result
                }
                
                if (targetState.todoEdges.isEmpty()) {
                    val allEdgesDone = targetState.doneEdges.size == targetState.node.incomingEdges.size
                    if (allEdgesDone) {
                        scope.launch(Dispatchers.Default) {
                            executeNodeAtomic(targetState, nodeStates, scope)
                        }
                    } else {
                        targetState.dead.set(true)
                    }
                }
                
                targetState
            }
        }
    }

    private fun prepareInputAtomic(state: NodeState): Any? {
        if (state.node.inputTypes.isEmpty()) return null
        
        val results = state.parentResults.values.toList()
        if (results.isEmpty()) return null
        
        if (results.size == 1) {
            return results.first()
        }
        
        val firstType = state.node.inputTypes.firstOrNull()?.toString() ?: return results
        val firstTypeName = firstType.substringAfterLast(".")
        val allSameType = results.all { it?.let { it::class.simpleName } == firstTypeName }
        
        return if (allSameType) results else results
    }

    private fun runNode(node: Node, input: Any?): Any? {
        return try {
            val executeFun = node.definition.executeFun
            if (!node.hasInput) {
                executeFun.call(node.instance)
            } else {
                executeFun.call(node.instance, input)
            }
        } catch (e: Throwable) {
            e
        }
    }

    data class NodeState(
        val node: Node,
        val todoEdges: MutableSet<Edge>,
        val doneEdges: MutableSet<Edge>,
        val parentResults: MutableMap<Edge, Any?>,
        val executed: AtomicBoolean,
        val dead: AtomicBoolean,
        val result: AtomicReference<Any?>
    )
}
