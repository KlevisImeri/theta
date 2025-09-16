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
        // Arguments.of(
        //   "/c/partialResultTest/cohendiv-ll_unwindbound1.c", 
        //   "Cegar(PRED_CART,LAZY,WHOLE,2,true) -> Cegar(PRED_CART,LAZY,CONJUNCTS,2,true)",
        //   "Cegar(PRED_CART,LAZY,WHOLE,2) -> Cegar(PRED_CART,LAZY,CONJUNCTS,2,true)",
        //   true
        // )
        // Arguments.of(
        //   "/c/partialResultTest/cohendiv-ll_unwindbound1.c", 
        //   "Cegar(PRED_CART,FULL,WHOLE,false,2) -> Cegar(PRED_CART,FULL,CONJUNCTS,false,2)",
        //   "Cegar(PRED_CART,FULL,WHOLE,true,2) -> Cegar(PRED_CART,FULL,CONJUNCTS,false,2)",
        //   true
        // )
        // Arguments.of(
        //   "/c/partialResultTest/cohendiv-ll_unwindbound1.c", 
        //   "Cegar(PRED_CART,FULL,WHOLE,true,2) -> Cegar(PRED_CART,FULL,CONJUNCTS,false,2)",
        //   "Cegar(PRED_CART,FULL,WHOLE,false,2) -> Bounded()",
        //   true
        // )
        // Arguments.of(
        //   "/c/partialResultTest/cohendiv-ll_unwindbound1.c", 
        //   "Cegar(PRED_CART,FULL,WHOLE,false,2) -> Cegar(PRED_CART,FULL,CONJUNCTS,false,2)",
        //   "Cegar(EXPL,LAZY,pRes=true) -> Bounded()",
        //   true
        // )
        // ----
        // Arguments.of("/c/partialResultTest/cohendiv-ll_unwindbound2.c", "PredBoolDefault->PredBoolConjuncts", true)
        // Arguments.of("/c/partialResultTest/cohendiv-ll_unwindbound100.c", "PredBoolDefault->PredBoolConjuncts", true)
        // Arguments.of("/c/partialResultTest/cohendiv-ll_valuebound5.c", "PredBoolDefault->PredBoolConjuncts", true)
        // Arguments.of(
        //   "/c/partialResultTest/cohendiv-ll_valuebound5.c",
        //   "Cegar(PRED_BOOL,LAZY,CONJUNCTS,2,true)",
        //   "Cegar(PRED_BOOL,LAZY,WHOLE,2,tru) -> Cegar(PRED_BOOL,LAZY,CONJUNCTS,2)", 
        //   true
        // )
        // Arguments.of(
        //   "/c/partialResultTest/cohendiv-ll_valuebound5.c",
        //   "Cegar(PRED_BOOL,LAZY,WHOLE,false,2) -> Cegar(PRED_BOOL,LAZY,CONJUNCTS,false,2)",
        //   "Cegar(PRED_BOOL,LAZY,WHOLE,true,2) -> Cegar(PRED_BOOL,LAZY,CONJUNCTS,false,2)", 
        //   true
        // )

        // Arguments.of("/c/partialResultTest/test_locks_14-2.c", "ExplDefault->ExplFull", false),
        // Arguments.of("/c/partialResultTest/test_locks_15-1.c", "ExplDefault->ExplFull", false)
        // Arguments.of(
        //   "/c/partialResultTest/test_locks_15-1.c", 
        //   "Cegar(EXPL,LAZY)", 
        //   "Cegar(EXPL,FULL) -> Cegar(EXPL,LAZY)", 
        //   false
        // ),

        // Arguments.of(
        //   "/c/partialResultTest/test_locks_15-1.c", 
        //   "Cegar(EXPL,FULL) -> Cegar(PRED_CART,LAZY,WHOLE,2)", 
        //   "Cegar(EXPL,FULL,pRes=true) -> Cegar(PRED_CART,LAZY,WHOLE,2)", 
        //   false
        // ),

        // ---
        // Arguments.of("/c/partialResultTest/test_locks_14-2.c", "ExplDefault->PredCartDefault", false) //TODO: check 
        // Arguments.of("/c/partialResultTest/test_locks_15-1.c", "ExplDefault->PredCartDefault", false)
        // Arguments.of("/c/partialResultTest/cohendiv-ll_unwindbound1.c", "ExplDefault->PredCartDefault", true)
        //----------------------------------------------------------------------------------------------------

        //--------------------- TO TEST -------------
        // Arguments.of(
        //   "/c/partialResultTest/cohendiv-ll_unwindbound1.c", 
        //   "Cegar(PRED_CART,FULL,WHOLE,false,2) -> Cegar(PRED_CART,FULL,CONJUNCTS,false,2)",
        //   "Cegar(PRED_CART,FULL,WHOLE,true,2) -> Cegar(PRED_CART,FULL,CONJUNCTS,false,2)",
        //   true
        // )
        Arguments.of(
          "/c/partialResultTest/cohendiv-ll_valuebound5.c",
          "Cegar(PRED_BOOL,LAZY,WHOLE,false,2) -> Cegar(PRED_BOOL,LAZY,CONJUNCTS,false,2)",
          "Cegar(PRED_BOOL,LAZY,WHOLE,true,2) -> Cegar(PRED_BOOL,LAZY,CONJUNCTS,false,2)", 
          true
        )
        // Arguments.of(
        //   "/c/partialResultTest/test_locks_15-1.c", 
        //   "Cegar(EXPL,FULL) -> Cegar(PRED_CART,LAZY,WHOLE,false,2)", 
        //   "Cegar(EXPL,FULL,pRes=true) -> Cegar(PRED_CART,LAZY,WHOLE,false,2)", 
        //   false
        // )
        //------------------------
          
        //  INFO:: long partial res
        // Arguments.of("/c/partialResultTest/egcd-ll_unwindbound2.c", true), 
        // Arguments.of("/c/partialResultTest/klevis.c", true),
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
              DebugConfig(
                stacktrace = true,
                // logLevel = Logger.Level.MAINSTEP,
                // logLevel = Logger.Level.INFO,
                logLevel = Logger.Level.VERBOSE,
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
