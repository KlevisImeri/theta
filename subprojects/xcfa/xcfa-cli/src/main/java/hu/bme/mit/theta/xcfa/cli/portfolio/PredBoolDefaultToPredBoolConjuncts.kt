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

import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy.FULL
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy.LAZY
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.common.logging.Logger.Level.RESULT
import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig.ArithmeticType.efficient
import hu.bme.mit.theta.frontend.transformation.grammar.preprocess.ArithmeticTrait
import hu.bme.mit.theta.frontend.transformation.grammar.preprocess.ArithmeticTrait.*
import hu.bme.mit.theta.graphsolver.patterns.constraints.MCM
import hu.bme.mit.theta.xcfa.analysis.ErrorDetection.DATA_RACE
import hu.bme.mit.theta.xcfa.analysis.ErrorDetection.ERROR_LOCATION
import hu.bme.mit.theta.xcfa.analysis.isInlined
import hu.bme.mit.theta.xcfa.analysis.oc.AutoConflictFinderConfig.SIMPLE
import hu.bme.mit.theta.xcfa.cli.params.*
import hu.bme.mit.theta.xcfa.cli.params.Backend.CEGAR
import hu.bme.mit.theta.xcfa.cli.params.Backend.OC
import hu.bme.mit.theta.xcfa.cli.params.CexMonitorOptions.CHECK
import hu.bme.mit.theta.xcfa.cli.params.ConeOfInfluenceMode.COI
import hu.bme.mit.theta.xcfa.cli.params.ConeOfInfluenceMode.NO_COI
import hu.bme.mit.theta.xcfa.cli.params.Domain.EXPL
import hu.bme.mit.theta.xcfa.cli.params.Domain.PRED_BOOL
import hu.bme.mit.theta.xcfa.cli.params.ExitCodes.SERVER_ERROR
import hu.bme.mit.theta.xcfa.cli.params.ExitCodes.SOLVER_ERROR
import hu.bme.mit.theta.xcfa.cli.params.ExprSplitterOptions.WHOLE
import hu.bme.mit.theta.xcfa.cli.params.InitPrec.EMPTY
import hu.bme.mit.theta.xcfa.cli.params.POR.*
import hu.bme.mit.theta.xcfa.cli.params.Refinement.NWT_IT_WP
import hu.bme.mit.theta.xcfa.cli.params.Refinement.SEQ_ITP
import hu.bme.mit.theta.xcfa.cli.params.Search.*
import hu.bme.mit.theta.xcfa.cli.runConfig
import hu.bme.mit.theta.xcfa.cli.utils.LocationInvariants
import hu.bme.mit.theta.xcfa.dereferences
import hu.bme.mit.theta.xcfa.model.XCFA
import hu.bme.mit.theta.xcfa.passes.LbePass
import hu.bme.mit.theta.xcfa.passes.LoopUnrollPass
import java.nio.file.Paths
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig.ArchitectureType;

fun predBoolDefaultToPredBoolConjuncts(
    xcfa: XCFA,
    mcm: MCM,
    parseContext: ParseContext,
    portfolioConfig: XcfaConfig<*, *>,
    logger: Logger,
    uniqueLogger: Logger,
): STM {

    fun checker(config: XcfaConfig<*, *>, witness: LocationInvariants? = null) =
        runConfig(config, logger, uniqueLogger, true, witness)

    val specConfig = portfolioConfig.frontendConfig.specConfig
    val baseConfig =
        XcfaConfig(
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
                        domain = PRED_BOOL, // This is the main change for the new portfolio
                        maxEnum = 2,
                        search = ERR,
                    ),
                    refinerConfig =
                    CegarRefinerConfig(
                        refinementSolver = "Z3",
                        validateRefinementSolver = false,
                        refinement = SEQ_ITP,
                        exprSplitter = WHOLE, // Base strategy is WHOLE
                        pruneStrategy = LAZY,
                    ),
                ),
            ),
            outputConfig =
            OutputConfig(
                versionInfo = false,
                resultFolder = Paths.get("./").toFile(),
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

    val startNodeConfig = baseConfig.copy(
        backendConfig = baseConfig.backendConfig.copy(
            inProcess = true,
            specConfig = (baseConfig.backendConfig.specConfig as CegarConfig).copy(
                refinerConfig = (baseConfig.backendConfig.specConfig as CegarConfig).refinerConfig.copy(
                    exprSplitter = WHOLE
                )
            )
        )
    )

    val endNodeConfig = baseConfig.copy(
        backendConfig = baseConfig.backendConfig.copy(
            inProcess = true,
            specConfig = (baseConfig.backendConfig.specConfig as CegarConfig).copy(
                refinerConfig = (baseConfig.backendConfig.specConfig as CegarConfig).refinerConfig.copy(
                    exprSplitter = ExprSplitterOptions.CONJUNCTS 
                )
            )
        )
    )

    val startNode = ConfigNode(
        "StartNode(PRED_BOOL,WHOLE)",
        startNodeConfig,
        ::checker
    )

    val endNode = ConfigNode(
        "EndNode(PRED_BOOL,CONJUNCTS)",
        endNodeConfig,
        ::checker
    )

    val timeoutOrNotSolvableError =
        ExceptionTrigger(
            fallthroughExceptions =
            setOf(ErrorCodeException(SERVER_ERROR.code), ErrorCodeException(SOLVER_ERROR.code)),
            label = "TimeoutOrNotSolvableError",
        )

    val edge = Edge(startNode, endNode, timeoutOrNotSolvableError)
    val actualStartNode = if ((portfolioConfig.backendConfig.specConfig as PortfolioConfig).partialResultTestOnlyEndNode) {
        endNode
    } else {
        startNode
    }

    val stm = STM(actualStartNode, setOf(edge))

    println(stm.visualize())
    return stm
}
