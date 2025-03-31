package hu.bme.mit.theta.xcfa.cli.witnesses
import hu.bme.mit.theta.core.type.inttype.IntType
import hu.bme.mit.theta.core.type.Expr
import hu.bme.mit.theta.core.type.inttype.IntExprs.Int
import hu.bme.mit.theta.xcfa.model.*
import hu.bme.mit.theta.xcfa.cli.witnesses.*
import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.core.stmt.AssumeStmt
import hu.bme.mit.theta.xcfa.passes.HavocPromotionAndRange
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.common.logging.Logger.Level.*
import hu.bme.mit.theta.xcfa.passes.ProcedurePassManager


// Work from main

//  \return ? == 2
//         XXXX not like this
// [] -- int x = havoc() --> []
// [] --  x == 2 --> [] you dont have to put an havoc statement but you need a AsumeSmt
// HavocPromotionAndRange :42

// TODO: remove source
class YamlWitnessToXcfa(
  val witness: YamlWitness, 
  val program: XCFA, 
  val parseContext: ParseContext, 
  val logger: Logger
) {

  lateinit var xcfaBuilder: XcfaBuilder; 
  lateinit var mainProcBuilder: XcfaProcedureBuilder;
  lateinit var locationMap: MutableMap<Location, XcfaLocation>;
  lateinit var currentLoc: XcfaLocation;
  lateinit var trapLoc: XcfaLocation;

  fun run(): XCFA {
    System.out.println(program.toString());
    xcfaBuilder = XcfaBuilder("WitnessModel_${witness.metadata.uuid.take(5)}")
    locationMap = mutableMapOf<Location, XcfaLocation>()
    mainProcBuilder = XcfaProcedureBuilder("main", ProcedurePassManager()) 
    mainProcBuilder.createInitLoc()
    // mainProcBuilder.createFinalLoc() TODO: Only for Correctnes
    currentLoc = mainProcBuilder.initLoc
    return when (witness.entryType) {
      EntryType.VIOLATION -> violationWitnessToXcfa()
      EntryType.INVARIANTS -> correctnessWitnessesToXcfa()
    }
  }

  private fun violationWitnessToXcfa(): XCFA {
    trapLoc = newLocation(Location("Trap", -1));

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
        val targetLoc = newLocation(location);

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
            toTrapNode(currentLoc, stmtLabel)
          }
          else -> {
              throw IllegalArgumentException("Unknown action type: $action")
          }
        }
      }
        
      WaypointType.TARGET -> {
        if (action == Action.FOLLOW) {
          val errorLoc = newLocation(location, true);
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

      WaypointType.FUNCTION_ENTER -> {} //ignore
      
      WaypointType.FUNCTION_RETURN -> {
        val targetLoc = newLocation(location);
        val (assumeStmt, successful) = getAssumeStmtAtLocation(location)
        if(successful) {
          when (action) {
            Action.FOLLOW -> {
              mainProcBuilder.addEdge(XcfaEdge(
                currentLoc,
                targetLoc,
                assumeStmt,
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
        } else {
          throw IllegalArgumentException("We only support some function for their return!")
        }
      }

      WaypointType.BRANCHING -> {
        val targetLoc = newLocation(location);
        val label = StmtLabel(
          SkipStmt(),
          if (constraint.value) ChoiceType.MAIN_PATH else ChoiceType.ALTERNATIVE_PATH
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
            toTrapNode(currentLoc, label)
          }
          else -> {
              throw IllegalArgumentException("Unknown action type: $action")
          }
        }
      }

    }
  }

  // Idea is that you create a label where you have the exact location and the statement.
  // This is the same for all types of statemetns.
  private fun correctnessWitnessesToXcfa(): XCFA {
    witness.content.forEach { contentItem ->
      val (type, location, value, format) = contentItem.
      if(format != C_EXPRESSION) {
        throw IllegalArgumentException("Only  C_EXPRESSION is supported currently")
      }
      val targetLoc = newLocation(location);
      val assumeStmt = CExpToAssumeStmt(value);

    }

    xcfaBuilder.addProcedure(mainProcBuilder)
    xcfaBuilder.addEntryPoint(mainBuilder, emptyList())
  }


  private fun newLocation(location: Location, error: Boolean = false): XcfaLocation {
    return locationMap.getOrPut(location) {
      val funcName = location.function ?: "unknown"
      val errorTag = if (error) "[Error]" else ""
      val newXcfaLocation = XcfaLocation(
        // name = "$errorTag${location.fileName}:${funcName}:L${location.line}:${location.column ?: 0}",
        name = "",
        error = error,
        metadata = EmptyMetaData
      )
      mainProcBuilder.addEdge(XcfaEdge(
        newXcfaLocation,
        newXcfaLocation,
        label,
        EmptyMetaData
      ))
      return newXcfaLocation;
    }
  }

  private fun toTrapNode(from: XcfaLocation, label: XcfaLabel) { 
    mainProcBuilder.addEdge(XcfaEdge(
      currentLoc,
      trapLoc,
      label,
      EmptyMetaData
    ))
  }
  
  private fun getAssumeStmtAtLocation(loc: Location): Pair<AssumeStmt, Boolean> {
      return Pair(AssumeStmt.of(BoolExprs.True()), true)
  }

  private fun CExpToAssumeStmt(value: String): AssumeStmt {
    return AssumeStmt.of(BoolExprs.True());
  }


  // //TODO: We dont even need this now
  // private fun CExpToAssumeStmt(value: String): AssumeStmt {
  //   val parseContext = ParseContext()
  //   val proc = program.initProcedures.first().first
  //   val ifBeforeReachErrorCall = proc.edges.filter {
  //     it.getCMetaData()?.let { // TODO: This si worng int between colum does not matter  
  //         it.lineNumberStart <= location.line && location.line <= it.lineNumberStop &&
  //         it.colNumberStart <= location.column  && location.column <= it.colNumerStop
  //     } ?: false 
  //   }.first()
  //   
  //   // THe scope definintions are ...
  //   val exp = parseCExpression(
  //       value,
  //       vars = this.program.collectVars().associateWith { CComplexType.getType(it.ref, parseContext) },
  //       scope = ifBeforeReachErrorCall.getCMetaData()!!.scope.reversed(), // TODO: check with DB
  //       warningLogger = logger
  //   )
  //
  //   return AssumeStmt.of(exp as Expr<BoolType>)
  // }

}

