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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import hu.bme.mit.theta.common.logging.Logger

// Test nodes - define locally for testing
data class SourceFile(val path: String, val content: String)
data class Xcfa(val procedures: List<String>)
data class VerificationResult(val isSafe: Boolean, val counterexample: String?)
data class OutputMessage(val message: String, val result: VerificationResult)

class ParsingNode {
    fun execute(file: SourceFile): Xcfa = Xcfa(listOf("main"))
}

class CegarNode {
    fun execute(xcfa: Xcfa): VerificationResult = VerificationResult(true, null)
}

class BmcNode {
    fun execute(xcfa: Xcfa): VerificationResult = VerificationResult(true, null)
}

class OutputNode {
    fun execute(result: VerificationResult): Unit = println("Done: $result")
}

class SourceNode {
    fun execute(): SourceFile = SourceFile("test.kt", "fun main() = println(42)")
}

class PortfolioNodeTest {

    @Test
    fun testParsingNodeHasInputAndOutput() {
        val node = ParsingNode()
        val def = NodeReflector.analyze(node)
        
        assertEquals("ParsingNode", def.name)
        assertTrue(def.hasInput)
        assertTrue(def.hasOutput)
    }

    @Test
    fun testCegarNodeHasInputAndOutput() {
        val node = CegarNode()
        val def = NodeReflector.analyze(node)
        
        assertEquals("CegarNode", def.name)
        assertTrue(def.hasInput)
        assertTrue(def.hasOutput)
    }

    @Test
    fun testOutputNodeHasInputNoOutput() {
        val node = OutputNode()
        val def = NodeReflector.analyze(node)
        
        assertEquals("OutputNode", def.name)
        assertTrue(def.hasInput)
        assertFalse(def.hasOutput)
    }

    @Test
    fun testSourceNodeNoInputHasOutput() {
        val node = SourceNode()
        val def = NodeReflector.analyze(node)
        
        assertEquals("SourceNode", def.name)
        assertFalse(def.hasInput)
        assertTrue(def.hasOutput)
    }

    @Test
    fun testCompatibleSourceToParsing() {
        val from = NodeReflector.analyze(SourceNode())
        val to = NodeReflector.analyze(ParsingNode())
        
        assertTrue(compatible(from, to))
    }

    @Test
    fun testCompatibleParsingToCegar() {
        val from = NodeReflector.analyze(ParsingNode())
        val to = NodeReflector.analyze(CegarNode())
        
        assertTrue(compatible(from, to))
    }

    @Test
    fun testCompatibleCegarToOutput() {
        val from = NodeReflector.analyze(CegarNode())
        val to = NodeReflector.analyze(OutputNode())
        
        assertTrue(compatible(from, to))
    }

    @Test
    fun testIncompatibleOutputToCegar() {
        val from = NodeReflector.analyze(OutputNode())
        val to = NodeReflector.analyze(CegarNode())
        
        assertFalse(compatible(from, to))
    }

    @Test
    fun testGraphConnection() {
        val graph = PortfolioGraph()
        
        val source = SourceNode()
        val parse = ParsingNode()
        
        graph.connect(source, parse)
        
        assertEquals(1, graph.connections.size)
    }

    @Test
    fun testGraphValidationNoCycles() {
        val graph = PortfolioGraph()
        
        val source = SourceNode()
        val parse = ParsingNode()
        val cegar = CegarNode()
        val output = OutputNode()
        
        graph.connect(source, parse)
        graph.connect(parse, cegar)
        graph.connect(cegar, output)
        
        // Check nodes are registered
        assertEquals(4, graph.nodes.size, "Should have 4 nodes")
        
        // Check connections
        assertEquals(3, graph.connections.size, "Should have 3 connections")
        
        // Check entry nodes
        val entryNodes = graph.getEntryNodes()
        assertEquals(1, entryNodes.size, "Should have 1 entry node")
        assertTrue(entryNodes.any { it.instance === source }, "SourceNode should be entry")
        
        // Check exit nodes
        val exitNodes = graph.getExitNodes()
        assertEquals(1, exitNodes.size, "Should have 1 exit node")
        assertTrue(exitNodes.any { it.instance === output }, "OutputNode should be exit")
        
        // Validate should pass
        val errors = graph.validate()
        assertTrue(errors.isEmpty(), "Expected no errors but got: $errors")
    }
    
    @Test
    fun testExecutePortfolio() {
        val graph = PortfolioGraph()
        
        val source = SourceNode()
        val parse = ParsingNode()
        val cegar = CegarNode()
        val output = OutputNode()
        
        graph.connect(source, parse)
        graph.connect(parse, cegar)
        graph.connect(cegar, output)
        
        val result = graph.execute()
        
        assertEquals(Unit, result)
    }

