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
package hu.bme.mit.theta.xcfa.port

import com.google.common.base.Stopwatch
import hu.bme.mit.theta.cat.dsl.CatDslManager
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.graphsolver.patterns.constraints.MCM
import hu.bme.mit.theta.xcfa.XcfaProperty
import hu.bme.mit.theta.xcfa.analysis.XcfaCoiMultiThread
import hu.bme.mit.theta.xcfa.analysis.XcfaCoiSingleThread
import hu.bme.mit.theta.xcfa.cli.params.*
import hu.bme.mit.theta.xcfa.cli.utils.getXcfa
import hu.bme.mit.theta.xcfa.cli.utils.registerAllSolverManagers
import hu.bme.mit.theta.xcfa.model.XCFA
import hu.bme.mit.theta.xcfa.passes.*
import java.io.File
import java.util.concurrent.TimeUnit

class XcfaParserNode(
    private val filePath: String,
    private val inputType: InputType = InputType.C,
    private val catFilePath: String? = null
) {
    fun execute(frontendConfig: FrontendConfig<*>): XcfaContext {
        val stopwatch = Stopwatch.createStarted()
        val inputFile = File(filePath)
        
        Logger.info("Parsing the input $inputFile as $inputType")
        
        val parseContext = ParseContext()
        
        if (inputType == InputType.C) {
            val cConfig = frontendConfig.specConfig as? CFrontendConfig
            if (cConfig != null) {
                parseContext.arithmetic = cConfig.arithmetic
                parseContext.architecture = cConfig.architecture
            }
        }
        
        propagateFrontendOptions(frontendConfig)
        
        val xcfa = parseXcfa(inputFile, parseContext, frontendConfig)
        
        val mcm: MCM = if (catFilePath != null) {
            CatDslManager.createMCM(File(catFilePath))
        } else {
            emptySet()
        }
        
        val coneOfInfluence = if (parseContext.multiThreading) {
            XcfaCoiMultiThread(xcfa)
        } else {
            XcfaCoiSingleThread(xcfa)
        }
        
        Logger.benchmark("Frontend finished: ${xcfa.name} (in ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms)")
        Logger.benchmark("ParsingResult Success")
        
        return XcfaContext(
            xcfa = xcfa,
            mcm = mcm,
            parseContext = parseContext,
            property = XcfaProperty(hu.bme.mit.theta.xcfa.ErrorDetection.ERROR_LOCATION)
        )
    }
    
    private fun propagateFrontendOptions(frontendConfig: FrontendConfig<*>) {
        LbePass.defaultLevel = frontendConfig.lbeLevel
        StaticCoiPass.enabled = frontendConfig.enableStaticCoi
        DataRaceToReachabilityPass.enabled = frontendConfig.enableDataRaceToReachability
        LoopUnrollPass.UNROLL_LIMIT = frontendConfig.loopUnroll
        LoopUnrollPass.FORCE_UNROLL_LIMIT = frontendConfig.forceUnroll
        FetchExecuteWriteback.enabled = frontendConfig.enableFew
    }
    
    private fun parseXcfa(
        inputFile: File,
        parseContext: ParseContext,
        frontendConfig: FrontendConfig<*>
    ): XCFA {
        val inputConfig = InputConfig(input = inputFile)
        val tempConfig = XcfaConfig<SpecFrontendConfig, SpecBackendConfig>(
            inputConfig = inputConfig,
            frontendConfig = frontendConfig as FrontendConfig<SpecFrontendConfig>
        )
        return getXcfa(tempConfig, parseContext)
    }
}
