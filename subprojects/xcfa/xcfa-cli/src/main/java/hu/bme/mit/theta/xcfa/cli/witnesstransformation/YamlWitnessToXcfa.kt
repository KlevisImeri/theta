package hu.bme.mit.theta.xcfa.cli.witnesstransformation

import hu.bme.mit.theta.core.type.inttype.IntType
import hu.bme.mit.theta.core.type.Expr
import hu.bme.mit.theta.core.type.inttype.IntExprs.*
import hu.bme.mit.theta.xcfa.model.*
import hu.bme.mit.theta.xcfa.witnesses.*
import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.core.stmt.AssumeStmt
import hu.bme.mit.theta.xcfa.passes.HavocPromotionAndRange
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.common.logging.Logger.Level.*
import hu.bme.mit.theta.xcfa.passes.ProcedurePassManager
import hu.bme.mit.theta.c2xcfa.getExpressionFromC
import hu.bme.mit.theta.core.stmt.SkipStmt;
import hu.bme.mit.theta.xcfa.collectVars;
import hu.bme.mit.theta.core.type.booltype.BoolExprs;
import hu.bme.mit.theta.c2xcfa.CMetaData;
import hu.bme.mit.theta.frontend.transformation.model.types.complex.integer.*;
import hu.bme.mit.theta.core.decl.VarDecl;
import hu.bme.mit.theta.core.decl.Decls.Var
import hu.bme.mit.theta.frontend.transformation.model.statements.CStatement
import hu.bme.mit.theta.frontend.transformation.model.statements.CIf
import hu.bme.mit.theta.frontend.transformation.model.statements.CCall
// import package hu.bme.mit.theta.core.type.anytype.RefExpr;


// TODO: Add waypoints to witness XCFA
// TODO: remove sourc
// TODO: implement priv funcs HavocPromotionAndRange :42
// TODO: implement correctin witness
// TODO: do more tests
// TODO: CMetaData instald of EmptyMetadata
// NOTE: --lbe NO_LBE if you need it
// Balázs Rippl 🙂 Master (Mutli things)
// Milán Phd (Bounded Molel Cheking)

data class WitnessMetadata (
    val waypoint: WaypointContent
) : MetaData()  {
    override fun toString(): String = buildString {
        append(waypoint.type)
        append(':')
        append(waypoint.action)
        append(':')
        append(waypoint.location.line)
        
        waypoint.location.column?.let { append(":$it") }
        waypoint.location.function?.let { append(":$it") }
        
        waypoint.constraint?.let { constraint ->
            append(" ")
            append(constraint.value)
            constraint.format?.let { append(" (${it.name.lowercase()})") }
        }
    }

    override fun combine(other: MetaData): MetaData {
      return other
    }

    override fun isSubstantial(): Boolean {
      return false
    }
}


