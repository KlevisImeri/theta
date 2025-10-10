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
import hu.bme.mit.theta.xcfa.cli.portfolio.anyError
import java.nio.file.Paths
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import hu.bme.mit.theta.solver.smtlib.SmtLibSolverManager
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig.ArchitectureType;
import hu.bme.mit.theta.ui.TUI.lightBlue
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy.LAZY

var hardTimeoutSecDefault = 900L; 


fun specFromNamePortfolio(
    xcfa: XCFA,
    mcm: MCM,
    parseContext: ParseContext,
    portfolioConfig: XcfaConfig<*, *>,
    portfolioNameAsSpec: String,
    logger: Logger,
    uniqueLogger: Logger,
): STM {
    // timeoutSecDefault = portfolioConfig.backendConfig.softTimeoutMs / 1000L
    // hardTimeoutSecDefault = portfolioConfig.backendConfig.timeoutMs / 1000L
    // if(portfolioConfig.backendConfig.timeoutMs != 0L) {
    //   timeoutSecDefault = portfolioConfig.backendConfig.timeoutMs
    // } else {
    //   logger.write(Logger.Level.INFO, lightBlue(
    //     "[INFO] TimoutMs is 0 but we set TimoutMs=${timeoutSecDefault} for specFromNamePortfolio!\n"
    //   ));
    // }

    fun checker(config: XcfaConfig<*, *>, witness: LocationInvariants? = null) =
        runConfig(config, logger, uniqueLogger, true, witness)

 
    if(portfolioNameAsSpec.isEmpty()) error("Your portfolio name cannot be empty")
    var parts = portfolioNameAsSpec.split("->").map { it.trim() }
    
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
          Edge(configNodes[i], configNodes[i + 1], anyError)
        )
    }

    val stm = STM(configNodes.first(),edges.toSet())

    // println(stm.visualize()) // WARN: When there is only one config you cant print for some reason
    return stm 
}

fun PredCart(sec: Long = hardTimeoutSecDefault, pRes: Boolean = false, itTimeHeu:Boolean = false, hardtimeout: Long = hardTimeoutSecDefault) =
  Cegar(
    domain = PRED_CART,
    pruneStrategy = LAZY,
    sec = sec,
    pRes = pRes,
    exprSplitter = CONJUNCTS,
    maxEnum = 2,
    itTimeHeu = itTimeHeu,
    hardtimeout = hardtimeout,
  )

fun Expl(sec: Long = hardTimeoutSecDefault, pRes: Boolean = false, itTimeHeu:Boolean = false, hardtimeout: Long = hardTimeoutSecDefault) =
  Cegar(
    domain = EXPL,
    pruneStrategy = LAZY,
    sec = sec, 
    pRes = pRes,
    maxEnum = 3, // WARN: have to decide
    itTimeHeu = itTimeHeu,
    hardtimeout = hardtimeout,
  )

fun KInd(sec: Long = hardTimeoutSecDefault, pRes: Boolean = false) =
  Bounded(
    sec = sec,
    disableInterpolation = true,
    pRes = pRes,
  )


fun Cegar(
      domain: Domain = EXPL,
      pruneStrategy: PruneStrategy = LAZY,
      exprSplitter: ExprSplitterOptions = WHOLE, // NOTE: only for pred abstraction
      pRes: Boolean = true,
      maxEnum: Int = 1,
      itTimeHeu: Boolean = false,
      // TODO: decide what should be the right order
      sec: Long = hardTimeoutSecDefault,
      hardtimeout: Long = hardTimeoutSecDefault,
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
          softTimeoutMs = sec * 1000L,
          timeoutMs = hardtimeout * 1000L,
          inProcess = inProcess,
          parseInProcess = parseInProcess,
          memlimit = memlimit,
          disablePartialResult = !pRes,
          inPortfolio = true,
          specConfig = CegarConfig(
              initPrec = initPrec,
              porLevel = porLevel,
              porRandomSeed = porRandomSeed,
              coi = coi,
              iterationTimeHeuristic = itTimeHeu,
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

  fun Bounded(
    // Configuration parameters aligned with BoundedConfig structure
    maxBound: Int = 0,
    reversed: Boolean = false,
    cegar: Boolean = false,
    initPrec: InitPrec = InitPrec.EMPTY,
    hardtimeout: Long = hardTimeoutSecDefault,

    // BMC configuration
    disableBmc: Boolean = false,
    nonLfPath: Boolean = false,
    bmcSolver: String = "Z3",
    validateBmcSolver: Boolean = false,
    
    // Induction configuration
    disableInduction: Boolean = false,
    indSolver: String = "Z3",
    validateIndSolver: Boolean = false,
    indMinBound: Int = 0,
    indFrequency: Int = 1,
    
    // Interpolation configuration
    disableInterpolation: Boolean = false,
    itpSolver: String = "Z3",
    validateItpSolver: Boolean = false,
    
    // General backend configuration
    solverHome: String = SmtLibSolverManager.HOME.toAbsolutePath().toString(),
    sec: Long = hardTimeoutSecDefault,
    inProcess: Boolean = true,
    parseInProcess: Boolean = false,
    memlimit: Long = 0L,
    pRes: Boolean = true,
  ): BackendConfig<BoundedConfig> =
    BackendConfig(
        backend = Backend.BOUNDED,
        solverHome = solverHome,
        softTimeoutMs = sec * 1000L,
        timeoutMs = hardtimeout * 1000L, 
        inProcess = inProcess,
        parseInProcess = parseInProcess,
        memlimit = memlimit,
        disablePartialResult = !pRes,
        inPortfolio = true,
        specConfig = BoundedConfig(
            maxBound = maxBound,
            reversed = reversed,
            cegar = cegar,
            initPrec = initPrec,
            bmcConfig = BMCConfig(
                disable = disableBmc,
                nonLfPath = nonLfPath,
                bmcSolver = bmcSolver,
                validateBMCSolver = validateBmcSolver
            ),
            indConfig = InductionConfig(
                disable = disableInduction,
                indSolver = indSolver,
                validateIndSolver = validateIndSolver,
                indMinBound = indMinBound,
                indFreq = indFrequency
            ),
            itpConfig = InterpolationConfig(
                disable = disableInterpolation,
                itpSolver = itpSolver,
                validateItpSolver = validateItpSolver
            )
        )
    )

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
            // outputConfig = OutputConfig(
            //    resultFolder = portfolioConfig.outputConfig.resultFolder
            //                   .resolve((portfolioConfig.backendConfig.specConfig as PortfolioConfig).portfolio),
            //   enableOutput = portfolioConfig.outputConfig.enableOutput,
            //   acceptUnreliableSafe = portfolioConfig.outputConfig.acceptUnreliableSafe,
            //   cOutputConfig = COutputConfig(disable=true),
            //   chcOutputConfig = ChcOutputConfig(disable=true),
            //   witnessConfig = WitnessConfig(disable=true),
            //   xcfaOutputConfig = XcfaOutputConfig(disable=false),
            //   partialResultOutputConfig = PartialResultOutputConfig(enable=true),
            //   argConfig = ArgConfig(disable=true)
            // ),
            outputConfig = portfolioConfig.outputConfig.copy(
                resultFolder = portfolioConfig.outputConfig.resultFolder
                    .resolve((portfolioConfig.backendConfig.specConfig as PortfolioConfig).portfolio),
            ),
      )
  }
