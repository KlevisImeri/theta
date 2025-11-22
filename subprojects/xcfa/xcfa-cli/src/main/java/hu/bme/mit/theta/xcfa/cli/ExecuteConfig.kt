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

import com.google.common.base.Stopwatch
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import hu.bme.mit.theta.analysis.Action
import hu.bme.mit.theta.analysis.EmptyCex
import hu.bme.mit.theta.analysis.State
import hu.bme.mit.theta.analysis.Trace
import hu.bme.mit.theta.analysis.algorithm.EmptyProof
import hu.bme.mit.theta.analysis.algorithm.SafetyResult
import hu.bme.mit.theta.analysis.algorithm.arg.ARG
import hu.bme.mit.theta.analysis.algorithm.arg.debug.ARGWebDebugger
import hu.bme.mit.theta.analysis.expl.ExplState
import hu.bme.mit.theta.analysis.ptr.PtrState
import hu.bme.mit.theta.analysis.utils.ArgVisualizer
import hu.bme.mit.theta.analysis.utils.TraceVisualizer
import hu.bme.mit.theta.c2xcfa.CMetaData
import hu.bme.mit.theta.cat.dsl.CatDslManager
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.common.logging.Logger.Level.*
import hu.bme.mit.theta.common.visualization.Graph
import hu.bme.mit.theta.common.visualization.writer.GraphvizWriter
import hu.bme.mit.theta.common.visualization.writer.WebDebuggerLogger
import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.graphsolver.patterns.constraints.MCM
import hu.bme.mit.theta.xcfa.analysis.ErrorDetection
import hu.bme.mit.theta.xcfa.analysis.XcfaAction
import hu.bme.mit.theta.xcfa.analysis.XcfaState
import hu.bme.mit.theta.xcfa.analysis.coi.ConeOfInfluence
import hu.bme.mit.theta.xcfa.analysis.coi.XcfaCoiMultiThread
import hu.bme.mit.theta.xcfa.analysis.coi.XcfaCoiSingleThread
import hu.bme.mit.theta.xcfa.analysis.oc.OcDecisionProcedureType
import hu.bme.mit.theta.xcfa.analysis.por.XcfaDporLts
import hu.bme.mit.theta.xcfa.analysis.por.XcfaSporLts
import hu.bme.mit.theta.xcfa.cli.checkers.getChecker
import hu.bme.mit.theta.xcfa.cli.params.*
import hu.bme.mit.theta.xcfa.cli.utils.*
import hu.bme.mit.theta.xcfa.cli.utils.LocationInvariants
import hu.bme.mit.theta.xcfa.cli.witnesstransformation.ApplyLocationInvariantsPassManager
import hu.bme.mit.theta.xcfa.cli.witnesstransformation.XcfaTraceConcretizer
import hu.bme.mit.theta.xcfa.getFlatLabels
import hu.bme.mit.theta.xcfa.model.MetadataLabelCustomizer
import hu.bme.mit.theta.xcfa.model.XCFA
import hu.bme.mit.theta.xcfa.model.XcfaLabel
import hu.bme.mit.theta.xcfa.model.optimizeFurther
import hu.bme.mit.theta.xcfa.model.toDot
import hu.bme.mit.theta.xcfa.passes.*
import hu.bme.mit.theta.xcfa.toC
import hu.bme.mit.theta.xcfa2chc.RankingFunction
import hu.bme.mit.theta.xcfa2chc.toSMT2CHC
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import hu.bme.mit.theta.xcfa.cli.checkers.InProcessChecker
import hu.bme.mit.theta.xcfa.cli.utils.CachingFileSerializer
import hu.bme.mit.theta.ui.TUI.yellow
import hu.bme.mit.theta.ui.GUI.rlj
import hu.bme.mit.theta.ui.GUI
import com.raylib.java.core.Color

// fun runConfig(
//   config: XcfaConfig<*, *>,
//   logger: Logger,
//   uniqueLogger: Logger,
//   throwDontExit: Boolean,
//   partialResult: LocationInvariants? = null,
// ): SafetyResult<*, *> {
//   if (GUI.enabled && config.backendConfig.inProcess) {
//     GUI.start();
//     GUI.draw {
//       rlj.text.DrawText("Starting the Execute Config!", 190, 200, 20, Color.LIGHTGRAY);
//     }
//     val res = internalRunConfig(config, logger, uniqueLogger, throwDontExit, partialResult)
//     return res;
//   } else {
//     return internalRunConfig(config, logger, uniqueLogger, throwDontExit, partialResult)
//   }
// }