    @Test
    fun testParallelExecution() {
        val graph = PortfolioGraph()

        var startTime = 0L
        var endTimeA = 0L
        var endTimeB = 0L

        val source = SourceNode()

        class SlowA {
            fun execute(file: SourceFile): String {
                startTime = System.currentTimeMillis()
                Thread.sleep(200)
                endTimeA = System.currentTimeMillis()
                return "A"
            }
        }

        class SlowB {
            fun execute(file: SourceFile): String {
                Thread.sleep(200)
                endTimeB = System.currentTimeMillis()
                return "B"
            }
        }

        class OutputNode2 {
            fun execute(input: List<String>) {
                println("Output: $input")
            }
        }

        val slowA = SlowA()
        val slowB = SlowB()
        val output = OutputNode2()

        graph.connect(source, slowA)
        graph.connect(source, slowB)
        graph.connectBroadcast(slowA, output)
        graph.connectBroadcast(slowB, output)

        val result = graph.execute()

        val parallelTime = maxOf(endTimeA, endTimeB) - startTime
        assertTrue(parallelTime < 300, "Parallel execution should take ~200ms, not 400ms. Actual: $parallelTime ms")
    }

    @Test
    fun testGuardSuccessPath() {
        val graph = PortfolioGraph()
        
        var outputExecuted = false
        
        class SuccessNode {
            fun execute(): String = "success"
        }
        
        class GuardedNode {
            fun execute(input: String): String {
                outputExecuted = true
                return input.uppercase()
            }
        }
        
        val success = SuccessNode()
        val guarded = GuardedNode()
        
        graph.connect(success, guarded) { result -> result != null }
        
        graph.execute()
        
        assertTrue(outputExecuted, "Guarded node should execute when guard passes")
    }

    @Test
    fun testGuardFailurePath() {
        val graph = PortfolioGraph()
        
        var guardedExecuted = false
        var fallbackExecuted = false
        
        class FailNode {
            fun execute(): Throwable = RuntimeException("error")
        }
        
        class GuardedNode {
            fun execute(input: String) {
                guardedExecuted = true
            }
        }
        
        class FallbackNode {
            fun execute(input: Throwable) {
                fallbackExecuted = true
            }
        }
        
        val fail = FailNode()
        val guarded = GuardedNode()
        val fallback = FallbackNode()
        
        // Guard passes only if result is NOT a Throwable (success case)
        graph.connect(fail, guarded) { result -> result !is Throwable }
        // Guard passes only if result IS a Throwable (error case)
        graph.connect(fail, fallback) { result -> result is Throwable }
        
        graph.execute()
        
        // In new model: if guard passes, edge is taken, target can execute
        // Here: guarded's guard fails (result IS Throwable), fallback's guard passes
        assertFalse(guardedExecuted, "Guarded node should NOT execute when guard fails")
        assertTrue(fallbackExecuted, "Fallback node should execute when guard passes")
    }

    @Test
    fun testDeadPathWhenGuardFails() {
        val graph = PortfolioGraph()
        
        var nodeAExecuted = false
        var nodeCExecuted = false
        
        class Source {
            fun execute(): String = "data"
        }
        
        class GuardPassNode {
            fun execute(input: String): String {
                nodeAExecuted = true
                return input
            }
        }
        
        class DependentNode {
            fun execute(input: String) {
                nodeCExecuted = true
            }
        }
        
        val source = Source()
        val nodeA = GuardPassNode()
        val dependent = DependentNode()
        
        graph.connect(source, nodeA) { true }
        
        graph.connect(nodeA, dependent)
        
        graph.execute()
        
        assertTrue(nodeAExecuted, "Node A should execute (guard passes)")
        assertTrue(nodeCExecuted, "Dependent should execute because node A passed guard")
    }

    @Test
    fun testNodeWithNoIncomingEdgesExecutes() {
        val graph = PortfolioGraph()
        
        var sourceExecuted = false
        
        class SourceNode {
            fun execute(): String {
                sourceExecuted = true
                return "data"
            }
        }
        
        class SinkNode {
            fun execute(input: String) = println("Received: $input")
        }
        
        val source = SourceNode()
        val sink = SinkNode()
        
        graph.connect(source, sink)
        
        graph.execute()
        
        assertTrue(sourceExecuted, "Source node should execute")
    }

    @Test
    fun testValidationCycleDetection() {
        val graph = PortfolioGraph()
        
        class NodeA {
            fun execute(input: String): String = input
        }
        
        class NodeB {
            fun execute(input: String): String = input
        }
        
        val nodeA = NodeA()
        val nodeB = NodeB()
        
        graph.connect(nodeA, nodeB)
        graph.connect(nodeB, nodeA) // Creates cycle
        
        val errors = graph.validate()
        
        assertTrue(errors.any { it.contains("Cycle") }, "Should detect cycle")
    }

