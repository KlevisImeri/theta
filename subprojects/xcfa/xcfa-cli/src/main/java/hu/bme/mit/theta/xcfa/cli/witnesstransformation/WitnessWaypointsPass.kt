package hu.bme.mit.theta.xcfa.cli.witnesstransformation

import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.xcfa.model.XcfaProcedureBuilder
import hu.bme.mit.theta.xcfa.model.XcfaEdge
import hu.bme.mit.theta.xcfa.model.XcfaGlobalVar
import hu.bme.mit.theta.xcfa.model.SequenceLabel
import hu.bme.mit.theta.xcfa.model.StmtLabel
import hu.bme.mit.theta.xcfa.passes.ProcedurePass
import kotlin.reflect.KClass
import hu.bme.mit.theta.frontend.transformation.model.statements.CStatement
import hu.bme.mit.theta.frontend.transformation.model.statements.CCompound
import hu.bme.mit.theta.core.stmt.AssumeStmt
import hu.bme.mit.theta.core.stmt.AssignStmt
import hu.bme.mit.theta.c2xcfa.getCMetaData
import hu.bme.mit.theta.core.type.anytype.RefExpr;
import hu.bme.mit.theta.core.type.inttype.IntExprs.Add;
import hu.bme.mit.theta.core.type.inttype.IntExprs.Int
// import hu.bme.mit.theta.core.type.inttype.IntExprs;
import hu.bme.mit.theta.core.type.inttype.IntType
import hu.bme.mit.theta.core.type.Expr;

data class WaypointKey(
  val lineStart: Int,
  val endInSameLine: Boolean,
  val type: KClass<out CStatement>
)

class AssumeStmtRegistry {
  private val map = mutableMapOf<
    WaypointKey,
    MutableList<Pair<KClass<out CStatement>, AssumeStmt>>
  >()

  fun put(key: WaypointKey, stmt: AssumeStmt) {
    map.getOrPut(key) { mutableListOf() }
       .add(key.type to stmt)
  }

  fun get(query: WaypointKey): AssumeStmt? {
    return map[query]
      ?.firstOrNull { (storedType, _) ->
        storedType.java.isAssignableFrom(query.type.java)
      }
      ?.second
  }
  // fun get(query: WaypointKey): AssumeStmt? {
  //   println("===== get() called =====")
  //   println("Query key: $query")
  //   println("Query type: ${query.type.java.name}")
  //   
  //   val candidates = map[query]
  //   println("Candidates found in map: ${candidates?.size ?: 0}")
  //   
  //   candidates?.forEachIndexed { index, (storedType, stmt) ->
  //       val isAssignable = storedType.java.isAssignableFrom(query.type.java)
  //       println("  Candidate #$index:")
  //       println("    Stored type: ${storedType.java.name}")
  //       println("    Stored stmt: $stmt")
  //       println("    isAssignable: $isAssignable")
  //   }
  //   
  //   return candidates?.firstOrNull { (storedType, _) ->
  //       storedType.java.isAssignableFrom(query.type.java)
  //   }?.second.also { result ->
  //       println("Selected result: $result")
  //       println("===== get() end =====")
  //   }
  // }
}