// FIX: Xcfa shoudl be prited even if an erro is through 
// postverficaion shoudl be caalled even if an error is throuwn

fun runConfig(
  config: XcfaConfig<*, *>,
  logger: Logger,
  uniqueLogger: Logger,
  throwDontExit: Boolean,
  partialResult: LocationInvariants? = null,
): SafetyResult<*, *>  {
  // println("RunConfig");
  // println(config)
  // println("disablePartialResult: ${config.backendConfig.disablePartialResult}")
  if(config.backendConfig.softTimeoutMs == 0L) config.backendConfig.softTimeoutMs = config.backendConfig.timeoutMs;

  propagateInputOptions(config, logger, uniqueLogger)
  registerAllSolverManagers(config.backendConfig.solverHome, logger)
  validateInputOptions(config, logger, uniqueLogger)

  val (xcfa, mcm, parseContext) =
    if (config.backendConfig.inProcess && config.backendConfig.parseInProcess) {
      logger.writeln(INFO, "Not parsing input because a worker process will handle it later.")
      Triple(null, null, null)
    } else {
      val (xcfa, mcm, parseContext) = frontend(config, logger, uniqueLogger, partialResult)
      preVerificationLogging(xcfa, mcm, parseContext, config, logger, uniqueLogger)
      Triple(xcfa, mcm, parseContext)
    }

  val result = backend(xcfa, mcm, parseContext, config, logger, uniqueLogger, throwDontExit)
  postVerificationLogging(xcfa, result, mcm, parseContext, config, logger, uniqueLogger)
  cleanUp(config, logger)

  return result
}

private fun propagateInputOptions(config: XcfaConfig<*, *>, logger: Logger, uniqueLogger: Logger) {
  config.inputConfig.property = determineProperty(config, logger)
  LbePass.level = config.frontendConfig.lbeLevel
  StaticCoiPass.enabled = config.frontendConfig.staticCoi
  if (config.backendConfig.backend == Backend.CEGAR) {
    val cegarConfig = config.backendConfig.specConfig
    cegarConfig as CegarConfig
    val random = Random(cegarConfig.porRandomSeed)
    XcfaSporLts.random = random
    XcfaDporLts.random = random
  }
  if (
    config.inputConfig.property == ErrorDetection.MEMSAFETY ||
      config.inputConfig.property == ErrorDetection.MEMCLEANUP
  ) {
    MemsafetyPass.NEED_CHECK = true
  }
  if (config.debugConfig.argToFile) {
    WebDebuggerLogger.enableWebDebuggerLogger()
    WebDebuggerLogger.getInstance().setTitle(config.inputConfig.input?.name)
  }

  LoopUnrollPass.UNROLL_LIMIT = config.frontendConfig.loopUnroll
  LoopUnrollPass.FORCE_UNROLL_LIMIT = config.frontendConfig.forceUnroll
  FetchExecuteWriteback.enabled = config.frontendConfig.enableFew
  ARGWebDebugger.on = config.debugConfig.argdebug
}

private fun validateInputOptions(config: XcfaConfig<*, *>, logger: Logger, uniqueLogger: Logger) {
  rule("NoCoiWhenDataRace") {
    config.backendConfig.backend == Backend.CEGAR &&
      (config.backendConfig.specConfig as? CegarConfig)?.coi != ConeOfInfluenceMode.NO_COI &&
      config.inputConfig.property == ErrorDetection.DATA_RACE
  }
  rule("NoAaporWhenDataRace") {
    (config.backendConfig.specConfig as? CegarConfig)?.porLevel?.isAbstractionAware == true &&
      config.inputConfig.property == ErrorDetection.DATA_RACE
  }
  rule("DPORWithoutDFS") {
    (config.backendConfig.specConfig as? CegarConfig)?.porLevel?.isDynamic == true &&
      (config.backendConfig.specConfig as? CegarConfig)?.abstractorConfig?.search != Search.DFS
  }
  rule("SensibleLoopUnrollLimits") {
    config.frontendConfig.loopUnroll != -1 &&
      config.frontendConfig.loopUnroll < config.frontendConfig.forceUnroll
  }
  rule("NoPredSplitUntilFixed(https://github.com/ftsrg/theta/issues/267)") {
    (config.backendConfig.specConfig as? CegarConfig)?.abstractorConfig?.domain == Domain.PRED_SPLIT
  }
  rule("OcPropagatorWithoutZ3") {
    config.backendConfig.backend == Backend.OC &&
      (config.backendConfig.specConfig as? OcConfig)?.decisionProcedure ==
        OcDecisionProcedureType.PROPAGATOR &&
      (config.backendConfig.specConfig as? OcConfig)?.smtSolver != "Z3:4.13"
  }
}

