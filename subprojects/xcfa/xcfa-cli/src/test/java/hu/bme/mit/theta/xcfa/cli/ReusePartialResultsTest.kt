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
package hu.bme.mit.theta.xcfa.cli

import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy.*
import hu.bme.mit.theta.common.logging.*
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig.ArithmeticType.efficient
import hu.bme.mit.theta.frontend.transformation.grammar.preprocess.ArithmeticTrait.*
import hu.bme.mit.theta.xcfa.cli.params.*
import hu.bme.mit.theta.xcfa.cli.params.Backend.CEGAR
import hu.bme.mit.theta.xcfa.cli.params.CexMonitorOptions.CHECK
import hu.bme.mit.theta.xcfa.cli.params.ConeOfInfluenceMode.NO_COI
import hu.bme.mit.theta.xcfa.cli.params.Domain.PRED_CART
import hu.bme.mit.theta.xcfa.cli.params.ExprSplitterOptions.WHOLE
import hu.bme.mit.theta.xcfa.cli.params.InitPrec.*
import hu.bme.mit.theta.xcfa.cli.params.POR.*
import hu.bme.mit.theta.xcfa.cli.params.Refinement.SEQ_ITP
import hu.bme.mit.theta.xcfa.cli.params.Search.*
import hu.bme.mit.theta.xcfa.model.*
import hu.bme.mit.theta.xcfa.witnesses.*
import java.io.File
import java.util.*
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import hu.bme.mit.theta.xcfa.passes.LbePass
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig.ArchitectureType.ILP32

class ReusePartialResultsTest {
  companion object {
    @JvmStatic
    fun partialResultExamples(): Stream<Arguments> {
      return Stream.of(
        // Arguments.of(
        //     "/c/partialResultTest/multivar_1-1.c",
        //     true
        // ),
        // Arguments.of( //WARN: overkill
        //     "/c/partialResultTest/large_const.c",
        //     true
        // ),
        Arguments.of("/c/partialResultTest/bresenham-ll_unwindbound10.c", true),
        // Arguments.of( "/c/partialResultTest/safe-program-example-expl.c", true ),
        // Arguments.of( "/c/partialResultTest/safe-program-example.c", true),
        // Arguments.of( "/c/partialResultTest/bresenham-ll_valuebound1.c", true),
        // Arguments.of(
        //     "/c/partialResultTest/unsafe-program-example.c",
        //     false
        // ),
      )
    }
  }

  @ParameterizedTest
  @MethodSource("partialResultExamples")
  fun testPartialResultsForPortfolio(cFile: String, expectSafe: Boolean) {
    try {
      val logger = ConsoleLogger(Logger.Level.VERBOSE)
      val uniqueLogger = UniqueWarningLogger(logger)
      // WebDebuggerLogger.enableWebDebuggerLogger();

      // val result = runConfig( // INFO: portfolio
      //   XcfaConfig<SpecFrontendConfig, SpecBackendConfig>(
      //       inputConfig = InputConfig(
      //           input = File(javaClass.getResource(cFile)!!.path)
      //       ),
      //       debugConfig = DebugConfig(
      //         debug = true,
      //         stacktrace = true,
      //         logLevel = Logger.Level.VERBOSE,
      //         argdebug = false,
      //         argToFile = false
      //       ),
      //       frontendConfig = FrontendConfig(
      //         specConfig = CFrontendConfig(arithmetic =
      // ArchitectureConfig.ArithmeticType.efficient),
      //       ),
      //       backendConfig = BackendConfig(
      //         backend = Backend.PORTFOLIO,
      //         specConfig = PortfolioConfig()
      //       ),
      //       outputConfig = OutputConfig(),
      //   ),
      //   logger,
      //   uniqueLogger,
      //   throwDontExit = false
      // )

      val result = runConfig( // INFO: baseConfig
          XcfaConfig(
            inputConfig = InputConfig(input = File(javaClass.getResource(cFile)!!.path)),
            debugConfig =
              DebugConfig(
                debug = true,
                stacktrace = true,
                logLevel = Logger.Level.VERBOSE,
                argdebug = false,
                argToFile = false,
              ),
            frontendConfig =
              FrontendConfig(
                lbeLevel = LbePass.LbeLevel.LBE_SEQ,
                specConfig = CFrontendConfig(
                  arithmetic = efficient,
                  architecture = ILP32
                )
              ),
            backendConfig =
              BackendConfig(
                backend = CEGAR,
                timeoutMs = 0,
                specConfig =
                  CegarConfig(
                    initPrec = EMPTY,
                    porLevel = NOPOR,
                    porRandomSeed = -1,
                    coi = NO_COI,
                    cexMonitor = CHECK,
                    abstractorConfig =
                      CegarAbstractorConfig(
                        abstractionSolver = "Z3",
                        validateAbstractionSolver = false,
                        domain = PRED_CART,
                        maxEnum = 1,
                        search = ERR,
                      ),
                    refinerConfig =
                      CegarRefinerConfig(
                        refinementSolver = "Z3",
                        validateRefinementSolver = false,
                        refinement = SEQ_ITP,
                        exprSplitter = WHOLE,
                        pruneStrategy = LAZY,
                      ),
                  ),
              ),
            outputConfig = OutputConfig(),
          ),
          logger,
          uniqueLogger,
          throwDontExit = false,
        )
      println("\n\nRES: ${result.getProof().toString().replace("main::", "")}");

      // WebDebuggerLogger.getInstance().writeToFile("./Arg.cfa");
      //
      // if (expectSafe) {
      //   Assertions.assertTrue(result.isSafe(), "Expected safe, but was $result")
      // } else {
      //   Assertions.assertTrue(result.isUnsafe(), "Expected unsafe, but was $result")
      // }

      println("✅ [$cFile] => " + if (expectSafe) "SAFE" else "UNSAFE")
    } catch (e: Throwable) {
      println(e.stackTraceToString())
    }
  }
}
