// Idea is that you create a label where you have the exact location and the statement.
// This is the same for all types of statemetns.

package hu.bme.mit.theta.xcfa.cli.witnesses
import hu.bme.mit.theta.xcfa.model
import hu.bme.mit.theta.core.decl.VarDecl
import hu.bme.mit.theta.core.type.inttype.IntType
import hu.bme.mit.theta.core.type.Expr
import hu.bme.mit.theta.core.type.inttype.IntExprs.Int
import hu.bme.mit.theta.xcfa.model.*
import hu.bme.mit.theta.xcfa.cli.witnesses.*
import hu.bme.mit.theta.c2xcfa

class YamlWitnessToXcfa(witness: YamlWitness, logger: Logger) {
  
  val xcfaBuilder: XcfaBuilder; 
  val mainProcBuilder: XcfaProcedureBuilder;

  val locationMap: MutableMap<Location, XcfaLocation> = mutableMapOf()
  private fun addLocation(location: Location, error: Boolean = false): XcfaLocation {
    return locationMap.getOrPut(location) {
      val funcName = location.function ?: "unknown"
      val errorTag = if (error) "[Error]" else ""
      XcfaLocation(
        name = "$errorTag${location.fileName}:${funcName}:L${location.line}:${location.column ?: 0}",
        error = error,
        metadata = EmptyMetaData
      )
    }
  }

  var currentLoc: XcfaLocation;

  fun toXcfa(): XCFA {
    xcfaBuilder = XcfaBuilder("WitnessModel_${witness.metadata.uuid.take(5)}")
    locationMap = mutableMapOf<Location, XcfaLocation>()
    mainProcBuilder  = XcfaProcedureBuilder("main", ProcedurePassManager()) 
    mainProcBuilder.createInitLoc()
    mainProcBuilder.createFinalLoc()
    currentLoc = mainProcBuilder.initLoc
    return when (witness.entryType) {
      EntryType.VIOLATION -> violationWitnessToXcfa()
      EntryType.INVARIANTS -> correctnessWitnessesToXcfa()
    }
  }

  private fun ViolationWitnessToXcfa(): XCFA {    
    witness.content.forEach { contentItem ->
      contentItem.segment?.let { segment ->
        segment.forEach { waypoint ->
          waypointToXcfa(waypoint)
        }
      }
    }

    // Ensure there's a path to the final location
    if (currentLoc != mainBuilder.finalLoc.get()) {
      mainBuilder.addEdge(XcfaEdge(currentLoc, mainBuilder.finalLoc.get(), listOf(ReturnAction(Int(0)))))
    }

    // Add the main procedure to the XCFA
    xcfaBuilder.addProcedure(mainBuilder)
    xcfaBuilder.addEntryPoint(mainBuilder, emptyList())
  }
 
