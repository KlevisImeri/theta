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

import com.beust.jcommander.JCommander
import com.beust.jcommander.ParameterException
import hu.bme.mit.theta.common.logging.Logger
import java.io.File
import java.io.FileReader
import javax.script.Bindings
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings
import kotlin.system.exitProcess

private val portfoliosDir = File(
    PortfolioGraph::class.java.protectionDomain.codeSource.location.toURI()
        .resolve("../resources/portfolios/").resolve(".")
).absoluteFile

private val builtInPortfolios = mapOf(
    "complex26" to "complex26.kts",
    "horn" to "horn.kts",
    "termination" to "termination.kts",
    "emergent26" to "emergent26.kts",
    "multithread" to "multithread.kts"
)

fun loadPortfolioKts(strategy: String): Any {
    val portfolioFile = when {
        builtInPortfolios.containsKey(strategy) -> 
            File(portfoliosDir, builtInPortfolios[strategy]!!)
        File(strategy).exists() -> 
            File(strategy)
        else -> 
            error("Portfolio not found: $strategy")
    }
    
    if (!portfolioFile.exists()) {
        error("Portfolio file not found: ${portfolioFile.absolutePath}")
    }
    
    val engine: ScriptEngine = ScriptEngineManager().getEngineByExtension("kts")
        ?: error("Kotlin script engine not available")
    
    val bindings: Bindings = SimpleBindings()
    bindings["portfolioDir"] = portfoliosDir
    
    return engine.eval(FileReader(portfolioFile), bindings)
}

fun executePortfolio(strategy: String) {
    Logger.info("Loading portfolio: $strategy")
    val portfolio = loadPortfolioKts(strategy)
    Logger.info("Portfolio loaded: ${portfolio?.javaClass?.simpleName}")
}

fun main(args: Array<String>) {
    val config = PortfolioConfig()
    val jc = JCommander.newBuilder()
        .addObject(config)
        .programName("thetaport")
        .build()

    try {
        jc.parse(*args)
    } catch (e: ParameterException) {
        System.err.println("Error: ${e.message}")
        jc.usage()
        exitProcess(1)
    }

    if (config.help) {
        jc.usage()
        exitProcess(0)
    }

    val logTypes = config.logLevels.split(",").map { it.trim() }.toTypedArray()
    Logger.init(logTypes)

    Logger.mainStep("Starting portfolio execution")
    Logger.info("Input file: %s", config.input?.absolutePath)
    Logger.info("Strategy: %s", config.strategy)
    Logger.info("Log levels: %s", config.logLevels)
    
    val strategy = config.strategy ?: error("No strategy specified")
    executePortfolio(strategy)
        
    Logger.close()
    exitProcess(0)
}
