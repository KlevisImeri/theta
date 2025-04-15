// package hu.bme.mit.theta.xcfa.validation
//
// import hu.bme.mit.theta.analysis.InitFunc
// import hu.bme.mit.theta.analysis.LTS
// import hu.bme.mit.theta.analysis.multi.MultiAnalysisSide
// import hu.bme.mit.theta.analysis.multi.MultiPrec
// import hu.bme.mit.theta.analysis.multi.MultiSide
// import hu.bme.mit.theta.analysis.multi.builder.stmt.StmtMultiBuilder
// import hu.bme.mit.theta.analysis.multi.stmt.ExprMultiState
// import hu.bme.mit.theta.analysis.unit.UnitPrec
// import hu.bme.mit.theta.analysis.unit.UnitState
// import hu.bme.mit.theta.core.stmt.Stmt
// import hu.bme.mit.theta.xcfa.model.XCFA
//
//
// // TODO: For the moment this is only for reach error
// // THe Idea of the implmentatoin si that you have the XCFAControl 
// // which has The current location or the current edge (i dont know how to do this) of the Xcfa of program
//
// class XcfaYamlWitnessValidator(program: XCFA, witness: XCFA, solver: Solver) {
//
//     private data class XcfaControl(val location: XcfaLocation) : State {
//         override fun isBottom() = false
//     }
//
//     private data class SharedVarsState(val vars: Map<String, Any>) : State {
//         override fun isBottom() = vars.isEmpty()
//     }
//
//
//     class ValidationException(message: String) : Exception(message)
//
//
//     companion object {
//       private val multiUnitPrec = MultiPrec(
//         UnitPrec.getInstance(),
//         UnitPrec.getInstance(),
//         UnitPrec.getInstance()
//       )
//     }
//
//     private val programSide: MultiAnalysisSide<XcfaState, UnitState, XcfaControl, Stmt, UnitPrec, UnitPrec>
//     private val witnessSide: MultiAnalysisSide<XcfaState, UnitState, XcfaControl, Stmt, UnitPrec, UnitPrec>
//     private val productAnalysis: ProductAnalysis
//
//
//     init {
//         programSide = createXcfaAnalysisSide(program)
//         witnessSide = createXcfaAnalysisSide(witness)
//         productAnalysis = createProductAnalysis()
//     }
//
//     private fun createPredXcfaAnalysisSide(
//         xcfa: XCFA,
//     ): MultiAnalysisSide<
//         XcfaState<PtrState<PredState>>,
//         UnitState,
//         XcfaControl,
//         XcfaAction,
//         XcfaPrec<PtrPrec<PredPrec>>, 
//         UnitPrec
//     > {
//
//         val analysis = PredXcfaAnalysis(
//             xcfa = xcfa,
//             solver = solver,
//             predAbstractor = PredAbstractors.createBooleanAbstractor(solver),
//             partialOrd = getPartialOrder(PredPartialOrd),
//             isHavoc = false
//         )
//
//         return MultiAnalysisSide(
//             analysis = analysis,
//             controlInitFunc = InitFunc { prec ->
//                 xcfa.initProcedures.map { proc ->
//                     XcfaControl(proc.first.initLoc)
//                 }
//             },
//             combineStates = { control, data ->
//                 XcfaState(
//                     xcfa = xcfa,
//                     processes = control.locations.associateWith { loc ->
//                         XcfaProcessState(
//                             locStack = LinkedList(listOf(loc)),
//                             varLookup = LinkedList(listOf(createLookup(...))
//                         )
//                     },
//                     sGlobal = data
//                 )
//             },
//             extractControlState = { state -> 
//                 XcfaControl(state.processes.values.first().locs.peek())
//             },
//             extractDataState = { state -> state.sGlobal },
//             extractControlPrec = { prec -> UnitPrec.getInstance() }
//         )
//     }
//
//
//         val builder = StmtMultiBuilder(programSide, XcfaLts(program))
//             .addRightSide(witnessSide, XcfaLts(witness))
//
//         return builder.build(
//             nextSideFunction = { state: ExprMultiState<XcfaControl, XcfaControl, DataState> ->
//                 // MultiState getLeftSate() getRightState()...
//                 // val programLoc = state.getLeftSate().location.metadata.getCMetaData();
//                 // val witnessLoc = state.getRightSate().location.metadata.getCMetaData();
//                 // if(programLoc.contains(witnessLoc) && ...) return MultiState.Left;
//                 // else MultiState.BOTH;
//             },
//             dataInitFunc = InitFunc { prec ->
//                 // Initialize shared variables from XCFA initial state
//                 SharedVarsState(program.initialVars.associate { it.name to it.initValue })
//             }
//         )
//     }
//
//     public fun validate() {
//         val initialStates = productAnalysis.analysis.initFunc.getInitStates(multiUnitPrec)
//         val reached = mutableSetOf<ExprMultiState<*, *, *>>()
//         val queue = ArrayDeque(initialStates)
//
//         while (queue.isNotEmpty()) {
//             val state = queue.removeFirst()
//             if (!reached.add(state)) continue
//
//             // Check refinement relationship
//             if (!productAnalysis.analysis.partialOrd.isLeq(state.leftState, state.rightState)) {
//                 throw ValidationException("Witness does not abstract program state: $state")
//             }
//
//             productAnalysis.lts.getEnabledActionsFor(state)
//                 .flatMap { action ->
//                     productAnalysis.analysis.transFunc.getSuccStates(state, action, multiUnitPrec)
//                 }
//                 .forEach { succ ->
//                     if (!isValidTransition(state, succ)) {
//                         throw ValidationException("Invalid transition: $state -> $succ")
//                     }
//                     queue.add(succ)
//                 }
//         }
//     }
//
// }
