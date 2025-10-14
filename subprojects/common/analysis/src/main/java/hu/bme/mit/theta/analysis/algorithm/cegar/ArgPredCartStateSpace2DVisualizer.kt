package hu.bme.mit.theta.analysis.algorithm.cegar

import com.google.common.base.Preconditions.checkNotNull
import com.google.common.base.Stopwatch
import hu.bme.mit.theta.analysis.Cex
import hu.bme.mit.theta.analysis.Prec
import hu.bme.mit.theta.analysis.algorithm.AlgorithmTimeoutException
import hu.bme.mit.theta.analysis.algorithm.Proof
import hu.bme.mit.theta.analysis.algorithm.SafetyChecker
import hu.bme.mit.theta.analysis.algorithm.SafetyResult
import hu.bme.mit.theta.analysis.runtimemonitor.MonitorCheckpoint
import hu.bme.mit.theta.analysis.utils.ProofVisualizer
import hu.bme.mit.theta.common.Utils
import hu.bme.mit.theta.common.exception.NotSolvableException
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.common.logging.Logger.Level
import hu.bme.mit.theta.common.logging.NullLogger
import hu.bme.mit.theta.common.visualization.writer.JSONWriter
import hu.bme.mit.theta.common.visualization.writer.WebDebuggerLogger
import java.time.Clock
import java.time.Duration
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import hu.bme.mit.theta.analysis.algorithm.predictors.ExpRLSPredictor1D
import kotlin.concurrent.schedule
import hu.bme.mit.theta.ui.TUI.warn
import hu.bme.mit.theta.ui.GUI.rlj
import hu.bme.mit.theta.ui.GUI
import hu.bme.mit.theta.ui.DEBUG
import com.raylib.java.core.input.Keyboard
import hu.bme.mit.theta.core.type.booltype.NotExpr;
import com.raylib.java.core.Color
import hu.bme.mit.theta.analysis.algorithm.arg.ARG
import hu.bme.mit.theta.analysis.pred.PredPrec
import hu.bme.mit.theta.analysis.pred.PredState
import hu.bme.mit.theta.xcfa.analysis.XcfaState
import hu.bme.mit.theta.xcfa.model.XcfaLocation
import kotlin.math.ceil
import kotlin.math.sqrt

class ArgPredCartStateSpace2DVisualizer(proof: ARG<*, *>, prec: PredPrec) {

    data class StateUi(val color: Color = Color.WHITE)

    val space2D: Map<Long, Map<XcfaLocation, StateUi>>

    private val PREDICATE_CELL_SIZE = 120
    private val LOCATION_SQUARE_SIZE = 10
    private val GAP = 5

    init {
        val predCount = prec.preds.size
        require(predCount <= 64) { "Cannot visualize a state space with more than 64 predicates." }
        val mutableSpace2D = mutableMapOf<Long, MutableMap<XcfaLocation, StateUi>>()

        proof.nodes.forEach { node ->
            val xcfaState = node.state as? XcfaState<*>
            val predState = (xcfaState?.sGlobal) as? PredState

            if (xcfaState != null && predState != null) {
                val location: XcfaLocation? = xcfaState.processes.values.firstOrNull()?.locs?.peek()

                if (location != null) {
                    val predStateId = getPredStateAsLong(prec, predState)
                    val locationMap = mutableSpace2D.getOrPut(predStateId) { mutableMapOf() }
                    val color = if (location.isError) Color.RED else Color.WHITE
                    locationMap[location] = StateUi(color)
                }
            }
        }
        this.space2D = mutableSpace2D
    }

    private fun getPredStateAsLong(prec: PredPrec, state: PredState): Long {
        val predCount = prec.preds.size
        require(predCount <= 64) { "Cannot generate a long key for more than 64 predicates." }

        if (predCount == 0) {
            return 0L
        }

        //     var bitmask = 0L
        //     prec.preds.forEachIndexed { index, basePred ->
        //         if (state.preds.contains(basePred)) {
        //             bitmask = bitmask or (1L shl index)
        //         }
        //     }

        // WARN: optimization wont work always
        if (state.preds.size != 1 && (state.preds.size != prec.preds.size)) {
            throw Error("how to compute this states id")
        }

        var bitmask = 0L
        state.preds.forEachIndexed { index, expr ->
            if (expr !is NotExpr) {
                bitmask = bitmask or (1L shl index)
            }
        }

        return bitmask
    }
 
    fun draw() {
        if (space2D.isEmpty()) return

        val outerGridSize = ceil(sqrt(space2D.size.toDouble())).toInt()
        if (outerGridSize == 0) return

        space2D.entries.forEachIndexed { index, entry ->
            val locations = entry.value
            val outerCol = index % outerGridSize
            val outerRow = index / outerGridSize

            val cellX = outerCol * (PREDICATE_CELL_SIZE + GAP)
            val cellY = outerRow * (PREDICATE_CELL_SIZE + GAP)

            drawStateWithLocations(locations, cellX, cellY)
        }
    }

    private fun drawStateWithLocations(locations: Map<XcfaLocation, StateUi>, offsetX: Int, offsetY: Int) {
        if (locations.isEmpty()) return

        val innerGridSize = ceil(sqrt(locations.size.toDouble())).toInt()
        if (innerGridSize == 0) return

        locations.entries.forEachIndexed { index, entry ->
            val stateUi = entry.value
            val innerCol = index % innerGridSize
            val innerRow = index / innerGridSize

            val squareX = offsetX + innerCol * (LOCATION_SQUARE_SIZE + GAP)
            val squareY = offsetY + innerRow * (LOCATION_SQUARE_SIZE + GAP)

            drawSquareAt(squareX, squareY, stateUi.color)
        }
    }

    private fun drawSquareAt(x: Int, y: Int, color: Color) {
      rl.shapes.DrawRectangle(x, y, LOCATION_SQUARE_SIZE, LOCATION_SQUARE_SIZE, color)
    }
}
