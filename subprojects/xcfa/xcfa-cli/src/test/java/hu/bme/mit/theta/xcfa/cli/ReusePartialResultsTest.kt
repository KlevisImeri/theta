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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import hu.bme.mit.theta.xcfa.cli.checkers.InProcessChecker

class ReusePartialResultsTest {
  companion object {
    @JvmStatic
    fun partialResultExamples(): Stream<Arguments> {
      return Stream.of(
        // Arguments.of("/c/partialResultTest/multivar_1-1.c", true),
        // Arguments.of("/c/partialResultTest/safe-program-example-expl.c", true ),
        // Arguments.of("/c/partialResultTest/safe-program-example.c", true),
        // Arguments.of("/c/partialResultTest/unsafe-program-example.c", false),
        //
        // Arguments.of("/c/partialResultTest/large_const.c", true), // INFO: overkill for solver
        // Arguments.of("/c/partialResultTest/AllInterval-015.c", true), // INFO: TIMEOUT
        // Arguments.of("/c/partialResultTest/bresenham-ll_unwindbound10.c", "PredDefault->PredConjuncts"),
        // Arguments.of("/c/partialResultTest/bresenham-ll_valuebound1.c", true), //INFO:: long
        // partial res

        //-------------------------------------Error(verification stuck)-------------------------------------
        //  INFO: 1 ite (?ms) vs 5 ite (?ms) [good]
        // Arguments.of("/c/partialResultTest/cohendiv-ll_unwindbound1.c", "PredCartDefault->PredCartConjuncts", true)
        //  INFO: UnknownSolverStatusException
        // Arguments.of("/c/partialResultTest/cohendiv-ll_valuebound10.c", "PredBoolDefault->PredBoolConjuncts", true)
        // ----
        //  INFO: 1 ite (392ms) vs 4 ite (1487ms) [good]
        Arguments.of("/c/partialResultTest/cohendiv-ll_unwindbound2.c", "PredBoolDefault->PredBoolConjuncts", true)
        //  INFO: Full exploration to long 
        // Arguments.of("/c/partialResultTest/cohendiv-ll_unwindbound100.c", "PredBoolDefault->PredBoolConjuncts", true)
        //  INFO: Full exploration -> solver error
        // Arguments.of("/c/partialResultTest/cohendiv-ll_valuebound5.c", "PredBoolDefault->PredBoolConjuncts", true)
        //  INFO:  
        // Arguments.of("/c/partialResultTest/mannadiv_unwindbound5.c", "PredBoolDefault->PredBoolConjuncts", true)
        //----------------------------------------------------------------------------------------------------

        //  INFO:: long partial res
        // Arguments.of("/c/partialResultTest/egcd-ll_unwindbound2.c", true), 
        // Arguments.of("/c/partialResultTest/klevis.c", true),
      )
    }
  }

  @ParameterizedTest
  @MethodSource("partialResultExamples")
  fun testPartialResultsForPortfolio(cFile: String, portfolioName: String, resultType: Boolean) {
    try {
      val logger = ConsoleLogger(Logger.Level.VERBOSE)
      val uniqueLogger = UniqueWarningLogger(logger)
      
      val config = XcfaConfig<SpecFrontendConfig, SpecBackendConfig>(
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
            outputConfig = OutputConfig(),
          )
      val configWithOnlyEndNode = config.copy(
            backendConfig = config.backendConfig.copy(
              specConfig = (config.backendConfig.specConfig as PortfolioConfig).copy(
                partialResultTestOnlyEndNode = true
              )
            )
          )

      val result = runConfig(configWithOnlyEndNode, logger, uniqueLogger, throwDontExit = false);
      InProcessChecker.backendTime.clear(); //TODO: shold be put into the config as a varible -1 would meen dont touch it
      val resultWithPartial = runConfig(config, logger, uniqueLogger, throwDontExit = false);


      if (
        (result.isSafe && resultType != true) ||
        (resultWithPartial.isSafe && resultType != true)
      ) {
        throw IllegalStateException("Safety condition mismatch: Expected resultType to be true, but got false.")
      }


      val backendTime = result.getStats().get().get("backendTimeMs") as List<Long>
      val partialBackendTime = resultWithPartial.getStats().get().get("backendTimeMs") as List<Long>
      println(backendTime);
      println(partialBackendTime);

      val time = backendTime.last() as Long
      val timeOfPartialLastNode = partialBackendTime.last() as Long

      println("Algorithm Time (NoPartial): $time ms")
      println("Algorithm Time (Partial): $timeOfPartialLastNode ms")
      assert(timeOfPartialLastNode <= time)

      // println("\n\nRES: ${result.getProof().toString().replace("main::", "")}")
      println("---- + ---- + ---- + ---- + ----")
    } catch (e: Throwable) {
      println(e.stackTraceToString())
    }
  }
}

