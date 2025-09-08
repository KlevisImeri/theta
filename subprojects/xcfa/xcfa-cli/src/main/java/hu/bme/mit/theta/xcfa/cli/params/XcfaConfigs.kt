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
package hu.bme.mit.theta.xcfa.cli.params

import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy.*
import hu.bme.mit.theta.common.logging.*
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig.ArithmeticType.efficient
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig.ArchitectureType;
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig.ArithmeticType;
import hu.bme.mit.theta.frontend.transformation.grammar.preprocess.ArithmeticTrait.*
import hu.bme.mit.theta.xcfa.cli.params.*
import hu.bme.mit.theta.xcfa.cli.params.InitPrec.*
import hu.bme.mit.theta.xcfa.cli.params.POR.*
import hu.bme.mit.theta.xcfa.cli.params.Search.*
import hu.bme.mit.theta.xcfa.model.*
import hu.bme.mit.theta.xcfa.witnesses.*
import java.io.File
import java.util.*
import java.util.stream.Stream
import hu.bme.mit.theta.xcfa.cli.checkers.InProcessChecker
import java.nio.file.Paths

object XcfaConfigs {
  fun createDefaultPortfolioConfig(cFile: String, portfolioName: String): XcfaConfig<SpecFrontendConfig, SpecBackendConfig> {
     return XcfaConfig<SpecFrontendConfig, SpecBackendConfig>(
              inputConfig = InputConfig(input = File(javaClass.getResource(cFile)!!.path)),
              debugConfig =
                DebugConfig(
                  debug = false,
                  stacktrace = true,
                  logLevel = Logger.Level.INFO,
                  argdebug = false,
                  argToFile = false,
                ),
              frontendConfig =
                FrontendConfig(
                  specConfig =
                    CFrontendConfig(architecture = ArchitectureType.ILP32)
                ),
              backendConfig =
                BackendConfig(
                  backend = Backend.PORTFOLIO,
                  specConfig = PortfolioConfig(portfolio = portfolioName),
                ),
              outputConfig = OutputConfig(
                enableOutput = true,
                resultFolder = Paths.get("./output").toFile()
              ),
            )
  }
}

