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
import hu.bme.mit.theta.analysis.Trace
import hu.bme.mit.theta.analysis.algorithm.SafetyResult
import hu.bme.mit.theta.analysis.ptr.PtrState
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.xcfa.analysis.XcfaAction
import hu.bme.mit.theta.xcfa.analysis.XcfaPrec
import hu.bme.mit.theta.xcfa.analysis.XcfaState
import hu.bme.mit.theta.xcfa.cli.checkers.getCegarChecker
import hu.bme.mit.theta.xcfa.cli.params.BackendConfig
import hu.bme.mit.theta.xcfa.cli.params.CegarConfig
import hu.bme.mit.theta.xcfa.cli.utils.registerAllSolverManagers
import java.util.concurrent.TimeUnit

class CegarNode(
    private val backendConfig: BackendConfig<CegarConfig>,
    private val solverHome: String = System.getProperty("user.home") + "/.theta/solvers"
) {
    fun execute(context: XcfaContext): SafetyResult<*, *> {
        val stopwatch = Stopwatch.createStarted()
        
        registerAllSolverManagers(solverHome)
        
        val checker = getCegarChecker(
            xcfa = context.xcfa,
            mcm = context.mcm,
            parseContext = context.parseContext,
            config = createDummyConfig(backendConfig, context),
            logger = Logger.instance
        )
        
        Logger.info("Starting CEGAR verification of ${context.xcfa.name}")
        
        val cegarConfig = backendConfig.specConfig
        val result = checker.check(
            cegarConfig.abstractorConfig.domain.initPrec(
                context.xcfa,
                cegarConfig.initPrec
            )
        )
        
        Logger.result("CEGAR finished (in ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms)")
        Logger.result(result.toString())
        
        return result
    }
    
    private fun createDummyConfig(
        backendConfig: BackendConfig<CegarConfig>,
        context: XcfaContext
    ): hu.bme.mit.theta.xcfa.cli.params.XcfaConfig<*, *> {
        return hu.bme.mit.theta.xcfa.cli.params.XcfaConfig<Nothing, CegarConfig>(
            backendConfig = backendConfig
        )
    }
}
