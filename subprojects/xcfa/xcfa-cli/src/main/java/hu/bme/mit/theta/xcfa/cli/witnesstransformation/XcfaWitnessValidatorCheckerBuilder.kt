package hu.bme.mit.theta.xcfa.cli.witnesstransformation

import hu.bme.mit.theta.analysis.InitFunc
import hu.bme.mit.theta.analysis.LTS
import hu.bme.mit.theta.analysis.multi.MultiAnalysisSide
import hu.bme.mit.theta.analysis.multi.MultiPrec
import hu.bme.mit.theta.analysis.multi.MultiSide
import hu.bme.mit.theta.analysis.multi.builder.stmt.StmtMultiBuilder
import hu.bme.mit.theta.analysis.multi.stmt.ExprMultiState
import hu.bme.mit.theta.analysis.unit.UnitPrec
import hu.bme.mit.theta.analysis.unit.UnitState
import hu.bme.mit.theta.core.stmt.Stmt
import hu.bme.mit.theta.xcfa.model.XCFA
import hu.bme.mit.theta.analysis.multi.NextSideFunctions.Alternating
import hu.bme.mit.theta.solver.SolverFactory;
import hu.bme.mit.theta.solver.z3.Z3SolverFactory
import hu.bme.mit.theta.common.logging.Logger;
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker;
import hu.bme.mit.theta.analysis.algorithm.arg.ARG
import hu.bme.mit.theta.xcfa.analysis.XcfaState
import hu.bme.mit.theta.xcfa.analysis.XcfaAction
import hu.bme.mit.theta.xcfa.analysis.XcfaPrec
import hu.bme.mit.theta.analysis.ptr.PtrState
import hu.bme.mit.theta.analysis.ptr.PtrPrec
import hu.bme.mit.theta.analysis.pred.PredState
import hu.bme.mit.theta.analysis.pred.PredPrec
import hu.bme.mit.theta.analysis.expr.ExprAction
import hu.bme.mit.theta.analysis.expr.ExprState
import hu.bme.mit.theta.analysis.expl.ExplPrec
import hu.bme.mit.theta.analysis.expl.ExplState
import hu.bme.mit.theta.analysis.Trace;
import hu.bme.mit.theta.analysis.multi.stmt.StmtMultiAction;
import hu.bme.mit.theta.analysis.multi.config.MultiConfig
import hu.bme.mit.theta.xcfa.analysis.getXcfaErrorPredicate
import hu.bme.mit.theta.xcfa.analysis.ErrorDetection
import java.util.function.Predicate;
import hu.bme.mit.theta.analysis.multi.config.StmtMultiConfigBuilder
import hu.bme.mit.theta.analysis.expl.ItpRefToExplPrec
import hu.bme.mit.theta.core.type.booltype.BoolExprs.True
import hu.bme.mit.theta.core.type.booltype.BoolExprs.False
import hu.bme.mit.theta.core.decl.VarDecl
import hu.bme.mit.theta.analysis.expr.refinement.PruneStrategy;
import hu.bme.mit.theta.xcfa.analysis.getXcfaLts
import hu.bme.mit.theta.analysis.multi.NextSideFunctions
import hu.bme.mit.theta.xcfa.analysis.ExplXcfaAnalysis
import hu.bme.mit.theta.xcfa.analysis.PredXcfaAnalysis
import hu.bme.mit.theta.xcfa.analysis.getExplXcfaInitFunc
import hu.bme.mit.theta.xcfa.analysis.getPredXcfaInitFunc
import hu.bme.mit.theta.xcfa.analysis.getPartialOrder
import hu.bme.mit.theta.analysis.expl.ExplOrd
import hu.bme.mit.theta.analysis.pred.PredAbstractors;
import hu.bme.mit.theta.analysis.pred.PredOrd;
import hu.bme.mit.theta.analysis.ptr.getPtrPartialOrd
import hu.bme.mit.theta.analysis.expr.refinement.ItpRefutation
import hu.bme.mit.theta.analysis.expr.refinement.RefutationToPrec
import hu.bme.mit.theta.analysis.pred.ExprSplitters
import hu.bme.mit.theta.analysis.pred.ItpRefToPredPrec
import hu.bme.mit.theta.core.type.booltype.BoolType;
import hu.bme.mit.theta.core.stmt.AssumeStmt
import hu.bme.mit.theta.core.type.Expr
import hu.bme.mit.theta.xcfa.model.XcfaEdge
import hu.bme.mit.theta.xcfa.model.StmtLabel
// TODO: For the moment this is only for reach error