  private fun waypointToXcfa(waypoint: Waypoint) {
    val (type, constraint, location, action) = waypoints.waypoint

    val targetLoc = addLocation(location);

    when (type) {

      WaypointType.ASSUMPTION -> {
        if(constraint==null) {
          throw IllegalArgumentException("For waypoint of type ASSUMPTION the constraint shoudl not be null")
        }
        val (value, format) = constraint;
        if(format != C_EXPRESSION or != null) {
          throw IllegalArgumentException("Only  C_EXPRESSION is supported currently")
        }
        Expr<*> exp = parseCExpression(
          value,
          vars: Map<VarDecl<*>, CComplexType>,
          scope: List<String>,
          logger,
        );


        if(action == Action.AVOID)) {

        } else if (action == Action.FOLLOW) {
            val actions = mutableListOf<XcfaAction>()
            actions.add(CommentAction("Assumption: ${constraint.value}"))
            builder.addEdge(XcfaEdge(currentLoc, targetLoc, actions))
            currentLoc = targetLoc
        } else {
          throw IllegalArgumentException("Unknown action type: ${action}")
        }
      }

      WaypointType.TARGET -> {
        if (action == Action.FOLLOW) {
          val errorLoc =  addLocation(location, true);
          builder.addEdge(XcfaEdge(
            currentLoc,
            errorLoc,
            EmptyMetaData
          ))
          builder.addEdge(XcfaEdge(
            errorLoc, 
            mainBuilder.finalLoc.get(), 
            ReturnLabel(XcfaLabel(EmptyMetaData), //TODO:return 1
            EmptyMetaData
          )

          currentLoc = errorLoc
        } else {
           throw IllegalArgumentException("For waypoint of type TARGET only action FOLLOW is allowed. Current action: ${action}")         
        }
      }

        WaypointType.FUNCTION_ENTER -> {
          if (action == Action.FOLLOW) {
            currentLoc = targetLoc
          }
        }

        WaypointType.FUNCTION_RETURN -> {
          if (action == Action.FOLLOW) {
            // Function return waypoint
            currentLoc = targetLoc
          }
        }
        // The file:line:col is included in the label or metatdata of the edge
        //
        // main.c:
        // 1. int main() {
        // 2.   int x=0;
        // 3.   while(x<5){
        // 4.     x++;
        // 5.   }
        // 6.   if(x==5) error()
        // 7.   return 0;
        // 8. }

        // XcfaProgram:       ----------------
        //                    ↓              |
        // [1] ---int x=0--> [2] ---x<5---> [3]
        //                    |
        //                   x>=5
        //                    ↓
        //                   [4]----x==5----->[error]
        //                    |
        //                   x<5
        //                    ↓
        //                  [exit]

        // entry_type: violation_sequence
        // metadata: <... >
        // content:
        //  - segment:
        //    - waypoint:
        //      action: follow
        //      type: assumtion
        //      location:
        //        file_name: "main.c"
        //        line: 3
        //        constraint:
        //          value: "int x=0"
        //  - segment:
        //    - waypoint:
        //      action: avoid
        //      type: branching
        //      location:
        //        file_name: "main.c"
        //        line: 3
        //        constraint:
        //          value: x>5
        //    - waypoint:
        //      action: follow
        //      type: branching
        //      location:
        //        file_name: "main.c"
        //        line: 3
        //        constraint:
        //          value: true
        //  - segment:
        //    - waypoint:
        //      action: follow
        //      type: branching
        //      location:
        //        file_name: "main.c"
        //        line: 3
        //        constraint:
        //          value: true 
        //  - segment:
        //    - waypoint:
        //      action: follow
        //      type: branching
        //      location:
        //        file_name: "main.c"
        //        line: 3
        //        constraint:
        //          value: false
        //  - segment:
        //    - waypoint:
        //      action: follow
        //      type: target
        //      location:
        //        file_name: "main.c"
        //        line: 6
        
        // XcfaViolationWitness:
        //
        // [1] ---int x=0--> [2] ---x>5---> [error]
        //                    |
        //                   x<=5
        //                    ↓
        //                   [3] ---true---> [4] ---true---> [5] ---false---> [target]

        WaypointType.BRANCHING -> {
          if (action == Action.FOLLOW) {
            val targetLoc = addLocation(location);
            builder.addEdge(XcfaEdge(
              currentLoc 
              targetLoc,
              StmtLabel(
                NoStmt(), // TODO: empty Stmt
                if (constraint.value) ChoiceType.MAIN_PATH else ChoiceType.ALTERNATIVE_PATH
              ),
              EmptyMetaData,
            ))
            currentLoc = targetLoc
          }
        }

      }
    }
  }




fun CorrectnessWitnessesToXcfa(witness: YamlWitness): XCFA {
    // For invariant witnesses, process the invariants
    val mainBuilder = XcfaProcedureBuilder("main", ProcedurePassManager())
    mainBuilder.createInitLoc()
    mainBuilder.createFinalLoc()
        
        // Track locations for each invariant
        val invariantLocations = mutableMapOf<Triple<String, Int, Int?>, XcfaLocation>()
        
        // Process each invariant
        witness.content.forEach { contentItem ->
            contentItem.invariant?.let { invariant ->
                // Create a location key based on the invariant location
                val locKey = Triple(invariant.location.fileName, invariant.location.line, invariant.location.column)
                
                // Get or create a location for this invariant
                val invLoc = invariantLocations.getOrPut(locKey) {
                    val funcName = invariant.location.function ?: "unknown"
                    XcfaLocation("${funcName}_L${invariant.location.line}_${invariant.location.column ?: 0}")
                }
                
                // Add the invariant as a self-loop with a comment
                val actions = listOf(CommentAction("Invariant: ${invariant.value}"))
                
                when (invariant.type) {
                    InvariantType.LOOP_INVARIANT -> {
                        // For loop invariants, add a self loop
                        mainBuilder.addEdge(XcfaEdge(invLoc, invLoc, actions))
                    }
                    InvariantType.LOCATION_INVARIANT -> {
                        // For location invariants, add a self loop
                        mainBuilder.addEdge(XcfaEdge(invLoc, invLoc, actions))
                    }
                }
                
                // Ensure there's a path from init to this location and from this location to final
                mainBuilder.addEdge(XcfaEdge(mainBuilder.initLoc, invLoc, listOf()))
                mainBuilder.addEdge(XcfaEdge(invLoc, mainBuilder.finalLoc.get(), listOf(ReturnAction(Int(0)))))
            }
        }
        
        // If no invariants were processed, add a direct path from init to final
        if (invariantLocations.isEmpty()) {
            mainBuilder.addEdge(XcfaEdge(mainBuilder.initLoc, mainBuilder.finalLoc.get(), listOf(ReturnAction(Int(0)))))
        }
        
        // Add the main procedure to the XCFA
        xcfaBuilder.addProcedure(mainBuilder)
        xcfaBuilder.addEntryPoint(mainBuilder, emptyList())
    }
    
    return xcfaBuilder.build()
}