fun frontend(
  config: XcfaConfig<*, *>,
  logger: Logger,
  uniqueLogger: Logger,
  partialResult: LocationInvariants? = null,
): Triple<XCFA, MCM, ParseContext> {
  val addPartialResultToXcfa =
    { xcfa: XCFA, parseContext: ParseContext, partialResult: LocationInvariants? ->
      // println("--------------XCFA------------------")
      // println(xcfa.toDot(MetadataLabelCustomizer).toString().replace("main::", ""))

      if (partialResult == null || partialResult.getPartitions().isEmpty()) {
        xcfa
      } else {
        // println("\n--------------PartialResult------------------")
        // println(partialResult)
        val updatedXcfa =
          xcfa.optimizeFurther(ApplyLocationInvariantsPassManager(parseContext, partialResult))
        // println("\n--------------XCFA+PartialResult------------------")
        // println(updatedXcfa.toDot(MetadataLabelCustomizer).toString().replace("main::", ""))
        updatedXcfa
      }
    }

  var currentPartialResult = partialResult
  val loadPartialResultFromFileIfItExits = { xcfa: XCFA -> 
    config.inputConfig.partialResult?.let { partialResultFile ->
      LocationInvariants.fromFile(partialResultFile, getGson(xcfa), logger)?.let { loadedInvariants ->
        currentPartialResult = loadedInvariants
      }
    } 
  }

  if (config.inputConfig.xcfaWCtx != null) {
    var (xcfa, mcm, parseContext) = config.inputConfig.xcfaWCtx!!
    loadPartialResultFromFileIfItExits(xcfa);
    xcfa = addPartialResultToXcfa(xcfa, parseContext, currentPartialResult)
    ConeOfInfluence =
      if (config.inputConfig.xcfaWCtx!!.third.multiThreading) {
        XcfaCoiMultiThread(xcfa)
      } else {
        XcfaCoiSingleThread(xcfa)
      }
    return Triple(xcfa, mcm, parseContext)
  }

  val stopwatch = Stopwatch.createStarted()

  val input = config.inputConfig.input!!
  logger.write(
    Logger.Level.INFO,
    "Parsing the input $input as ${config.frontendConfig.inputType}\n",
  )

  val parseContext = ParseContext()

  if (config.frontendConfig.inputType == InputType.C) {
    val cConfig = config.frontendConfig.specConfig
    cConfig as CFrontendConfig
    parseContext.arithmetic = cConfig.arithmetic
    parseContext.architecture = cConfig.architecture
  }

  var xcfa = getXcfa(config, parseContext, logger, uniqueLogger)
  loadPartialResultFromFileIfItExits(xcfa);
  xcfa = addPartialResultToXcfa(xcfa, parseContext, currentPartialResult)

  val mcm =
    if (config.inputConfig.catFile != null) {
      CatDslManager.createMCM(config.inputConfig.catFile!!)
    } else {
      emptySet()
    }

  ConeOfInfluence =
    if (parseContext.multiThreading) XcfaCoiMultiThread(xcfa) else XcfaCoiSingleThread(xcfa)

  if (
    parseContext.multiThreading &&
      (config.backendConfig.specConfig as? CegarConfig)?.let {
        it.abstractorConfig.search == Search.ERR
      } == true
  ) {
    val cConfig = config.backendConfig.specConfig as CegarConfig
    cConfig.abstractorConfig.search = Search.DFS
    uniqueLogger.write(INFO, "Multithreaded program found, using DFS instead of ERR.")
  }

  logger.write(
    Logger.Level.INFO,
    "Frontend finished: ${xcfa.name}  (in ${
        stopwatch.elapsed(TimeUnit.MILLISECONDS)
    } ms)\n",
  )

  logger.write(RESULT, "ParsingResult Success\n")
  logger.write(
    RESULT,
    "Alias graph size: ${xcfa.pointsToGraph.size} -> ${xcfa.pointsToGraph.values.map { it.size }.toList()}\n",
  )

  return Triple(xcfa, mcm, parseContext)
}