class XcfaWitnessValidatorCheckerBuilder(
  val program: XCFA,
  val witness: XCFA,
  val solverFactory: SolverFactory = Z3SolverFactory.getInstance(),
  val logger: Logger
) {

    private val multiUnitPrec = MultiPrec(
      UnitPrec.getInstance(),
      UnitPrec.getInstance(),
      UnitPrec.getInstance()
    )

    fun XCFA.allVars(): Set<VarDecl<*>> {
      val globals = globalVars.map { it.wrappedVar }
      val fromProcs = procedures.flatMap { proc ->
        // val ps = proc.params.map { it.first }
        // val ls = proc.vars
        // ps + ls
        proc.vars
      }
      // return (globals + fromProcs).toSet()
      return globals.toSet();
    }

    fun XcfaEdge.getGuard(): Expr<BoolType>? =
        (label as? StmtLabel)
          ?.stmt
          .let { if (it is AssumeStmt) it.getCond() else null }

    fun XCFA.getGuards(): Set<Expr<BoolType>> =
        procedures
          .asSequence()
          .flatMap { proc -> proc.edges.asSequence() }
          .mapNotNull { edge -> edge.getGuard() }
          .toSet()

    class ItpRefToXcfaPrec : RefutationToPrec<
        XcfaPrec<PtrPrec<ExplPrec>>,
        ItpRefutation
    > {
        private val explConverter = ItpRefToExplPrec()

        override fun toPrec(
            refutation: ItpRefutation,
            index: Int
        ): XcfaPrec<PtrPrec<ExplPrec>> {
            val explPrec: ExplPrec = explConverter.toPrec(refutation, index)
            return XcfaPrec(PtrPrec(explPrec))

        }

        override fun join(
            prec1: XcfaPrec<PtrPrec<ExplPrec>>,
            prec2: XcfaPrec<PtrPrec<ExplPrec>>
        ): XcfaPrec<PtrPrec<ExplPrec>> {
            val inner1 = prec1.p.innerPrec
            val inner2 = prec2.p.innerPrec
            val joinedExpl: ExplPrec = inner1.join(inner2)
            return XcfaPrec(PtrPrec(joinedExpl))
        }

        override fun toString(): String = "ItpRefToXcfaPrec"
    }

    class ItpRefToPredXcfaPrec : RefutationToPrec<
        XcfaPrec<PtrPrec<PredPrec>>,
        ItpRefutation
    > {
        private val predConverter = ItpRefToPredPrec(ExprSplitters.atoms());

        override fun toPrec(
            refutation: ItpRefutation,
            index: Int
        ): XcfaPrec<PtrPrec<PredPrec>> {
            return XcfaPrec(PtrPrec(predConverter.toPrec(refutation, index)));
        }

        override fun join(
            prec1: XcfaPrec<PtrPrec<PredPrec>>,
            prec2: XcfaPrec<PtrPrec<PredPrec>>
        ): XcfaPrec<PtrPrec<PredPrec>> {
            val inner1 = prec1.p.innerPrec
            val inner2 = prec2.p.innerPrec
            val joinedPred: PredPrec = inner1.join(inner2)
            return XcfaPrec(PtrPrec(joinedPred))
        }

        override fun toString(): String = "ItpRefToPredXcfaPrec"
    }

    class ItpRefToUnitPrec : RefutationToPrec<UnitPrec, ItpRefutation> {
        override fun toPrec(refutation: ItpRefutation, index: Int): UnitPrec =
            UnitPrec.getInstance()

        override fun join(prec1: UnitPrec, prec2: UnitPrec): UnitPrec =
            UnitPrec.getInstance()

        override fun toString(): String = "ItpRefToUnitPrec"
    }


    public fun buildExplMultiSafetyChecker(): 
      SafetyChecker<
        ARG<ExprMultiState<XcfaState<PtrState<ExplState>>, XcfaState<PtrState<ExplState>>, UnitState>,
            StmtMultiAction<XcfaAction, XcfaAction>>,
        Trace<ExprMultiState<XcfaState<PtrState<ExplState>>, XcfaState<PtrState<ExplState>>, UnitState>,
              StmtMultiAction<XcfaAction, XcfaAction>>,
        MultiPrec<XcfaPrec<PtrPrec<ExplPrec>>, XcfaPrec<PtrPrec<ExplPrec>>, UnitPrec>
      > {
      val mc = buildExplMultiConfig()
      return SafetyChecker { _: MultiPrec<*,*,*>? ->
        mc.check()
      }
    }



    private fun buildExplMultiConfig():
      MultiConfig<
        UnitState,
        XcfaState<PtrState<ExplState>>,
        XcfaState<PtrState<ExplState>>,
        XcfaAction,
        XcfaAction,
        XcfaPrec<PtrPrec<ExplPrec>>,
        XcfaPrec<PtrPrec<ExplPrec>>,
        UnitPrec,
        ExprMultiState<XcfaState<PtrState<ExplState>>, XcfaState<PtrState<ExplState>>, UnitState>,
        StmtMultiAction<XcfaAction, XcfaAction>,
      > {
        val errorPred = getXcfaErrorPredicate(ErrorDetection.ERROR_LOCATION);
        val target = Predicate<ExprMultiState<
            XcfaState<PtrState<ExplState>>,
            XcfaState<PtrState<ExplState>>,
            UnitState
        >> { ms ->
            errorPred.test(ms.leftState) &&
            errorPred.test(ms.rightState)
        }
        return  StmtMultiConfigBuilder.ItpStmtMultiConfigBuilder(
            product = createExplProductAnalysis(),
            prop = True(),
            target = target,
            lRefToPrec = ItpRefToXcfaPrec(),
            rRefToPrec = ItpRefToXcfaPrec(),
            dRefToPrec = ItpRefToUnitPrec(),
            lInitPrec = XcfaPrec(PtrPrec(ExplPrec.of(program.allVars()))),
            rInitPrec = XcfaPrec(PtrPrec(ExplPrec.of(witness.allVars()))),
            dInitPrec = UnitPrec.getInstance(),
            solverFactory = solverFactory,
            logger = logger,
            pruneStrategy = PruneStrategy.FULL
        ).build()
    }

    private fun createExplProductAnalysis() =
      StmtMultiBuilder(createExplXcfaAnalysisSide(program), getXcfaLts())
        .addRightSide(createExplXcfaAnalysisSide(witness), getXcfaLts())
        .build(NextSideFunctions.Alternating(),
          object : InitFunc<UnitState, UnitPrec> {
            override fun getInitStates(prec: UnitPrec?) =
              listOf(UnitState.getInstance())
          }
        )

    private fun createExplXcfaAnalysisSide(xcfa: XCFA):
      MultiAnalysisSide<
        XcfaState<PtrState<ExplState>>,
        UnitState,
        XcfaState<PtrState<ExplState>>,
        XcfaAction,
        XcfaPrec<PtrPrec<ExplPrec>>,
        XcfaPrec<PtrPrec<ExplPrec>>,
    > {
        val analysis = ExplXcfaAnalysis(
          xcfa = xcfa,
          solver = solverFactory.createSolver(),
          maxEnum = 1,
          partialOrd = getPartialOrder(ExplOrd.getInstance().getPtrPartialOrd()),
          isHavoc = true
        )

        return MultiAnalysisSide(
          analysis = analysis,
          controlInitFunc = analysis.getInitFunc(),
          combineStates = { control, data -> control },
          extractControlState = { state -> state },
          extractDataState = { state -> UnitState.getInstance() },
          extractControlPrec = { prec -> prec }
        )
   }

    public fun buildPredMultiSafetyChecker(): 
      SafetyChecker<
        ARG<ExprMultiState<XcfaState<PtrState<PredState>>, XcfaState<PtrState<PredState>>, UnitState>,
            StmtMultiAction<XcfaAction, XcfaAction>>,
        Trace<ExprMultiState<XcfaState<PtrState<PredState>>, XcfaState<PtrState<PredState>>, UnitState>,
              StmtMultiAction<XcfaAction, XcfaAction>>,
        MultiPrec<XcfaPrec<PtrPrec<PredPrec>>, XcfaPrec<PtrPrec<PredPrec>>, UnitPrec>
      > {
      val mc = buildPredMultiConfig()
      return SafetyChecker { _: MultiPrec<*,*,*>? ->
        mc.check()
      }
    }

    private fun buildPredMultiConfig():
      MultiConfig<
        UnitState,
        XcfaState<PtrState<PredState>>,
        XcfaState<PtrState<PredState>>,
        XcfaAction,
        XcfaAction,
        XcfaPrec<PtrPrec<PredPrec>>,
        XcfaPrec<PtrPrec<PredPrec>>,
        UnitPrec,
        ExprMultiState<XcfaState<PtrState<PredState>>, XcfaState<PtrState<PredState>>, UnitState>,
        StmtMultiAction<XcfaAction, XcfaAction>,
      > {
        val errorPred = getXcfaErrorPredicate(ErrorDetection.ERROR_LOCATION)
        val target = Predicate<ExprMultiState<
            XcfaState<PtrState<PredState>>,
            XcfaState<PtrState<PredState>>,
            UnitState
        >> { ms ->
          val leftMatches = errorPred.test(ms.leftState)
          val rightMatches = errorPred.test(ms.rightState)
          val result = leftMatches && rightMatches
 
          if (result) {
              logger.write(
                  Logger.Level.INFO,
                  "Target predicate evaluated to TRUE! Error found in both program and witness:" +
                  "\n  - Left state (program): ${ms.leftState}" +
                  "\n  - Right state (witness): ${ms.rightState}"
              )
          }
          
          result
        }
        
        return StmtMultiConfigBuilder.ItpStmtMultiConfigBuilder(
            product = createPredProductAnalysis(),
            prop = True(),
            target = target,
            lRefToPrec = ItpRefToPredXcfaPrec(),
            rRefToPrec = ItpRefToPredXcfaPrec(),
            dRefToPrec = ItpRefToUnitPrec(),
            lInitPrec = XcfaPrec(PtrPrec(PredPrec.of(program.getGuards()))),
            rInitPrec = XcfaPrec(PtrPrec(PredPrec.of(witness.getGuards()))),
            dInitPrec = UnitPrec.getInstance(),
            solverFactory = solverFactory,
            logger = logger,
            pruneStrategy = PruneStrategy.FULL
        ).build()
    }

    private fun createPredProductAnalysis() =
      StmtMultiBuilder(createPredXcfaAnalysisSide(program), getXcfaLts())
        .addRightSide(createPredXcfaAnalysisSide(witness), getXcfaLts())
        .build(NextSideFunctions.Alternating(),
          object : InitFunc<UnitState, UnitPrec> {
            override fun getInitStates(prec: UnitPrec?) =
              listOf(UnitState.getInstance())
          }
        )

    private fun createPredXcfaAnalysisSide(xcfa: XCFA):
      MultiAnalysisSide<
        XcfaState<PtrState<PredState>>,
        UnitState,
        XcfaState<PtrState<PredState>>,
        XcfaAction,
        XcfaPrec<PtrPrec<PredPrec>>,
        XcfaPrec<PtrPrec<PredPrec>>,
    > {
        val analysis = PredXcfaAnalysis(
          xcfa = xcfa,
          solver = solverFactory.createSolver(),
          // TODO: check you have to create a new abstractor and solver everytime
          predAbstractor = PredAbstractors.cartesianAbstractor(solverFactory.createSolver()),
          partialOrd = getPartialOrder(PredOrd.create(solverFactory.createSolver()).getPtrPartialOrd()),
          isHavoc = true
        )

        return MultiAnalysisSide(
          analysis = analysis,
          controlInitFunc = analysis.getInitFunc(),
          combineStates = { control, data -> control },
          extractControlState = { state -> state },
          extractDataState = { state -> UnitState.getInstance() },
          extractControlPrec = { prec -> prec }
        )
    }

}
