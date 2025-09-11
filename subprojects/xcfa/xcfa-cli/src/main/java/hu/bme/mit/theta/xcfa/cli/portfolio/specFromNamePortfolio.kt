/*
 *  Copyright 2025 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http:
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.xcfa.cli.portfolio

import hu.bme.mit.theta.analysis.algorithm.mdd.MddChecker
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy.*
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.common.logging.Logger.Level.RESULT
import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig.ArithmeticType.efficient
import hu.bme.mit.theta.frontend.transformation.grammar.preprocess.ArithmeticTrait
import hu.bme.mit.theta.graphsolver.patterns.constraints.MCM
import hu.bme.mit.theta.xcfa.analysis.ErrorDetection.*
import hu.bme.mit.theta.xcfa.analysis.isInlined
import hu.bme.mit.theta.xcfa.analysis.oc.AutoConflictFinderConfig.SIMPLE
import hu.bme.mit.theta.xcfa.cli.params.*
import hu.bme.mit.theta.xcfa.cli.params.Backend.*
import hu.bme.mit.theta.xcfa.cli.params.CexMonitorOptions.*
import hu.bme.mit.theta.xcfa.cli.params.ConeOfInfluenceMode.*
import hu.bme.mit.theta.xcfa.cli.params.Domain.*
import hu.bme.mit.theta.xcfa.cli.params.ExitCodes.*
import hu.bme.mit.theta.xcfa.cli.params.ExprSplitterOptions.*
import hu.bme.mit.theta.xcfa.cli.params.InitPrec
import hu.bme.mit.theta.xcfa.cli.params.InitPrec.*
import hu.bme.mit.theta.xcfa.cli.params.POR.*
import hu.bme.mit.theta.xcfa.cli.params.Refinement.*
import hu.bme.mit.theta.xcfa.cli.params.Search.*
import hu.bme.mit.theta.xcfa.cli.runConfig
import hu.bme.mit.theta.xcfa.cli.utils.LocationInvariants
import hu.bme.mit.theta.xcfa.dereferences
import hu.bme.mit.theta.xcfa.model.XCFA
import hu.bme.mit.theta.xcfa.passes.LbePass
import hu.bme.mit.theta.xcfa.passes.LoopUnrollPass
import hu.bme.mit.theta.xcfa.cli.portfolio.timeoutOrNotSolvableError
import java.nio.file.Paths
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import hu.bme.mit.theta.solver.smtlib.SmtLibSolverManager
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig.ArchitectureType;

fun specFromNamePortfolio(
    xcfa: XCFA,
    mcm: MCM,
    parseContext: ParseContext,
    portfolioConfig: XcfaConfig<*, *>,
    portfolioNameAsSpec: String,
    logger: Logger,
    uniqueLogger: Logger,
): STM {

    fun checker(config: XcfaConfig<*, *>, witness: LocationInvariants? = null) =
        runConfig(config, logger, uniqueLogger, true, witness)

 
    if(portfolioNameAsSpec.isEmpty()) error("Your portfolio name cannot be empty")
    var parts = portfolioNameAsSpec.split("->").map { it.trim() }
    // if(parts.isEmpty() || (parts.size == 1 && parts[0].isEmpty())) {
    //    parts = listOf(portfolioNameAsSpec)
    // }
    
    val baseConfig = createDefaultBaseConfig(portfolioConfig, xcfa, mcm, parseContext)
    val kotlinEngine: ScriptEngine = ScriptEngineManager().getEngineByExtension("kts")
    
    println("PARTS: $parts");
    val configNodes = parts.map { part ->
        try {
            val ktsCode = """
                import hu.bme.mit.theta.xcfa.cli.portfolio.*
                import hu.bme.mit.theta.xcfa.cli.params.*
                import hu.bme.mit.theta.xcfa.cli.params.Backend.*
                import hu.bme.mit.theta.xcfa.cli.params.Domain.*
                import hu.bme.mit.theta.xcfa.cli.params.Search.*
                import hu.bme.mit.theta.xcfa.cli.params.Refinement.*
                import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy.LAZY
                import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy.FULL
                import hu.bme.mit.theta.xcfa.cli.params.POR.*
                import hu.bme.mit.theta.xcfa.cli.params.ConeOfInfluenceMode.*
                import hu.bme.mit.theta.xcfa.cli.params.CexMonitorOptions.*
                import hu.bme.mit.theta.xcfa.cli.params.InitPrec.*
                import hu.bme.mit.theta.xcfa.cli.params.ExprSplitterOptions.*
                
                $part
            """.trimIndent()
            
            val backendConfig = kotlinEngine.eval(ktsCode) as BackendConfig<SpecBackendConfig>
            val fullConfig = baseConfig
                .withBackendConfig(backendConfig)
                .copy(
                    outputConfig = baseConfig.outputConfig.withOutputSubdirectory(part)
                )
 
            ConfigNode(
                part,
                fullConfig,
                ::checker
            )
        } catch (e: Exception) {
            error("Failed to parse configuration part '$part': ${e.message}")
        }
    }
    

    val edges = mutableListOf<Edge>()
    for(i in 0 until configNodes.size - 1) {
        edges.add(
          Edge(configNodes[i], configNodes[i + 1], timeoutOrNotSolvableError)
        )
    }

    val stm = STM(configNodes.first(),edges.toSet())
 
    // println(stm.visualize()) // WARN: When there is only one config you cant print for some reason
    return stm 
    // return  STM(ConfigNode("Klvis", baseConfig, ::checker),setOf())
}

//Example:
//Cegar(EXPL,FULL) -> Cegar(EXPL,LAZY)
//Cegar(PRED_BOOL,LAZY,WHOLE,2) -> Cegar(PRED_BOOL,LAZY,CONJUNCTS,2)
//Cegar(PRED_CART,LAZY,WHOLE,2) -> Cegar(PRED_CART,LAZY,CONJUNCTS,2)
fun Cegar(
      domain: Domain = EXPL,
      pruneStrategy: PruneStrategy = FULL,
      exprSplitter: ExprSplitterOptions = WHOLE, // NOTE: only for pred abstraction
      maxEnum: Int = 1,
      disablePartialResult: Boolean = false,

      //TODO: decide what should be the right order
      timeoutMs: Long = 5 * 60 * 1000L,
      inProcess: Boolean = true,
      parseInProcess: Boolean = false,
      memlimit: Long = 0L,

      initPrec: InitPrec = EMPTY,
      porLevel: POR = NOPOR,
      porRandomSeed: Int = 0,
      coi: ConeOfInfluenceMode = NO_COI,
      cexMonitor: CexMonitorOptions = CHECK,

      abstractionSolver: String = "Z3",
      validateAbstractionSolver: Boolean = false,
      search: Search = ERR,

      refinementSolver: String = "Z3",
      validateRefinementSolver: Boolean = false,
      refinement: Refinement = SEQ_ITP,
      solverHome: String = SmtLibSolverManager.HOME.toAbsolutePath().toString(),

  ): BackendConfig<CegarConfig> {
      return BackendConfig(
          backend = CEGAR,
          solverHome = solverHome,
          timeoutMs = timeoutMs,
          inProcess = inProcess,
          parseInProcess = parseInProcess,
          memlimit = memlimit,
          disablePartialResult = disablePartialResult,
          specConfig = CegarConfig(
              initPrec = initPrec,
              porLevel = porLevel,
              porRandomSeed = porRandomSeed,
              coi = coi,
              cexMonitor = cexMonitor,
              abstractorConfig = CegarAbstractorConfig(
                  abstractionSolver = abstractionSolver,
                  validateAbstractionSolver = validateAbstractionSolver,
                  domain = domain,
                  maxEnum = maxEnum,
                  search = search
              ),
              refinerConfig = CegarRefinerConfig(
                  refinementSolver = refinementSolver,
                  validateRefinementSolver = validateRefinementSolver,
                  exprSplitter = exprSplitter,
                  refinement = refinement,
                  pruneStrategy = pruneStrategy
              )
          )
      )
  }

  fun createDefaultBaseConfig(
    portfolioConfig: XcfaConfig<*,*>,
    xcfa: XCFA,
    mcm: MCM,
    parseContext: ParseContext,
    ): XcfaConfig<SpecFrontendConfig, SpecBackendConfig> { 
    val specConfig = portfolioConfig.frontendConfig.specConfig
    return XcfaConfig(
            inputConfig =
            InputConfig(
                input = null,
                xcfaWCtx = Triple(xcfa, mcm, parseContext),
                propertyFile = null,
                property = portfolioConfig.inputConfig.property,
            ),
            frontendConfig =
            FrontendConfig(
                lbeLevel = LbePass.level,
                loopUnroll = LoopUnrollPass.UNROLL_LIMIT,
                inputType = InputType.C,
                specConfig = CFrontendConfig(
                    architecture = if (specConfig is CFrontendConfig) {
                        specConfig.architecture
                    } else {
                        ArchitectureType.LP64 
                    }
                ),
            ),
            backendConfig =
            BackendConfig(
                backend = CEGAR,
                solverHome = portfolioConfig.backendConfig.solverHome,
                timeoutMs = 5*60*1000,
                specConfig =
                CegarConfig(
                    initPrec = EMPTY,
                    porLevel = NOPOR,
                    porRandomSeed = 0,
                    coi = NO_COI,
                    cexMonitor = CHECK,
                    abstractorConfig =
                    CegarAbstractorConfig(
                        abstractionSolver = "Z3",
                        validateAbstractionSolver = false,
                        domain = EXPL,
                        maxEnum = 1,
                        search = ERR,
                    ),
                    refinerConfig =
                    CegarRefinerConfig(
                        refinementSolver = "Z3",
                        validateRefinementSolver = false,
                        refinement = SEQ_ITP,
                        pruneStrategy = FULL,
                    ),
                ),
            ),
            outputConfig =
            OutputConfig(
                versionInfo = false,
                resultFolder = portfolioConfig.outputConfig.resultFolder
                    .resolve((portfolioConfig.backendConfig.specConfig as PortfolioConfig).portfolio),
                cOutputConfig = COutputConfig(disable = true),
                witnessConfig =
                WitnessConfig(
                    disable = false,
                    concretizerSolver = "Z3",
                    validateConcretizerSolver = false,
                    inputFileForWitness =
                    portfolioConfig.outputConfig.witnessConfig.inputFileForWitness
                        ?: portfolioConfig.inputConfig.input,
                ),
                argConfig = ArgConfig(disable = true),
                enableOutput = portfolioConfig.outputConfig.enableOutput,
                acceptUnreliableSafe = portfolioConfig.outputConfig.acceptUnreliableSafe,
                xcfaOutputConfig = XcfaOutputConfig(disable = true),
                chcOutputConfig = ChcOutputConfig(disable = true),
            ),
            debugConfig = portfolioConfig.debugConfig.copy(
                stacktrace = true,
            ),
      )
  }

