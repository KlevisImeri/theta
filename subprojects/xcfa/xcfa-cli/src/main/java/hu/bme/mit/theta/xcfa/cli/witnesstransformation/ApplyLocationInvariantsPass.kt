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
import hu.bme.mit.theta.xcfa.model.*
import hu.bme.mit.theta.analysis.expr.ExprState;
import hu.bme.mit.theta.core.type.*;
import hu.bme.mit.theta.core.type.booltype.*;



class ApplyLocationInvariantsPass(parseContext: ParseContext, val witness: LocationInvariants) :
  ProcedurePass {
    override fun run(builder: XcfaProcedureBuilder): XcfaProcedureBuilder {
        val invariantMap: Map<XcfaLocation, Collection<ExprState>> = witness.getPartitions()
        var partialCnt = 0
        
        val edgesToAdd = mutableSetOf<XcfaEdge>()
        val edgesToRemove = mutableSetOf<XcfaEdge>()
        val locationsToAdd = mutableSetOf<XcfaLocation>()
        
        for (loc in builder.getLocs()) {
            if (!invariantMap.containsKey(loc)) continue
            val invariants = invariantMap[loc]!!
            if (invariants.isEmpty()) continue

            val exprs: List<Expr<BoolType>> = invariants.map { expr -> expr.toExpr() }//.filterNot { it is TrueExpr }
            // val stmtXcfaLabel = StmtLabel(Stmts.Assume(OrExpr.of(exprs)));
            val stmtXcfaLabel = StmtLabel(Stmts.Assume(exprs.last()));
            // val stmtXcfaLabel = StmtLabel(Stmts.Assume(AndExpr.of(exprs)));
            
            val newLoc = XcfaLocation(name = "partial$partialCnt", metadata = EmptyMetaData)
            partialCnt++
            
            val newEdge = XcfaEdge(
                source = newLoc, 
                target = loc,
                label = SequenceLabel(listOf(stmtXcfaLabel)),
                EmptyMetaData
            )
            
            for (incomingEdge in loc.incomingEdges) {
                val redirectedEdge = incomingEdge.withTarget(newLoc)
                edgesToRemove.add(incomingEdge)
                edgesToAdd.add(redirectedEdge)
            }
            
            edgesToAdd.add(newEdge)
            locationsToAdd.add(newLoc)
        }
        
        edgesToRemove.forEach { builder.removeEdge(it) }
        edgesToAdd.forEach { builder.addEdge(it) }
        locationsToAdd.forEach { builder.addLoc(it) }
        
        return builder
    }
}
