package hu.bme.mit.theta.xcfa.cli.witnesstransformation
import hu.bme.mit.theta.core.type.inttype.IntType
import hu.bme.mit.theta.core.type.Expr
import hu.bme.mit.theta.core.type.inttype.IntExprs.Int
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

// HavocPromotionAndRange :42

// TODO: do more tests
// TODO  implement priv funcs
// TODO: implement correctin witness

// TODO: remove source
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

  fun run(): XCFA {
    System.out.println(program.toString());
    xcfaBuilder = XcfaBuilder("WitnessModel_${witness.metadata.uuid.take(5)}")
    locationMap = mutableMapOf<Location, XcfaLocation>()
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

  private fun violationWitnessToXcfa(): XCFA {

    witness.content.forEach { contentItem ->
      contentItem.segment?.let { segment ->
        segment.forEach { waypoint ->
          waypointToXcfa(waypoint)
        }
      }
    }

    xcfaBuilder.addProcedure(mainProcBuilder)
    xcfaBuilder.addEntryPoint(mainProcBuilder, emptyList())
    return xcfaBuilder.build();
  }
 
  private fun waypointToXcfa(waypoint: Waypoint) {
    val (type, constraint, location, action) = waypoint.waypoint
    val targetLoc = newLocation(location);

    when (type) {

      WaypointType.ASSUMPTION -> {
        if(constraint==null) {
          throw IllegalArgumentException("For waypoint of type ASSUMPTION the constraint shoudl not be null")
        }
        val (value, format) = constraint;
        if(format != Format.C_EXPRESSION && format!= null) {
          throw IllegalArgumentException("Only  C_EXPRESSION is supported currently")
        }

        val stmtLabel = StmtLabel(CExpToAssumeStmt(value));

        when (action) {
          Action.FOLLOW -> {
            mainProcBuilder.addEdge(XcfaEdge(
              currentLoc,
              targetLoc,
              stmtLabel,
              EmptyMetaData
            ))
            currentLoc = targetLoc
          }
          Action.AVOID -> {
            toTrapNode(stmtLabel)
          }
          else -> {
              throw IllegalArgumentException("Unknown action type: $action")
          }
        }
      }

      WaypointType.TARGET -> {
        if (action == Action.FOLLOW) {
          mainProcBuilder.addEdge(XcfaEdge(
            currentLoc,
            errorLoc,
            NopLabel,
            EmptyMetaData
          ))
          currentLoc = errorLoc
        } else {
           throw IllegalArgumentException("For waypoint of type TARGET only action FOLLOW is allowed. Current action: ${action}")
        }
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
              label,
              EmptyMetaData,
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
      }

      WaypointType.BRANCHING -> {
        if(constraint==null) throw IllegalArgumentException("For $action the constraint can not be empty!") 
        val value = constraint.value;

        if(value=="true" || value=="false") {

          val label = StmtLabel(
            SkipStmt.getInstance(),
            if (value=="true") ChoiceType.MAIN_PATH else ChoiceType.ALTERNATIVE_PATH
          )
          when (action) {
            Action.FOLLOW -> {
             mainProcBuilder.addEdge(XcfaEdge(
                currentLoc,
                targetLoc,
                label,
                EmptyMetaData,
              ))
              currentLoc = targetLoc
            }
            Action.AVOID -> {
              toTrapNode(label)
            }
            else -> {
                throw IllegalArgumentException("Unknown action type: $action")
            }
          }

        } else { //switch statement
          throw TODO("How do you even label a switch?")

          // val label = StmtLabel(
          //   SkipStmt(),
          //   if (constraint.value=="true") ChoiceType.MAIN_PATH else ChoiceType.ALTERNATIVE_PATH
          // )
          // when (action) {
          //   Action.FOLLOW -> {
          //    mainProcBuilder.addEdge(XcfaEdge(
          //       currentLoc,
          //       targetLoc,
          //       label,
          //       EmptyMetaData,
          //     ))
          //     currentLoc = targetLoc
          //   }
          //   Action.AVOID -> {
          //     toTrapNode(label)
          //   }
          //   else -> {
          //       throw IllegalArgumentException("Unknown action type: $action")
          //   }

        }
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
  private fun correctnessWitnessesToXcfa(): XCFA {
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
    return xcfaBuilder.build();
  }

  // private fun findPrevLocation(xcfaLoc: XcfaLocation, loc: Location): XcfaLocation {
  //   val orderdArrayOfLocations = locationMap to array orderd by locatoin
  //   return binarySearchPrevLocation(OrderdArrayOfLcoations)
  // }

  private fun newLocation(location: Location): XcfaLocation {
    return locationMap.getOrPut(location) {
      val funcName = location.function ?: "unknown"
      val newXcfaLocation = XcfaLocation(
        name = "${location.fileName}:${funcName}:L${location.line}:${location.column ?: 0}",
        metadata = EmptyMetaData
      )
      mainProcBuilder.addEdge(XcfaEdge(
        newXcfaLocation,
        newXcfaLocation,
        StmtLabel(SkipStmt.getInstance()),
        EmptyMetaData
      ))
      return newXcfaLocation;
    }
  }

  private fun toTrapNode(label: XcfaLabel) { 
    mainProcBuilder.addEdge(XcfaEdge(
      currentLoc,
      trapLoc,
      label,
      EmptyMetaData
    ))
  }
  
  private fun getStmtLabelAtLocation(loc: Location): StmtLabel {
      return StmtLabel(AssumeStmt.of(BoolExprs.True()));
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