class YamlWitnessToXcfa(
  val witness: YamlWitness, 
  val program: XCFA, 
  val parseContext: ParseContext, 
  val logger: Logger,
  val warningLogger: Logger
) {

  lateinit var xcfaBuilder: XcfaBuilder; 
  lateinit var mainProcBuilder: XcfaProcedureBuilder;
  lateinit var locationMap: MutableMap<Location, XcfaLocation>;
  lateinit var currentLoc: XcfaLocation;
  lateinit var trapLoc: XcfaLocation;
  lateinit var errorLoc: XcfaLocation;
  lateinit var globalWaypointVar: XcfaGlobalVar
  lateinit var edgesMap: AssumeStmtRegistry;

  fun run(): Pair<XCFA, XCFA> {
    xcfaBuilder = XcfaBuilder("WitnessModel_${witness.metadata.uuid.take(5)}")
    locationMap = mutableMapOf<Location, XcfaLocation>()
    edgesMap = AssumeStmtRegistry()
    mainProcBuilder = XcfaProcedureBuilder("main", ProcedurePassManager());
    mainProcBuilder.createInitLoc()
    mainProcBuilder.createErrorLoc()
    currentLoc = mainProcBuilder.initLoc //TODO: add to location Map
    errorLoc = mainProcBuilder.errorLoc.get()
    trapLoc = newLocation(Location("Trap", -1))
    return when (witness.entryType) {
      EntryType.VIOLATION -> violationWitnessToXcfa()
      EntryType.INVARIANTS -> correctnessWitnessesToXcfa()
    }
  }

  private fun violationWitnessToXcfa(): Pair<XCFA, XCFA> {
    logger.write(
      Logger.Level.INFO,
      "Create the Witness XCFA\n",
    )
    // var unsignedIntType = CUnsignedInt(null, parseContext)
    globalWaypointVar = XcfaGlobalVar(
      wrappedVar = Var("waypoint", Int()),
      initValue = Int(0),
      threadLocal = false,
      atomic = true
    )
    xcfaBuilder.addVar(globalWaypointVar)

    var i = 0
    witness.content?.forEach { contentItem ->
        contentItem.segment?.forEach { waypoint ->
            waypointToXcfa(waypoint, i)
            i++
        }
    }

    xcfaBuilder.addProcedure(mainProcBuilder)
    xcfaBuilder.addEntryPoint(mainProcBuilder, emptyList())
    

    val programXcfaWithWaypoints = program.optimizeFurther(ProcedurePassManager(listOf(
      WitnessWaypointsPass(edgesMap, globalWaypointVar)
    )));
    val witnessXcfa = xcfaBuilder.build();

    return Pair(programXcfaWithWaypoints, witnessXcfa)
}

  private fun waypointToXcfa(waypoint: Waypoint, waypointvalue: Int) {

    val (type, constraint, location, action) = waypoint.waypoint
    val targetLoc = newLocation(location);
    val assumeWaypointStmt = AssumeStmt.create(
      Eq(
        globalWaypointVar.wrappedVar.getRef() as Expr<IntType>,
        Int(waypointvalue)
      ) //waypoint = waypointvalue
    )
    val waypointLabel = StmtLabel(assumeWaypointStmt)
    val metadata = WitnessMetadata(waypoint.waypoint)

    when (type) {

      WaypointType.ASSUMPTION -> {
        if(constraint==null) {
          throw IllegalArgumentException("For waypoint of type ASSUMPTION the constraint should not be null")
        }
        val (value, format) = constraint;
        if(format != Format.C_EXPRESSION && format!= null) {
          throw IllegalArgumentException("Only C_EXPRESSION is supported currently")
        }

        val sequenceLabel = SequenceLabel(listOf(
            waypointLabel,
            StmtLabel(CExpToAssumeStmt(value))
        ))

        when (action) {
          Action.FOLLOW -> {
            mainProcBuilder.addEdge(XcfaEdge(
              currentLoc,
              targetLoc,
              sequenceLabel,
              metadata
            ))
            currentLoc = targetLoc
          }
          Action.AVOID -> {
            toTrapNode(sequenceLabel, metadata)
          }
          else -> {
              throw IllegalArgumentException("Unknown action type: $action")
          }
        }

        edgesMap.put(WaypointKey(
          location.line,
          true,
          CStatement::class // or any or any children
        ), assumeWaypointStmt);
      }

      WaypointType.TARGET -> {
        if (action == Action.FOLLOW) {
          // mainProcBuilder.addEdge(XcfaEdge(
          //   currentLoc,
          //   targetLoc,
          //   NopLabel,
          //   metadata
          // ))
          // currentLoc = targetLoc

          mainProcBuilder.addEdge(XcfaEdge(
            currentLoc,
            errorLoc,
            SequenceLabel(listOf(
              waypointLabel,
              NopLabel
            )),
            metadata
          ))
          currentLoc = errorLoc
        } else {
           throw IllegalArgumentException("For waypoint of type TARGET only action FOLLOW is allowed. Current action: ${action}")
        }
        edgesMap.put(WaypointKey(
          location.line,
          true,
          CStatement::class
        ), assumeWaypointStmt);
      }

      WaypointType.FUNCTION_ENTER -> {
        throw TODO("Not implmented FUNCTION_ENTER")
      }

      WaypointType.FUNCTION_RETURN -> {
        val label = getStmtLabelAtLocation(location)
        when (action) {
          Action.FOLLOW -> {
            mainProcBuilder.addEdge(XcfaEdge(
              currentLoc,
              targetLoc,
              SequenceLabel(listOf(waypointLabel, label)),
              metadata,
            ))
            currentLoc = targetLoc
          }
          Action.AVOID -> {
             throw IllegalArgumentException("We don't know what to do when you avoid a function return!!!")
            // toTrapNode(currentLoc, ?)  TODO: what type of label ...
          }
          else -> {
            throw IllegalArgumentException("Unknown action type: $action")
          }
        }
        edgesMap.put(WaypointKey( //TODO: you probably need more precision even the column
          location.line,
          false,
          CCall::class 
        ), assumeWaypointStmt);
      }

      WaypointType.BRANCHING -> {
        if(constraint==null) throw IllegalArgumentException("For $action the constraint can not be empty!") 
        val value = constraint.value;

        if(value=="true" || value=="false") {

          val label = SequenceLabel(listOf(
            waypointLabel,
            StmtLabel(
              SkipStmt.getInstance(),
              if (value=="true") ChoiceType.MAIN_PATH else ChoiceType.ALTERNATIVE_PATH
            ),
          ));

          when (action) {
            Action.FOLLOW -> {
             mainProcBuilder.addEdge(XcfaEdge(
                currentLoc,
                targetLoc,
                label,
                metadata,
              ))
              currentLoc = targetLoc
            }
            Action.AVOID -> {
              toTrapNode(label, metadata)
            }
            else -> {
                throw IllegalArgumentException("Unknown action type: $action")
            }
          }

        } else { //switch statement
          throw TODO("How do you even label a switch?")
        }
        edgesMap.put(WaypointKey(
          location.line,
          false,
          CIf::class
        ), assumeWaypointStmt);
      }

      WaypointType.RECURRENCE_CONDITION -> {
        throw TODO("Not implmented RECURRENCE_CONDITION!")
      }

    }
  }

// Semantics. The correctness witness is valid if it fulfills the following requirements.
//     Each must always hold immediately before evaluating the condition of the corresponding loop.
//     Each must always hold immediately before evaluating the corresponding statement or declaration.
//     The specification must be satisfied for all program executions.
//     No invariant evaluation causes undefined behavior and no undefined behavior occurs during any execution of the program.
// Note that the order of invariants in an or their division into several entries of type is not important. The semantics also reveals the difference between the two types of invariants: if we replace with , then the invariant has to hold only before the loop is executed, but not after each loop iteration.
  // Idea is that you create a label where you have the exact location and the statement.
  // This is the same for all types of statemetns.
  // val loopInvariant: Map<Location, AssumeStmt>;
  private fun correctnessWitnessesToXcfa(): Pair<XCFA, XCFA> {
    // witness.content.forEach { contentItem ->
    //   val (type, location, value, format) = contentItem.
    //   if(format != C_EXPRESSION) {
    //     throw IllegalArgumentException("Only C_EXPRESSION is supported currently")
    //   }
    //   val (targetLoc, new) = newLocation(location);
    //   val assumeStmt = CExpToAssumeStmt(value);
    //   if(loopInvariant.find(prevLocation)) {
    //     assumeStmt += loopInvarinat[prevLocatoin]
    //   }
    //
    // }

    xcfaBuilder.addProcedure(mainProcBuilder)
    xcfaBuilder.addEntryPoint(mainProcBuilder, emptyList())
    return Pair(program, xcfaBuilder.build());
  }

  // private fun findPrevLocation(xcfaLoc: XcfaLocation, loc: Location): XcfaLocation {
  //   val orderdArrayOfLocations = locationMap to array orderd by locatoin
  //   return binarySearchPrevLocation(OrderdArrayOfLcoations)
  // }

  var numberOfnodes=0;
  private fun newLocation(location: Location): XcfaLocation {
    return locationMap.getOrPut(location) {
      val funcName = location.function ?: ""
      val newXcfaLocation = XcfaLocation(
        // name = "\"${location.fileName}:${funcName}:L${location.line}:${location.column ?: 0}\"",
        name = "q$numberOfnodes",
        metadata = EmptyMetaData
      )
      numberOfnodes++;
      mainProcBuilder.addEdge(XcfaEdge(
        newXcfaLocation,
        newXcfaLocation,
        NopLabel,
        EmptyMetaData 
      ))
      return newXcfaLocation;
    }
  }

  private fun toTrapNode(label: XcfaLabel, metadata: WitnessMetadata) { 
    mainProcBuilder.addEdge(XcfaEdge(
      currentLoc,
      trapLoc,
      label,
      metadata
    ))
  }

  private fun getStmtLabelAtLocation(loc: Location): StmtLabel {
      return StmtLabel(AssumeStmt.of(BoolExprs.True())); // TODO:
      //throw IllegalArgumentException("We only support some function for their return!")
  }

  private fun CExpToAssumeStmt(value: String): AssumeStmt {
    return AssumeStmt.of(getExpressionFromC(
        value,
        parseContext,
        false,
        false,
        warningLogger,
        program.collectVars(),  // TODO: i have a feeling i have to filter only the nessesary vars
      ));
  }

}

