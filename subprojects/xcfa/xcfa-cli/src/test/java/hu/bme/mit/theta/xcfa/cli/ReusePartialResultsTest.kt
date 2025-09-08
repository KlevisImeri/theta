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
import hu.bme.mit.theta.xcfa.cli.params.*
import hu.bme.mit.theta.termui.TermUI.red
import java.nio.file.Paths

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
        // Arguments.of("/c/partialResultTest/cohendiv-ll_valuebound10.c", "PredBoolDefault->PredBoolConjuncts", true)
        // Arguments.of("/c/partialResultTest/cohendiv-ll_unwindbound1.c", "PredCartDefault->PredCartConjuncts", true)
        // ----
        // Arguments.of("/c/partialResultTest/cohendiv-ll_unwindbound2.c", "PredBoolDefault->PredBoolConjuncts", true)
        // Arguments.of("/c/partialResultTest/cohendiv-ll_unwindbound100.c", "PredBoolDefault->PredBoolConjuncts", true)
        Arguments.of("/c/partialResultTest/cohendiv-ll_valuebound5.c", "PredBoolDefault->PredBoolConjuncts", true)
        // Arguments.of("/c/partialResultTest/test_locks_14-2.c", "ExplDefault->ExplFull", false),
        // Arguments.of("/c/partialResultTest/test_locks_15-1.c", "ExplDefault->ExplFull", false)
        // ---
        // Arguments.of("/c/partialResultTest/test_locks_14-2.c", "ExplDefault->PredCartDefault", false)
        // Arguments.of("/c/partialResultTest/test_locks_15-1.c", "ExplDefault->PredCartDefault", false)
        // Arguments.of("/c/partialResultTest/cohendiv-ll_unwindbound1.c", "ExplDefault->PredCartDefault", true)
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

      val config = XcfaConfigs.createDefaultPortfolioConfig(cFile, portfolioName)
      val configWithOnlyEndNode = config.copy(
        backendConfig = config.backendConfig.copy(
          specConfig = (config.backendConfig.specConfig as PortfolioConfig).copy(
            partialResultTestOnlyEndNode = true
          )
        ),
        outputConfig = config.outputConfig.copy(
          resultFolder = Paths.get("./outputNoPartial").toFile()
        )
      )

      val runs = 20
      val timesNoPartial = mutableListOf<Long>()
      val timesPartial = mutableListOf<Long>()

      repeat(runs) {
        val result = runConfig(configWithOnlyEndNode, logger, uniqueLogger, throwDontExit = false)
        val resultWithPartial = runConfig(config, logger, uniqueLogger, throwDontExit = false)

        if (
          (result.isSafe && resultType != true) || (result.isUnsafe && resultType != false) ||
          (resultWithPartial.isSafe && resultType != true) || (resultWithPartial.isUnsafe && resultType != false)
        ) {
          throw IllegalStateException("Safety condition mismatch: Expected resultType=$resultType")
        }

        val backendTime = result.getStats().get()["backendTimeMs"] as List<Long>
        val partialBackendTime = resultWithPartial.getStats().get()["backendTimeMs"] as List<Long>

        timesNoPartial += backendTime.last()
        timesPartial += partialBackendTime.last()
      }

      val avgNoPartial = timesNoPartial.average()
      val avgPartial = timesPartial.average()

      println("Average Time (NoPartial): $avgNoPartial ms")
      println("Average Time (Partial): $avgPartial ms")
      assert(avgPartial <= avgNoPartial)

      println("---- + ---- + ---- + ---- + ----")
    } catch (e: IllegalStateException) {
      println(red(e.message ?: ""))
      throw e
    } catch (e: AssertionError) {
      println(red(e.message ?: ""))
      throw e
    } catch (e: Throwable) {
      println(red(e.stackTraceToString()))
    }
  }
}
