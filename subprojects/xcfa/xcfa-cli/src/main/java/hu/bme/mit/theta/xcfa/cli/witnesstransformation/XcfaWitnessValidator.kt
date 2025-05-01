package hu.bme.mit.theta.xcfa.validation

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


// TODO: For the moment this is only for reach error

class XcfaWitnessValidatorConfigBuilder(
  program: XCFA,
  witness: XCFA,
  solverFactory: SolverFactory,
  logger: logger
) {

    companion object {
      private val multiUnitPrec = MultiPrec(
        UnitPrec.getInstance(),
        UnitPrec.getInstance(),
        UnitPrec.getInstance()
      )
    }

    private fun build():
      MultiConfig<
        DataState,
        LControl,
        RControl,
        LAction,
        RAction,
        LPrec,
        RPrec,
        DataPrec,
        ExprMultiState<LControl, RControl, DataState>,
        StmtMultiAction<LAction, RAction>,
      > {
        return  StmtMultiConfigBuilder.ItpStmtMultiConfigBuilder(
            product = createProductAnalysis(),
            prop = yourBooleanExpr,
            target = yourTargetPredicate,
            lRefToPrec = leftRefutationConverter,
            rRefToPrec = rightRefutationConverter,
            dRefToPrec = dataRefutationConverter,
            lInitPrec = multiUnitPrec,
            rInitPrec = multiUnitPrec,
            dInitPrec = UnitPrec.getInstance(),
            solverFactory = solverFactory,
            logger = logger,
            pruneStrategy = PruneStrategy.FULL
        ).build()
    }

    private fun createProductAnalysis() {
        return StmtMultiBuilder(createXcfaAnalysisSide(program), getXcfaLts())
              .addRightSide(createProductAnalysis(witness), getXcfaLts())
              .build(NextSideFunctions.Alternating(),
                object : InitFunc<UnitState, UnitPrec> {
                  override fun getInitStates(prec: UnitPrec?): Collection<UnitState?>? =
                    listOf(UnitState.getInstance())
                }
              )
    }

    private fun createPredXcfaAnalysisSide(xcfa: XCFA):
      MultiAnalysisSide<
        XcfaState<PtrState<PredState>>,
        UnitState,
        XcfaState,
        XcfaAction,
        XcfaPrec<PtrPrec<PredPrec>>,
        UnitPrec
    > {
        val analysis = PredXcfaAnalysis(
          xcfa = xcfa,
          solver = solver,
          // TODO: check you have to create a new abstractor and solver everytime
          predAbstractor = PredAbstractors.cartesianAbstractor(solverFactory.createSolver()),
          partialOrd = getPartialOrder(PredOrd.create(solver).getPtrPartialOrd()),
          isHavoc = true
        )

        return MultiAnalysisSide(
          analysis = analysis,
          controlInitFunc = InitFunc<XcfaState, UnitPrec> { prec ->
            getPredXcfaInitFunc(xcfa, PredAbstractors.cartesianAbstractor(solverFactory.createSolver()))
          },
          combineStates = { control, data -> control },
          extractControlState = { state -> state },
          extractDataState = { state -> UnitState.getInstance() },
          extractControlPrec = { prec -> prec.p }
        )
    }

}