private fun backend(
  xcfa: XCFA?,
  mcm: MCM?,
  parseContext: ParseContext?,
  config: XcfaConfig<*, *>,
  logger: Logger,
  uniqueLogger: Logger,
  throwDontExit: Boolean,
  partialResult: LocationInvariants? = null,
): SafetyResult<*, *> =
  if (config.backendConfig.backend == Backend.NONE) {
    SafetyResult.unknown<EmptyProof, EmptyCex>()
  } else {
    if (
      xcfa?.procedures?.all {
        it.errorLoc.isEmpty && config.inputConfig.property == ErrorDetection.ERROR_LOCATION
      } ?: false
    ) {
      val result = SafetyResult.safe<EmptyProof, EmptyCex>(EmptyProof.getInstance())
      logger.write(Logger.Level.RESULT, "Input is trivially safe\n")

      logger.write(RESULT, result.toString() + "\n")
      result
    } else {
      val stopwatch = Stopwatch.createStarted()

      // logger.write(
      //   Logger.Level.INFO,
      //   "Starting verification of ${if (xcfa?.name == "") "UnnamedXcfa" else (xcfa?.name ?: "DeferredXcfa")} using ${config.backendConfig.backend}\n${config}\n",
      // )
      
      fun addBackendTimeToStatisticsAndLog() {
        val backendAlgoTimeMs =stopwatch.elapsed(TimeUnit.MILLISECONDS)
        logger.write(
          RESULT,
          "Backend finished (in ${backendAlgoTimeMs} ms)\n",
        )
        val key = if (config.backendConfig.inPortfolio) {
          "inPortfolio"
        }  else { "Total" }
        XcfaBackendStatistics.addTime(key, backendAlgoTimeMs);
      }
      val checker = getChecker(xcfa, mcm, config, parseContext, logger, uniqueLogger)
      var result = exitOnError(config.debugConfig.stacktrace, config.debugConfig.debug || throwDontExit) {
              try{
                  checker.check()
              } catch(e: Throwable) {
                addBackendTimeToStatisticsAndLog()
                throw e
              }
          }
          .let ResultMapper@{ result ->
            when {
              result.isSafe && xcfa?.unsafeUnrollUsed ?: false -> {
                // cannot report safe if force unroll was used
                logger.write(RESULT, "Incomplete loop unroll used: safe result is unreliable.\n")
                if (config.outputConfig.acceptUnreliableSafe)
                  result // for comparison with BMC tools
                else SafetyResult.unknown<EmptyProof, EmptyCex>()
              }

              result.isUnsafe -> {
                // need to determine what kind
                val property =
                  when (config.inputConfig.property) {
                    ErrorDetection.MEMSAFETY,
                    ErrorDetection.MEMCLEANUP -> {
                      val trace = result.asUnsafe().cex as? Trace<XcfaState<*>, XcfaAction>
                      trace
                        ?.states
                        ?.asReversed()
                        ?.firstOrNull {
                          it.processes.values.any { it.locs.any { it.name.contains("__THETA_") } }
                        }
                        ?.processes
                        ?.values
                        ?.firstOrNull { it.locs.any { it.name.contains("__THETA_") } }
                        ?.locs
                        ?.firstOrNull { it.name.contains("__THETA_") }
                        ?.name
                        ?.let {
                          when (it) {
                            "__THETA_bad_free" -> "valid-free"
                            "__THETA_bad_deref" -> "valid-deref"
                            "__THETA_lost" ->
                              if (config.inputConfig.property == ErrorDetection.MEMCLEANUP)
                                "valid-memcleanup"
                              else
                                "valid-memtrack"
                                  .also { // this is not an exact check.
                                    return@ResultMapper SafetyResult.unknown<EmptyProof, EmptyCex>()
                                  }
                            else ->
                              throw RuntimeException(
                                "Something went wrong; could not determine subproperty! Named location: $it"
                              )
                          }
                        }
                    }
                    ErrorDetection.DATA_RACE -> "no-data-race"
                    ErrorDetection.ERROR_LOCATION -> "unreach-call"
                    ErrorDetection.OVERFLOW -> "no-overflow"
                    ErrorDetection.NO_ERROR -> null
                    ErrorDetection.TERMINATION -> "termination"
                  }
                property?.also { logger.write(RESULT, "(Property %s)\n", it) }
                result
              }

              else -> {
                result
              }
            }
          }

      addBackendTimeToStatisticsAndLog()

      val outResult = if(result.stats.isEmpty) {
        result.withStats(XcfaBackendStatistics())
      } else {
        val stats = result.stats.get()
        if (stats is XcfaBackendStatistics && !config.backendConfig.inPortfolio) {
          val backendStats = stats as XcfaBackendStatistics
          result.withStats(XcfaBackendStatistics()) // Update the statistics
        } else {
          result
        }
      }

      logger.write(RESULT, outResult.toString() + "\n")

      outResult
    }
  }