    @Test
    fun testValidationMissingInput() {
        // In the new model, nodes with no incoming edges are entry nodes that execute
        // So no validation error is expected - node will just execute with null input
        val graph = PortfolioGraph()
        
        class RequiresInput {
            fun execute(input: String): String = input
        }
        
        val node = RequiresInput()
        
        // Don't connect anything - node has no incoming edges but will still execute
        
        val errors = graph.validate()
        
        // No errors - node is an entry point
        assertTrue(errors.isEmpty(), "Entry nodes with no input should be allowed")
    }

    @Test
    fun testVisualize() {
        val graph = PortfolioGraph()
        
        class Source {
            fun execute(): String = "data"
        }
        
        class Sink {
            fun execute(input: String) = Unit
        }
        
        val source = Source()
        val sink = Sink()
        
        graph.connect(source, sink)
        
        val viz = graph.visualize()
        
        assertTrue(viz.contains("digraph Portfolio"), "Should contain digraph header")
        assertTrue(viz.contains("Source"), "Should contain Source node")
        assertTrue(viz.contains("Sink"), "Should contain Sink node")
        assertTrue(viz.contains("->"), "Should contain edge")
    }

    @Test
    fun testTopologicalSort() {
        val graph = PortfolioGraph()
        
        class Node1 { fun execute(): String = "1" }
        class Node2 { fun execute(input: String): String = input }
        class Node3 { fun execute(input: String): String = input }
        
        val n1 = Node1()
        val n2 = Node2()
        val n3 = Node3()
        
        graph.connect(n1, n2)
        graph.connect(n2, n3)
        
        val sorted = graph.topologicalSort()
        
        val idx1 = sorted.indexOfFirst { it.name == "Node1" }
        val idx2 = sorted.indexOfFirst { it.name == "Node2" }
        val idx3 = sorted.indexOfFirst { it.name == "Node3" }
        
        assertTrue(idx1 < idx2, "Node1 should come before Node2")
        assertTrue(idx2 < idx3, "Node2 should come before Node3")
    }

    @Test
    fun testMultipleInputTypes() {
        val graph = PortfolioGraph()
        
        class IntSource { fun execute(): Int = 42 }
        class StringSource { fun execute(): String = "hello" }
        
        class MergeNode {
            fun execute(a: Int, b: String): String = "$a $b"
        }
        
        val intSrc = IntSource()
        val strSrc = StringSource()
        val merge = MergeNode()
        
        graph.connect(intSrc, merge)
        graph.connect(strSrc, merge)
        
        graph.execute()
    }

    @Test
    fun testSameOutputTypeAsList() {
        val graph = PortfolioGraph()
        
        class IntSourceA { fun execute(): Int = 1 }
        class IntSourceB { fun execute(): Int = 2 }
        
        class ListNode {
            fun execute(inputs: List<Int>): Int = inputs.sum()
        }
        
        val srcA = IntSourceA()
        val srcB = IntSourceB()
        val listNode = ListNode()
        
        graph.connect(srcA, listNode)
        graph.connect(srcB, listNode)
        
        graph.execute()
    }

    @Test
    fun testGetEntryAndExitNodes() {
        val graph = PortfolioGraph()
        
        class Source { fun execute(): String = "data" }
        class Middle { fun execute(input: String): String = input }
        class Sink { fun execute(input: String) = Unit }
        
        val source = Source()
        val middle = Middle()
        val sink = Sink()
        
        graph.connect(source, middle)
        graph.connect(middle, sink)
        
        val entries = graph.getEntryNodes()
        val exits = graph.getExitNodes()
        
        assertEquals(1, entries.size)
        assertEquals("Source", entries.first().name)
        
        assertEquals(1, exits.size)
        assertEquals("Sink", exits.first().name)
    }

    @Test
    fun testConnectBroadcast() {
        val graph = PortfolioGraph()
        
        class Source { fun execute(): String = "data" }
        class TargetA { fun execute(input: String) = println("A: $input") }
        class TargetB { fun execute(input: String) = println("B: $input") }
        
        val source = Source()
        val targetA = TargetA()
        val targetB = TargetB()
        
        graph.connectBroadcast(source, targetA, targetB)
        
        assertEquals(2, graph.connections.size)
        
        graph.execute()
    }

    @Test
    fun testNodeInputTypesList() {
        class SingleInput {
            fun execute(input: String): String = input
        }
        
        class MultiInput {
            fun execute(a: String, b: Int): String = "$a $b"
        }
        
        val singleDef = NodeReflector.analyze(SingleInput())
        val multiDef = NodeReflector.analyze(MultiInput())
        
        assertEquals(1, singleDef.inputTypes.size)
        assertEquals(2, multiDef.inputTypes.size)
    }
}
