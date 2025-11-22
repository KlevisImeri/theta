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
import hu.bme.mit.theta.ui.TUI.red
import java.nio.file.Paths
import hu.bme.mit.theta.xcfa.passes.LbePass

class ReusePartialResultsTest {
  companion object {
    @JvmStatic
    fun partialResultExamples(): Stream<Arguments> {
      return Stream.of(
        Arguments.of( 
          // "/c/partialResultTest/unsafe-program-example.c",
          // "/c/partialResultTest/functions_1-1.c",
          // "/c/partialResultTest/array_1-1.c",
          // "/c/partialResultTest/invert_string-3.c",
          // "/c/partialResultTest/sum01-2.c",
          // "/c/partialResultTest/test_locks_9.c",
          // "/c/partialResultTest/sum01-2.c",
          "/c/partialResultTest/nested_2.c",
          // "/c/partialResultTest/nested_4.c",
          // "/c/partialResultTest/simple.c",
          // "/c/partialResultTest/nested_3.c",
          // "/c/partialResultTest/KIndFail.c",
          // "/c/partialResultTest/s3_srvr_1b.cil.c",
          // "/c/partialResultTest/test_locks_13.c",
          // "/c/partialResultTest/test_locks_12.c",
          // "/c/partialResultTest/diamond_2-2.c",
          // "/c/partialResultTest/nested_1-1.c",
          // "/c/partialResultTest/Problem03_label44.c",
          // "/c/partialResultTest/cohendiv-ll_unwindbound1.c",
          // "/c/partialResultTest/cohendiv-ll_valuebound5.c",
          // "/c/partialResultTest/Problem03_label27.c",
          // "/c/partialResultTest/gr2006.c",
          "PredCart(pRes=true) -> KInd()",
          "KInd()",
          // "KInd()",
          "Expl(pRes=true) -> PredCart()",
          // "PredCart(pRes=true) -> PredCart()",
          // "PredCart(900, true) -> KInd()",
          // "PredCart(900, true, true) -> KInd()",
          // "PredCart(100, true, true) -> KInd()",
          // "Expl(100, true, true) -> PredCart()",
          false
        )
      )
    }
  }


@ParameterizedTest
  @MethodSource("partialResultExamples")
  fun testPartialResultsForPortfolio(cFile: String, portfolioName1: String,  portfolioNamePartial: String, resultType: Boolean) {
    try {
      val logger = ConsoleLogger(Logger.Level.VERBOSE)
      val uniqueLogger = UniqueWarningLogger(logger)

      val config1 = createDefaultPortfolioConfig(cFile, portfolioName1)
      val config2 = createDefaultPortfolioConfig(cFile, portfolioNamePartial)


      val runs = 1
      val timesNoPartial = mutableListOf<Long>()
      val timesPartial = mutableListOf<Long>()

      repeat(runs) {
        val result = runConfig(config1, logger, uniqueLogger, throwDontExit = false)
        val resultWithPartial = runConfig(config2, logger, uniqueLogger, throwDontExit = false)

        if (
          (result.isSafe && resultType != true) || (result.isUnsafe && resultType != false) ||
          (resultWithPartial.isSafe && resultType != true) || (resultWithPartial.isUnsafe && resultType != false)
        ) {
          throw IllegalStateException("Safety condition mismatch: Expected resultType=$resultType")
        }

        val backendTime = result.getStats().get()["backendTimesMs"] as Map<String, List<Long>>
        val partialBackendTime = resultWithPartial.getStats().get()["backendTimesMs"] as Map<String, List<Long>>

        println(backendTime)
        println(partialBackendTime)

        timesNoPartial += backendTime["inProcess"]!!.last()
        timesPartial += partialBackendTime["inProcess"]!!.last()
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



fun createDefaultPortfolioConfig(
  cFile: String, 
  portfolioName: String,
): XcfaConfig<SpecFrontendConfig, SpecBackendConfig> {
   return XcfaConfig<SpecFrontendConfig, SpecBackendConfig>(
            inputConfig = InputConfig(input = File(ReusePartialResultsTest::class.java.getResource(cFile)!!.path)),
            debugConfig =
              DebugConfig( //WARN: wont work cause you have to implement json parsing
                stacktrace = true,
                // logLevel = Logger.Level.MAINSTEP,
                // logLevel = Logger.Level.SUBSTEP,
                logLevel = Logger.Level.INFO,
                // logLevel = Logger.Level.VERBOSE,
              ),
            frontendConfig =
              FrontendConfig(
                specConfig =
                  CFrontendConfig(architecture = ArchitectureType.ILP32),
                // lbeLevel = LbePass.LbeLevel.NO_LBE // WARN:
              ),
            backendConfig =
              BackendConfig(
                backend = Backend.PORTFOLIO,
                specConfig = PortfolioConfig(portfolio = portfolioName),
              ),
            outputConfig = OutputConfig(
              enableOutput = true, // FIX: if enableOutput==false and PartialResultOutputConfig(enable=true) you get error 
              resultFolder = Paths.get("./output").toFile(),
              cOutputConfig = COutputConfig(disable=true),
              chcOutputConfig = ChcOutputConfig(disable=true),
              witnessConfig = WitnessConfig(disable=true),
              xcfaOutputConfig = XcfaOutputConfig(disable=false),
              partialResultOutputConfig = PartialResultOutputConfig(enable=false),
              argConfig = ArgConfig(disable=true)
            ),
          )
}