private fun preVerificationLogging(
  xcfa: XCFA,
  mcm: MCM,
  parseContext: ParseContext,
  config: XcfaConfig<*, *>,
  logger: Logger,
  uniqueLogger: Logger,
) {
  if (config.outputConfig.enableOutput) {
    try {
      val resultFolder = config.outputConfig.resultFolder
      // println("resultFolder:${resultFolder}")
      resultFolder.mkdirs()

      // logger.write(
      //   Logger.Level.INFO,
      //   "Writing pre-verification artifacts to directory ${resultFolder.absolutePath} with config ${config.outputConfig}\n",
      // )

      if (!config.outputConfig.chcOutputConfig.disable) {
        xcfa.procedures.forEach {
          try {
            val chcFile = File(resultFolder, "xcfa-${it.name}.smt2")
            chcFile.writeText(
              it.toSMT2CHC(
                config.inputConfig.property == ErrorDetection.TERMINATION,
                (config.backendConfig.specConfig as? HornConfig)?.rankingFuncConstr
                  ?: RankingFunction.ADD,
              )
            )
          } catch (e: Exception) {
            logger.write(INFO, "Could not write CHC file: " + e.stackTraceToString())
          }
        }
      }

      if (!config.outputConfig.xcfaOutputConfig.disable) {
        val xcfaDotFile = File(resultFolder, "xcfa.dot")
        xcfaDotFile.writeText(xcfa.toDot())

        val xcfaJsonFile = File(resultFolder, "xcfa.json")
        val uglyJson = getGson(xcfa).toJson(xcfa)
        val create = GsonBuilder().setPrettyPrinting().create()
        xcfaJsonFile.writeText(create.toJson(JsonParser.parseString(uglyJson)))
      }

      if (!config.outputConfig.cOutputConfig.disable) {
        try {
          val xcfaCFile = File(resultFolder, "xcfa.c")
          xcfaCFile.writeText(
            xcfa.toC(
              parseContext,
              config.outputConfig.cOutputConfig.useArr,
              config.outputConfig.cOutputConfig.useExArr,
              config.outputConfig.cOutputConfig.useRange,
            )
          )
        } catch (e: Throwable) {
          logger.write(Logger.Level.VERBOSE, "Could not emit C file\n")
        }
      }
    } catch (e: Throwable) {
      logger.write(Logger.Level.INFO, "Could not output files: ${e.stackTraceToString()}\n")
    }
  }
}

