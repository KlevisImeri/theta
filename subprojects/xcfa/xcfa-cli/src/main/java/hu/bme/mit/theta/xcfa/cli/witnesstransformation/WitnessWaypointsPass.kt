package hu.bme.mit.theta.xcfa.cli.witnesstransformation

import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.xcfa.model.XcfaProcedureBuilder
import hu.bme.mit.theta.xcfa.model.XcfaEdge
import hu.bme.mit.theta.xcfa.model.XcfaGlobalVar
import hu.bme.mit.theta.xcfa.model.SequenceLabel
import hu.bme.mit.theta.xcfa.model.StmtLabel
import hu.bme.mit.theta.xcfa.model.ChoiceType
import hu.bme.mit.theta.xcfa.model.XcfaLabel
import hu.bme.mit.theta.xcfa.passes.ProcedurePass
import kotlin.reflect.KClass
import hu.bme.mit.theta.frontend.transformation.model.statements.CStatement
import hu.bme.mit.theta.frontend.transformation.model.statements.CCompound
import hu.bme.mit.theta.core.stmt.Stmt
import hu.bme.mit.theta.core.stmt.AssumeStmt
import hu.bme.mit.theta.core.stmt.AssignStmt
import hu.bme.mit.theta.c2xcfa.getCMetaData
import hu.bme.mit.theta.core.type.anytype.RefExpr
import hu.bme.mit.theta.core.type.inttype.IntExprs.Add
import hu.bme.mit.theta.core.type.inttype.IntExprs.Int
import hu.bme.mit.theta.core.type.inttype.IntType
import hu.bme.mit.theta.core.type.Expr
import hu.bme.mit.theta.core.decl.VarDecl

data class WaypointKey(
    val lineStart: Int,
    val endInSameLine: Boolean = false,
    val path: ChoiceType = ChoiceType.NONE,
    val type: KClass<out CStatement> = CStatement::class
)

class SequenceLabelRegistry {
    data class PositionKey(
        val lineStart: Int,
        val endInSameLine: Boolean,
        val path: ChoiceType
    )
    
    private val map = mutableMapOf<
        PositionKey,
        MutableList<Pair<KClass<out CStatement>, SequenceLabel>>
    >()

    fun put(key: WaypointKey, label: SequenceLabel) {
        val pos = PositionKey(key.lineStart, key.endInSameLine, key.path)
        map.getOrPut(pos) { mutableListOf() }
           .add(key.type to label)
    }

    fun get(query: WaypointKey): SequenceLabel? {
        val pos = PositionKey(query.lineStart, query.endInSameLine, query.path)
        return map[pos]
            ?.firstOrNull { (storedType, _) ->
                storedType.java.isAssignableFrom(query.type.java)
            }
            ?.second
    }
}

class WitnessWaypointsPass(
    private val edgesMap: SequenceLabelRegistry,
    private val globalVars: Set<XcfaGlobalVar>
) : ProcedurePass {

    fun XcfaEdge.edgeToWaypointKeyLabel(): SequenceLabel? {
        if (getCMetaData() == null) return null

        var path = ChoiceType.NONE
        val sequenceLabel = label
        if (sequenceLabel is SequenceLabel) {
            for (currentLabel in sequenceLabel.labels) {
                if (currentLabel is StmtLabel && currentLabel.choiceType != ChoiceType.NONE) {
                    path = currentLabel.choiceType
                    break
                }
            }
        }

        for (cstmt in getCMetaData()!!.astNodes) {
            val label = cstmt.edgeToWaypointKeyLabel(path)
            if (label != null) return label
        }
        return null
    }

    fun CStatement.edgeToWaypointKeyLabel(path: ChoiceType): SequenceLabel? {
        return edgesMap.get(
            WaypointKey(
                lineNumberStart,
                lineNumberStop == lineNumberStart,
                path,
                this::class
            )
        )
    }

    fun CCompound.edgeToWaypointKeyLabel(path: ChoiceType): SequenceLabel? {
        for (cstmt in getcStatementList()) {
            val label = cstmt.edgeToWaypointKeyLabel(path)
            if (label != null) return label
        }
        return null
    }

    override fun run(builder: XcfaProcedureBuilder): XcfaProcedureBuilder {
        for (gVar in globalVars) {
            builder.parent.addVar(gVar)
        }

        for (edge in builder.getEdges().toList()) {
            val waypointLabel = edge.edgeToWaypointKeyLabel()

            if (waypointLabel != null) {
                val baseLabels = when (val lbl = edge.label) {
                    is SequenceLabel -> lbl.labels
                    else -> listOf(lbl)
                }

                val combinedLabels = mutableListOf<XcfaLabel>().apply {
                    addAll(waypointLabel.labels)
                    addAll(baseLabels)
                }
                
                val newLabel = SequenceLabel(combinedLabels)
                val newEdge = edge.withLabel(newLabel)

                builder.removeEdge(edge)
                builder.addEdge(newEdge)
            }
        }
        return builder
    }
}
