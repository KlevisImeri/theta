package hu.bme.mit.theta.xcfa.cli.utils

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
import hu.bme.mit.theta.ui.DEBUG.debug
import com.raylib.java.core.input.Keyboard
import hu.bme.mit.theta.core.type.booltype.NotExpr;
import com.raylib.java.core.Color
import hu.bme.mit.theta.analysis.algorithm.arg.ARG
import hu.bme.mit.theta.analysis.pred.PredPrec
import hu.bme.mit.theta.analysis.pred.PredState
import hu.bme.mit.theta.analysis.ptr.PtrState
import hu.bme.mit.theta.xcfa.analysis.XcfaState
import hu.bme.mit.theta.xcfa.model.XcfaLocation
import kotlin.math.ceil
import kotlin.math.sqrt


class ArgPredCartStateSpace2DVisualizer(proof: ARG<*, *>, prec: PredPrec) {

    data class StateUi(
      val id: Long = -1L,
      val color: Color = Color.WHITE, 
      val borderColor: Color = Color.BLACK,
      val borderSize: Int = 1,
      val fontSize: Int = 12
    )

    val space2D: Map<Long, Map<XcfaLocation, StateUi>>
    val locs: Set<XcfaLocation> 
    // private val PREDICATE_CELL_SIZE = 120
    // private val LOCATION_SQUARE_SIZE = 10
    // private val PREDICATE_CELL_SIZE = 100
    private val LOCATION_SQUARE_SIZE = 20
    private val FONT_SIZE = 20;
    private val GAP = 0
    

    init {
        debug("Initializing ArgPredCartStateSpace2DVisualizer")
        val predCount = prec.preds.size
        require(predCount <= 64) { "Cannot visualize a state space with more than 64 predicates." }
        
        // Calculate total number of predicate combinations (2^predCount)
        val totalCombinations = 1L shl predCount
        val mutableSpace2D = mutableMapOf<Long, MutableMap<XcfaLocation, StateUi>>()
        
        debug("|ARG| = ${proof.nodes.count()}")

        val _locs = mutableSetOf<XcfaLocation>()
        proof.nodes.forEach { node ->
            val xcfaState = node.state as XcfaState<*>
            val location: XcfaLocation? = xcfaState.processes.values.firstOrNull()?.locs?.peek()
            if (location != null) {
                _locs.add(location)
            }
        }
        this.locs = _locs;

        for (index in 0 until totalCombinations) {
            val locationMap = mutableMapOf<XcfaLocation, StateUi>()
            locs.forEach { location ->
                locationMap[location] = StateUi(id = index)
            }
            mutableSpace2D[index] = locationMap
        }

        proof.nodes.forEach { node ->
            val xcfaState = node.state as XcfaState<*>
            val predState = (xcfaState.sGlobal as PtrState<*>).innerState as PredState
            val location: XcfaLocation? = xcfaState.processes.values.firstOrNull()?.locs?.peek()
            // debug("$xcfaState");
            if (location != null) {
                val predStateId = getPredStateAsLong(prec, predState)
                val locationMap = mutableSpace2D.getOrPut(predStateId) { mutableMapOf() }
                val color = if (location.error) Color.RED else Color.GRAY
                locationMap[location] = StateUi(id = predStateId, color = color)
                debug("(loc,bitmask) = ($location, $predStateId)")
            } else {
                debug("(loc,bitmask) = null")
            }
        }
        
        this.space2D = mutableSpace2D
        // debug("${space2D}")
        debug("ArgPredCartStateSpace2DVisualizer initialized")
    }

    private fun getPredStateAsLong(prec: PredPrec, state: PredState): Long {
        val predCount = prec.preds.size
        require(predCount <= 64) { "Cannot generate a long key for more than 64 predicates." }

        if (predCount == 0) {
            return 0L
        }
        
        var bitmask = 0L
        prec.preds.forEachIndexed { index, basePred ->
            var res = false;
            if (state.preds.contains(basePred)) {
              res = true;
              bitmask = bitmask or (1L shl index)
            } else {
              res = false;
            }
            // debug("${basePred} ∈? ${state.preds} => $res")
        }
        // debug(" bitmaks = $bitmask")

        // // WARN: optimization wont work always
        // if (state.preds.size > 1 && (state.preds.size != prec.preds.size)) {
        //     throw Error("how to compute this states id")
        // }
        //
        // var bitmask = 0L
        // state.preds.forEachIndexed { index, expr ->
        //     if (expr !is NotExpr) {
        //         bitmask = bitmask or (1L shl index)
        //     }
        // }

        return bitmask
    }
    
     
    fun draw() {
        // debug("Drawing the stateSpace");
        if (space2D.isEmpty()) return
        // debug("After Drawing the stateSpace");


        val outerGridSize = ceil(sqrt(space2D.size.toDouble())).toInt()
        if (outerGridSize == 0) return
        val innerGridSize = ceil(sqrt(locs.size.toDouble())).toInt()
        val predicateCellSize = innerGridSize * LOCATION_SQUARE_SIZE;

        space2D.entries.forEach{ entry ->
            val index = entry.key
            val locations = entry.value
            val outerCol = index % outerGridSize
            val outerRow = index / outerGridSize

            val cellX = outerCol * (predicateCellSize + GAP)
            val cellY = outerRow * (predicateCellSize + GAP)

            drawStateWithLocations(index, cellX, cellY)

            // val binaryString = Integer.toBinaryString(index.toInt()) 
            val binaryString = index.toString();
            val textMeasurement = rlj.text.MeasureTextEx(rlj.text.GetFontDefault(), binaryString, FONT_SIZE.toFloat(), 0f)

            val textX = (cellX + predicateCellSize / 2 - textMeasurement.x / 2).toInt()
            val textY = (cellY + predicateCellSize / 2 - textMeasurement.y / 2).toInt()

            rlj.text.DrawText(
              binaryString, 
              textX,
              textY,
              FONT_SIZE,
              Color.BLACK
            )
        }
    }

    private fun drawStateWithLocations(id: Long, offsetX: Long, offsetY: Long) {
        val locations = space2D[id];
        if (locations == null || locations.isEmpty()) return

        val innerGridSize = ceil(sqrt(locations.size.toDouble())).toInt()
        if (innerGridSize == 0) return
          
        locations.entries.forEachIndexed { index, entry ->
            val stateUi = entry.value
            val innerCol = index % innerGridSize
            val innerRow = index / innerGridSize

            val squareX = offsetX + innerCol * (LOCATION_SQUARE_SIZE + GAP)
            val squareY = offsetY + innerRow * (LOCATION_SQUARE_SIZE + GAP)

            drawSquareAt(squareX, squareY, stateUi)
        }
    }

    private fun drawSquareAt(x: Long, y: Long, stateUi: StateUi) {
        val (id, mainColor, borderColor, borderSize, fontSize) = stateUi

        rlj.shapes.DrawRectangle(
            (x - borderSize).toInt(),
            (y - borderSize).toInt(),
            LOCATION_SQUARE_SIZE + 2 * borderSize, 
            LOCATION_SQUARE_SIZE + 2 * borderSize, 
            borderColor
        )

        rlj.shapes.DrawRectangle(
            x.toInt(),
            y.toInt(),
            LOCATION_SQUARE_SIZE, 
            LOCATION_SQUARE_SIZE, 
            mainColor
        )
    }
}