private fun postVerificationLogging(
  xcfa: XCFA?,
  safetyResult: SafetyResult<*, *>,
  mcm: MCM?,
  parseContext: ParseContext?,
  config: XcfaConfig<*, *>,
  logger: Logger,
  uniqueLogger: Logger,
) {
  if (
    config.outputConfig.enableOutput && 
    safetyResult.isPartial && 
    !config.backendConfig.inProcess && 
    config.backendConfig.inPortfolio
   ) {
    val locInvNew = safetyResult.asPartial().proof
    if (locInvNew !is LocationInvariants) {
      logger.write(
        Logger.Level.INFO,
        yellow("[WARN] For the moment thetas XCFA subproject can only process LocationInvariants as partial results between analyses!\n"),
      )
    } else if(config.backendConfig.disablePartialResult &&  config.outputConfig.partialResultOutputConfig.enable) {
        logger.write(Logger.Level.INFO, yellow(
          "[WARN] You have [--disable-partial-results] but you have enabled [--enable-epartial-result-to-file]. " +
          "There is no partial result to put into a file!\n"
        ));
    } else {
        val partialResultOutputConfig = config.outputConfig.partialResultOutputConfig
        if (partialResultOutputConfig.tempFileLocation == null) {
          if (config.inputConfig.partialResult == null) {
            error(
              "Both config.inputConfig.partialResult and config.outputConfig.partialResultOutputConfig.tempFileLocation are null! " +
              "You have partial results on but there is no input or output file path for them.\n"
                )
          }
          logger.write(Logger.Level.INFO, yellow(
            "[WARN] Partial results are on but no tempFileLocation for the partialResult is specified! " +
            "Using config.inputConfig.partialResult as output temporary file for partialResult.\n"
          ));
          partialResultOutputConfig.tempFileLocation = config.inputConfig.partialResult
        }
        xcfa!!
        val partialResultTempFile = partialResultOutputConfig.tempFileLocation!!
        val gson = getGson(xcfa)

        val locInvOld =
        config.inputConfig.partialResult?.let { inputFile ->
          LocationInvariants.fromFile(inputFile, gson, logger)
        }

        val finalInvariants =
        if (locInvOld != null) {
          // logger.write(Logger.Level.INFO, "Merging new partial results with existing ones.\n")
          // locInvNew.merge(locInvOld)
          locInvNew  // WARN:  Eventually we have to merg
        } else {
          locInvNew
        }
        // println(finalInvariants)
        println(finalInvariants.toString().lineSequence().take(40).joinToString("\n"))

        finalInvariants.toJsonFile(partialResultTempFile, gson, logger)
        
        if(partialResultOutputConfig.enable) {
            val resultFolder  = config.outputConfig.resultFolder
            partialResultTempFile.copyTo(resultFolder.resolve("PartialResult.json"), overwrite = true)
            resultFolder.resolve("PartialResult.txt").writeText(finalInvariants.toString())
        }
      }
    }

  if (
    config.frontendConfig.inputType == InputType.CHC &&
      xcfa != null &&
      (config.frontendConfig.specConfig as CHCFrontendConfig).model
  ) {
    val resultFolder = config.outputConfig.resultFolder
    resultFolder.mkdirs()
    val chcAnswer = writeModel(xcfa, safetyResult)
    val chcAnswerFile = File(resultFolder, "chc-answer.smt2")
    if (chcAnswerFile.exists()) {
      logger.writeln(
        INFO,
        "CHC answer/model already written to file $chcAnswerFile, not overwriting",
      )
    } else {
      chcAnswerFile.writeText(chcAnswer)
      logger.writeln(INFO, "CHC answer/model written to file $chcAnswerFile")
    }
  }
  if (config.outputConfig.enableOutput && mcm != null && parseContext != null) {
    try {
      // we only want to log the files if the current configuration is not --in-process or portfolio
      if (config.backendConfig.inProcess || config.backendConfig.backend == Backend.PORTFOLIO) {
        return
      }

      val resultFolder = config.outputConfig.resultFolder
      resultFolder.mkdirs()

      logger.write(
        Logger.Level.INFO,
        "Writing post-verification artifacts to directory ${resultFolder.absolutePath}%n",
      )

      // TODO: eliminate the need for the instanceof check
      if (
        !config.outputConfig.argConfig.disable && safetyResult.proof is ARG<out State, out Action>
      ) {
        val argFile = File(resultFolder, "arg-${safetyResult.isSafe}.dot")
        val g: Graph =
          ArgVisualizer.getDefault().visualize(safetyResult.proof as ARG<out State, out Action>)
        argFile.writeText(GraphvizWriter.getInstance().writeString(g))
      }

      if (!config.outputConfig.witnessConfig.disable) {
        if (
          safetyResult.isUnsafe &&
            safetyResult.asUnsafe().cex != null &&
            !config.outputConfig.witnessConfig.svcomp
        ) {
          val concrTrace: Trace<XcfaState<ExplState>, XcfaAction> =
            XcfaTraceConcretizer.concretize(
              safetyResult.asUnsafe().cex as Trace<XcfaState<PtrState<*>>, XcfaAction>,
              getSolver(
                config.outputConfig.witnessConfig.concretizerSolver,
                config.outputConfig.witnessConfig.validateConcretizerSolver,
              ),
              parseContext,
            )

          val traceFile = File(resultFolder, "trace.dot")
          val traceG: Graph = TraceVisualizer.getDefault().visualize(concrTrace)
          traceFile.writeText(GraphvizWriter.getInstance().writeString(traceG))

          val sequenceFile = File(resultFolder, "trace.plantuml")
          writeSequenceTrace(
            sequenceFile,
            safetyResult.asUnsafe().cex as Trace<XcfaState<ExplState>, XcfaAction>,
          ) { (_, act) ->
            act.label.getFlatLabels().map(XcfaLabel::toString)
          }

          val optSequenceFile = File(resultFolder, "trace-optimized.plantuml")
          writeSequenceTrace(optSequenceFile, concrTrace) { (_, act) ->
            act.label.getFlatLabels().map(XcfaLabel::toString)
          }

          val cSequenceFile = File(resultFolder, "trace-c.plantuml")
          writeSequenceTrace(cSequenceFile, concrTrace) { (state, act) ->
            val proc = state.processes[act.pid]
            val loc = proc?.locs?.peek()
            (loc?.metadata as? CMetaData)?.sourceText?.split("\n") ?: listOf("<unknown>")
          }
        }
        val witnessFile = File(resultFolder, "witness.graphml")
        GraphmlWitnessWriter()
          .writeWitness(
            safetyResult,
            config.outputConfig.witnessConfig.inputFileForWitness ?: config.inputConfig.input!!,
            getSolver(
              config.outputConfig.witnessConfig.concretizerSolver,
              config.outputConfig.witnessConfig.validateConcretizerSolver,
            ),
            parseContext,
            witnessFile,
            config.inputConfig.property,
          )
        val yamlWitnessFile = File(resultFolder, "witness.yml")
        YamlWitnessWriter()
          .writeWitness(
            safetyResult,
            config.outputConfig.witnessConfig.inputFileForWitness ?: config.inputConfig.input!!,
            config.inputConfig.property,
            (config.frontendConfig.specConfig as? CFrontendConfig)?.architecture,
            getSolver(
              config.outputConfig.witnessConfig.concretizerSolver,
              config.outputConfig.witnessConfig.validateConcretizerSolver,
            ),
            parseContext,
            yamlWitnessFile,
          )
      }
    } catch (e: Throwable) {
      logger.write(Logger.Level.INFO, "Could not output files: ${e.stackTraceToString()}\n")
    }
  }
}


