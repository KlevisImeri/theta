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
}
