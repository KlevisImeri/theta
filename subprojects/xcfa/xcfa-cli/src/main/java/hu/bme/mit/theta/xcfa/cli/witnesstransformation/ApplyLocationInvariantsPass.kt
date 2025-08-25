/*
 *  Copyright 2025 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package hu.bme.mit.theta.xcfa.cli.witnesstransformation

import hu.bme.mit.theta.core.stmt.Stmts
import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.xcfa.cli.utils.LocationInvariants
import hu.bme.mit.theta.xcfa.model.SequenceLabel
import hu.bme.mit.theta.xcfa.model.StmtLabel
import hu.bme.mit.theta.xcfa.model.XcfaProcedureBuilder
import hu.bme.mit.theta.xcfa.passes.ProcedurePass
import hu.bme.mit.theta.xcfa.witnesses.*

class ApplyLocationInvariantsPass(parseContext: ParseContext, val witness: LocationInvariants) :
  ProcedurePass {
  override fun run(builder: XcfaProcedureBuilder): XcfaProcedureBuilder {
    val invariantMap = witness.getPartitions()

    for (loc in builder.getLocs()) {
      if (!invariantMap.containsKey(loc)) continue
      val invariants = invariantMap[loc]!!
      if (invariants.isEmpty()) continue

      val stmtXcfaLabels = invariants.map { expr -> StmtLabel(Stmts.Assume(expr.toExpr())) }

      for (edge in loc.incomingEdges.toList()) {
        val currentLabels = (edge.label as? SequenceLabel)?.labels ?: listOf(edge.label)
        val newEdge = edge.withLabel(SequenceLabel(currentLabels + stmtXcfaLabels))
        builder.removeEdge(edge)
        builder.addEdge(newEdge)
      }
    }

    return builder
  }
}
