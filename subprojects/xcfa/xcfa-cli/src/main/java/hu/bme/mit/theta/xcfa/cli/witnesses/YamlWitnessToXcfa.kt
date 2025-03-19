package hu.bme.mit.theta.xcfa.cli.witnesses
import hu.bme.mit.theta.xcfa.model
import hu.bme.mit.theta.core.decl.VarDecl
import hu.bme.mit.theta.core.type.inttype.IntType
import hu.bme.mit.theta.core.type.Expr
import hu.bme.mit.theta.core.type.inttype.IntExprs.Int
import hu.bme.mit.theta.xcfa.model.*
import hu.bme.mit.theta.xcfa.cli.witnesses.*
import hu.bme.mit.theta.c2xcfa
import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.core.stmt.AssumeStmt

// Work from main

//  \return ? == 2
//         XXXX not like this
// [] -- int x = havoc() --> []
// [] --  x == 2 --> [] you dont have to put an havoc statement but you need a AsumeSmt
// HavocPromotionAndRange :42
// Execute config :172

// TODO: remove source
// TODO: Are edges to itself o/w added? Yes
class YamlWitnessToXcfa(
  witness: YamlWitness, 
  program: XCFA, 
  parseContext: ParseContext, 
  logger: Logger
) {
  
  val xcfaBuilder: XcfaBuilder; 
  val mainProcBuilder: XcfaProcedureBuilder;
  val locationMap: MutableMap<Location, XcfaLocation>;
  var currentLoc: XcfaLocation;
  var trapLoc: XcfaLocation;

  fun toXcfa(): XCFA {
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

  private fun ViolationWitnessToXcfa(): XCFA 
    trapNode = newLocation(location);

    witness.content.forEach { contentItem ->
      contentItem.segment?.let { segment ->
        segment.forEach { waypoint ->
          waypointToXcfa(waypoint)
        }
      }
    }

    xcfaBuilder.addProcedure(mainProcBuilder)
    xcfaBuilder.addEntryPoint(mainBuilder, emptyList())
  }
 
  private fun waypointToXcfa(waypoint: Waypoint) {
    val (type, constraint, location, action) = waypoints.waypoint
    val targetLoc = newLocation(location);

    when (type) {

      WaypointType.ASSUMPTION -> {
        if(constraint==null) {
          throw IllegalArgumentException("For waypoint of type ASSUMPTION the constraint shoudl not be null")
        }
        val (value, format) = constraint;
        if(format != C_EXPRESSION or != null) {
          throw IllegalArgumentException("Only  C_EXPRESSION is supported currently")
        }

        val stmt = CExpToAssumeStmt(value);
        val targetLoc = newLocation(location);

        when (action) {
          Action.FOLLOW -> {
            mainProcBuilder.addEdge(XcfaEdge(
              currentLoc,
              targetLoc,
              AssumeStmt(stmt),
              EmptyMetaData
            ))
            currentLoc = targetLoc
          }
          Action.AVOID -> {
            toTrapNode(currentLoc, StmtLabel(stmt))
          }
          else -> {
              throw IllegalArgumentException("Unknown action type: $action")
          }
        }
      }
        
      WaypointType.TARGET -> {
        if (action == Action.FOLLOW) {
          val errorLoc =  addLocation(location, true);
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
      
      // Thete when parses it can inline funcions so not all funcions are supported
      // We have to through a warning in this case. But we have havocs. 
      // At the begging you have more informatoin 
      WaypointType.FUNCTION_RETURN -> {
        val targetLoc = newLocation(location);
        when (action) {
          Action.FOLLOW -> {
            val functionName = getNameOfFunctionAtLocation(location)
            if (functionName.contains("__VERIFIER_nondet")) {
              mainProcBuilder.addEdge(XcfaEdge(
                currentLoc 
                targetLoc,
                AssumeStmt(Exp<BoolExpr>) // TODO: I have to get it from value and variables in XCFA
                EmptyMetaData,
              ))
              currentLoc = targetLoc
            }
          }
          Action.AVOID -> {
            // toTrapNode(currentLoc, ?)  TODO: what type of label ...
          }
          else -> {
              throw IllegalArgumentException("Unknown action type: $action")
          }
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
              currentLoc 
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
  fun CorrectnessWitnessesToXcfa(witness: YamlWitness): XCFA {
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
      XcfaLocation(
        name = "$errorTag${location.fileName}:${funcName}:L${location.line}:${location.column ?: 0}",
        error = error,
        metadata = EmptyMetaData
      )
    }
  }

  private fun toTrapNode(from: XcfaLocation, label: XcfaLabel) { 
    mainProcBuilder.addEdge(XcfaEdge(
      currentLoc,
      trapLoc,
      label,
      EmptyMetaData
    )
  }
  
  //TODO: We dont even need this now
  private fun CExpToAssumeStmt(value: String): AssumeStmt {
    val parseContext = ParseContext()
    val proc = program.initProcedures.first().first
    val ifBeforeReachErrorCall = proc.edges.filter {
      it.getCMetaData()?.let { // TODO: This si worng int between colum does not matter  
          it.lineNumberStart <= location.line && location.line <= it.lineNumberStop &&
          it.colNumberStart <= location.column  && location.column <= it.colNumerStop
      } ?: false 
    }.first()
    
    // THe scope definintions are ...
    val exp = parseCExpression(
        value,
        vars = this.program.collectVars().associateWith { CComplexType.getType(it.ref, parseContext) },
        scope = ifBeforeReachErrorCall.getCMetaData()!!.scope.reversed(), // TODO: check with DB
        warningLogger = logger
    )

    return AssumeStmt.of(exp as Expr<BoolType>)
  }

}