class WitnessWaypointsPass(
  private val edgesMap: AssumeStmtRegistry,
  private val globalWaypointVar: XcfaGlobalVar,
) : ProcedurePass {

//  data class XcfaEdge(
//   val source: XcfaLocation, // source location
//   val target: XcfaLocation, // target location
//   val label: XcfaLabel = NopLabel, // edge label
//   val metadata: MetaData,
// ) { 
  fun XcfaEdge.edgeToWaypointKeyStmt(): AssumeStmt? {
    if (getCMetaData() == null) return null
    for (cstmt in getCMetaData()!!.astNodes) {
        val stmt = cstmt.edgeToWaypointKeyStmt()
        if (stmt != null) return stmt
    }
    return null
  }
  // fun XcfaEdge.edgeToWaypointKeyStmt(): AssumeStmt? {
  //   println("[edgeToWaypointKeyStmt] Checking edge: $this")
  //   
  //   if (getCMetaData() == null) {
  //       println("[edgeToWaypointKeyStmt] No CMetaData found - returning null")
  //       return null
  //   }
  //   
  //   val astNodes = getCMetaData()!!.astNodes
  //   println("[edgeToWaypointKeyStmt] Found ${astNodes.size} AST nodes to process")
  //   
  //   for ((index, cstmt) in astNodes.withIndex()) {
  //       println("[edgeToWaypointKeyStmt] Processing AST node #${index + 1}: $cstmt")
  //       val stmt = cstmt.edgeToWaypointKeyStmt()
  //       if (stmt != null) {
  //           println("[edgeToWaypointKeyStmt] Found matching statement: $stmt")
  //           return stmt
  //       }
  //   }
  //   
  //   println("[edgeToWaypointKeyStmt] No matching statements found - returning null")
  //   return null
  // }
// public abstract class CStatement {
//     private Optional<CStatement> parent = Optional.empty();
//     protected final ParseContext parseContext;
//     private String id;
//     protected static int counter = 0;
//     protected CStatement preStatements;
//     protected CStatement postStatements;
//
//     private int lineNumberStart = -1;
//     private int colNumberStart = -1;
//     private int lineNumberStop = -1;
//     private int colNumberStop = -1;
//     private int offsetStart = -1;
//     private int offsetEnd = -1;
//     private String sourceText = "";
//     private ParserRuleContext ctx;
  fun CStatement.edgeToWaypointKeyStmt(): AssumeStmt? {
    return edgesMap.get(
      WaypointKey(
        lineNumberStart,
        lineNumberStop == lineNumberStart,
        this::class
      )
    )
  }
// public class CCompound extends CStatement {
//
//     private final List<CStatement> cStatementList;
  fun CCompound.edgeToWaypointKeyStmt(): AssumeStmt? {
    for (cstmt in getcStatementList()) {
      val stmt = cstmt.edgeToWaypointKeyStmt()
      if (stmt != null) return stmt
    }
    return null;
  }


  // TODO: When need implment it right now we dont
  //
  //  CAssignment.java
  //  CAssume.java
  //  CBreak.java
  //  CCall.java
  //  CCase.java
  //  CContinue.java
  //  CDecls.java
  //  CDefault.java
  //  CDoWhile.java
  //  CExpr.java
  //  CFor.java
  //  CFunction.java
  //  CGoto.java
  //  CIf.java
  //  CInitializerList.java
  //  CNullStatement.kt
  //  CProgram.java
  //  CRet.java
  //  CStatementVisitor.java
  //  CStatementVisitorBase.java
  //  CSwitch.java
  //  CWhile.java


  override fun run(builder: XcfaProcedureBuilder): XcfaProcedureBuilder {
    // TODO: Varchanger class if you also need for program XCFA to have a global waypoint
    for (edge in builder.getEdges().toList()) {
      // println("Iterating over ${edge}")
      val stmt = edge.edgeToWaypointKeyStmt()


      if (stmt != null) {
        println("Found a maching waypoint ${stmt}")
    // logger.write(
    //   Logger.Level.INFO,
    //   "Create the Witness XCFA\n",
    // )
        val baseLabels = when (val lbl = edge.label) {
          is SequenceLabel -> lbl.labels
          else       -> listOf(lbl)
        }

        val waypointVarDecl = globalWaypointVar.wrappedVar
        val waypointRef = waypointVarDecl.getRef() as Expr<IntType>

        val incrementExpr = Add(
            listOf<Expr<IntType>>(
                waypointRef,
                Int(1)
            )
        )

        val newLabel = SequenceLabel(listOf(
           StmtLabel(stmt),
          *baseLabels.toTypedArray(),
          // StmtLabel(AssignStmt.of(
          //   waypointVarDecl,
          //   incrementExpr
          // ))
        )) //TODO: maybe have to add it before or after the Stmt's
        val newEdge = edge.withLabel(newLabel)

        builder.removeEdge(edge)
        builder.addEdge(newEdge)
      }
    }
    return builder
  }
}
