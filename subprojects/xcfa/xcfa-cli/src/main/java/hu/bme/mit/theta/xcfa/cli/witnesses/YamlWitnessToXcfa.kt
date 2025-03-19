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
import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.core.stmt.AssumeStmt

class YamlWitnessToXcfa(witness: YamlWitness, program: XCFA, source: String, logger: Logger) {
  
  val xcfaBuilder: XcfaBuilder; 
  val mainProcBuilder: XcfaProcedureBuilder;
  val locationMap: MutableMap<Location, XcfaLocation> = mutableMapOf()
  var currentLoc: XcfaLocation;
  var trapLoc: XcfaLocation;

  fun toXcfa(): XCFA {
    xcfaBuilder = XcfaBuilder("WitnessModel_${witness.metadata.uuid.take(5)}")
    locationMap = mutableMapOf<Location, XcfaLocation>()
    mainProcBuilder = XcfaProcedureBuilder("main", ProcedurePassManager()) 
    mainProcBuilder.createInitLoc()
    // mainProcBuilder.createFinalLoc() TODO: Do we really need a final locaition for procedure?
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
              exp,
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

      WaypointType.FUNCTION_ENTER -> {} //ignore TODO: What about avoid fucntion enter?

      WaypointType.FUNCTION_RETURN -> {
        val targetLoc = newLocation(location);
        when (action) {
          Action.FOLLOW -> {
            val functionName = getNameOfFunctionAtLocation(location)
            if (functionName.contains("__VERIFIER_nondet")) {
              mainProcBuilder.addEdge(XcfaEdge(
                currentLoc 
                targetLoc,
                HavocStmt.of(getVarDeclAtLocation(Location)); // TODO: is this the right way to creat havoc
                EmptyMetaData,
              ))
              currentLoc = targetLoc
            }
          }
          Action.AVOID -> {
            // toTrapNode(currentLoc, ?)  TODO: what type of label
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


fun CorrectnessWitnessesToXcfa(witness: YamlWitness): XCFA {

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
  
  // TODO: Understand thsi fucntion exacly
  private fun CExpToAssumeStmt(value: String): AssumeStmt {
    val parseContext = ParseContext() //TODO: Understand what does this do and how is it used below?
    val proc = program.initProcedures.first().first
    val ifBeforeReachErrorCall = proc.edges.filter {
      it.getCMetaData()?.let { 
          it.lineNumberStart == location.line && it.colNumberStart == location.column 
      } ?: false 
    }.first()

    val exp = parseCExpression(
        value,
        vars = this.program.collectVars().associateWith { CComplexType.getType(it.ref, parseContext) },
        scope = ifBeforeReachErrorCall.getCMetaData()!!.scope.reversed(),
        warningLogger = logger
    )

    return AssumeStmt.of(exp as Expr<BoolType>) //TODO: can you do it like this?
  }
  
  private getVarDeclAtLocation(location: Location) {
    // TODO: Use the XCFA.initProcedureBuildrs.first().vars ?
    // Do something similar to parseCExpression exmaple
  }

  // TODO: Problamatic if not in the same line
  private fun getNameOfFunctionAtLocation(location: Location): String {
    val lines = try {
      File(source).takeIf { it.exists() }?.readLines() 
          ?: source.split("\n")
    } catch (e: Exception) {
      logger.error("Error reading source: ${e.message}")
      throw IllegalArgumentException("Yaml witness location is not appropiate for the input file!")
    }

    var (line, column, function) = location;
    line--
    column-- 

    if (lineNumber !in lines.indices) throw IllegalArgumentException("Yaml witness location is not appropiate for the input file!")
    if (columnNumber !in line.indices) throw IllegalArgumentException("Yaml witness location is not appropiate for the input file!")
    if (lines[line][column] != ')') throw IllegalArgumentException("Yaml witness location is not appropiate for the input file!")

    var parenCount = 0;
    var pos = column; 
      while (pos >= 0 && parenCount > 0) {
        when (line[pos]) {
          ')' -> parenCount++
          '(' -> parenCount--
        }
        pos--
      }

    if (parenCount != 0) throw IllegalArgumentException("Yaml witness location is not appropiate for the input file!")

    var nameStart = pos
    while (nameStart >= 0 && line[nameStart].isWhitespace()) {
      nameStart--
    }

    val functionName = line.substring(nameStart + 1, pos).trim()
    return functionName.ifEmpty { "unknown" }
  }

}

