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
package hu.bme.mit.theta.xcfa.cli.checkers

import com.google.common.base.Stopwatch
import hu.bme.mit.theta.analysis.Trace
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker
import hu.bme.mit.theta.analysis.algorithm.SafetyResult
import hu.bme.mit.theta.analysis.algorithm.arg.ARG
import hu.bme.mit.theta.analysis.ptr.PtrState
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.graphsolver.patterns.constraints.MCM
import hu.bme.mit.theta.xcfa.analysis.XcfaAction
import hu.bme.mit.theta.xcfa.analysis.XcfaPrec
import hu.bme.mit.theta.xcfa.analysis.XcfaState
import hu.bme.mit.theta.xcfa.cli.params.CHCFrontendConfig
import hu.bme.mit.theta.xcfa.cli.params.InputType
import hu.bme.mit.theta.xcfa.cli.params.PortfolioConfig
import hu.bme.mit.theta.xcfa.cli.params.XcfaConfig
import hu.bme.mit.theta.xcfa.cli.portfolio.*
import hu.bme.mit.theta.xcfa.model.XCFA
import java.io.File
import java.io.FileReader
import java.util.concurrent.TimeUnit
import javax.script.Bindings
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings
import hu.bme.mit.theta.xcfa.cli.portfolio.specFromNamePortfolio
import java.nio.file.Path;

fun getPortfolioChecker(
  xcfa: XCFA,
  mcm: MCM,
  config: XcfaConfig<*, *>,
  parseContext: ParseContext,
  logger: Logger,
  uniqueLogger: Logger,
): SafetyChecker<
  ARG<XcfaState<PtrState<*>>, XcfaAction>,
  Trace<XcfaState<PtrState<*>>, XcfaAction>,
  XcfaPrec<*>,
> = SafetyChecker { _ ->
  val sw = Stopwatch.createStarted()
  val portfolioName = (config.backendConfig.specConfig as PortfolioConfig).portfolio
  val chcModels =
    if (config.frontendConfig.inputType == InputType.CHC)
      (config.frontendConfig.specConfig as CHCFrontendConfig).model
    else false

  val portfolioStm =
    when (portfolioName) {
      "STABLE",
      "COMPLEX" -> complex26(xcfa, mcm, parseContext, config, logger, uniqueLogger)

      "EMERGENT" -> emergent26(xcfa, mcm, parseContext, config, logger, uniqueLogger)

      "CHC-COMP" ->
        if (!chcModels) chcCompPortfolio25(xcfa, mcm, parseContext, config, logger, uniqueLogger)
        else chcCompPortfolioModel25(xcfa, mcm, parseContext, config, logger, uniqueLogger)

      "TESTING",
      "CHC",
      "HORN" -> hornPortfolio(xcfa, mcm, parseContext, config, logger, uniqueLogger)

      "TERMINATION" -> termination(xcfa, mcm, parseContext, config, logger, uniqueLogger)

      "MULTITHREAD" -> multithreadPortfolio(xcfa, mcm, parseContext, config, logger, uniqueLogger)

      else -> {
        if (File(portfolioName).exists()) {
          val kotlinEngine: ScriptEngine = ScriptEngineManager().getEngineByExtension("kts")
          val bindings: Bindings = SimpleBindings()
          bindings["xcfa"] = xcfa
          bindings["mcm"] = mcm
          bindings["parseContext"] = parseContext
          bindings["portfolioConfig"] = config
          bindings["logger"] = logger
          bindings["uniqueLogger"] = uniqueLogger
          kotlinEngine.eval(FileReader(portfolioName), bindings) as STM
        } else {
          try {
              specFromNamePortfolio(xcfa, mcm, parseContext, config, portfolioName, logger, uniqueLogger)
          } catch (e: Exception) {
              error("""
                  No such file or built-in config: $portfolioName
                  
                  If you are trying to write a simple config spec as a string name, the script 
                  parsing failed with the following error:
                  
                  ${e::class.simpleName}: ${e.message}
                  
                  Example valid portfolio specifications:
                  - "Cegar(EXPL,FULL) -> Cegar(EXPL,LAZY)"
                  - "Cegar(domain=EXPL, pruneStrategy=LAZY) -> Cegar(domain=PRED_BOOL, pruneStrategy=FULL)"
                  - "Cegar(PRED_BOOL,LAZY,WHOLE,2) -> Cegar(PRED_BOOL,LAZY,CONJUNCTS,2)"
                  - "Cegar(PRED_CART,LAZY,WHOLE,2) -> Cegar(PRED_CART,LAZY,CONJUNCTS,2)"
                  - ...
                  
                  Stack trace:
                  ${e.stackTrace.take(5).joinToString("\n") { "  at $it" }}
              """.trimIndent())
          }
        }
      }
    }

  val result =
    portfolioStm.execute(Logger.instance) as Pair<Pair<String, XcfaConfig<*, *>>, SafetyResult<*, *>>

  Logger.result("Config ${result.first.first} succeeded in ${sw.elapsed(TimeUnit.MILLISECONDS)} ms")
  Logger.benchmark("success-result: ${result.first.first}\n")
  result.second
    as
    SafetyResult<
      ARG<XcfaState<PtrState<*>>, XcfaAction>,
      Trace<XcfaState<PtrState<*>>, XcfaAction>,
    >?
}