private fun writeSequenceTrace(
  sequenceFile: File,
  trace: Trace<XcfaState<ExplState>, XcfaAction>,
  printer: (Pair<XcfaState<ExplState>, XcfaAction>) -> List<String>,
) {
  sequenceFile.writeText("@startuml\n")
  var maxWidth = 0
  trace.actions.forEachIndexed { i, it ->
    val stateBefore = trace.states[i]
    sequenceFile.appendText("hnote over ${it.pid}\n")
    val labelStrings = printer(Pair(stateBefore, it))
    if (maxWidth < (labelStrings.maxOfOrNull { it.length } ?: 0)) {
      maxWidth = labelStrings.maxOfOrNull { it.length } ?: 0
    }
    sequenceFile.appendText("${labelStrings.joinToString("\n")}\n")
    sequenceFile.appendText("endhnote\n")
  }
  trace.actions
    .map { it.pid }
    .distinct()
    .reduce { acc, current ->
      sequenceFile.appendText("$acc --> $current: \"${" ".repeat(maxWidth)}\"\n")
      current
    }
  sequenceFile.appendText("@enduml\n")
}

private fun cleanUp(config: XcfaConfig<*, *>, logger: Logger) {
  val resultFolder = config.outputConfig.resultFolder;
  if (resultFolder.exists() && resultFolder.isDirectory && resultFolder.listFiles().isNullOrEmpty()) {
    logger.write(Logger.Level.VERBOSE,  yellow(
      "[WARN] The output result folder was empty so it was deleted. "+
      "The folder creation and deletion is taking compute time for no reason. \n"
    ))
    resultFolder.delete()
  }

  if(!config.backendConfig.inProcess && !config.backendConfig.inPortfolio){ 
    // NOTE: if you run several runConfigs you have to clear up 
    XcfaBackendStatistics.clear();
    CachingFileSerializer.clear();
  }
}

