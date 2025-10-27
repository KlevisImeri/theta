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

import hu.bme.mit.theta.frontend.ParseContext
import hu.bme.mit.theta.xcfa.cli.utils.LocationInvariants
import hu.bme.mit.theta.xcfa.passes.*
import hu.bme.mit.theta.common.logging.Logger

class ApplyLocationInvariantsPassManager(parseContext: ParseContext, witness: LocationInvariants) :
  ProcedurePassManager(
    listOf(
      ApplyLocationInvariantsPass(parseContext, witness),
      NormalizePass(),
      // EliminateSelfLoops(),
      DeterministicPass(),
      // LbePass(parseContext)
      // EmptyEdgeRemovalPass(),
    )
    // listOf(
    //   // formatting
    //   NormalizePass(),
    //   DeterministicPass(),
    //   // removing redundant elements
    //   EmptyEdgeRemovalPass(),
    //   UnusedLocRemovalPass(),
    //   // handling intrinsics
    // ),
    // listOf(ReferenceElimination(parseContext), MallocFunctionPass(parseContext)),
    // listOf(
    //   // optimizing
    //   SimplifyExprsPass(parseContext),
    //   LoopUnrollPass(),
    //   SimplifyExprsPass(parseContext),
    //   EmptyEdgeRemovalPass(),
    //   UnusedLocRemovalPass(),
    // ),
    // listOf(StaticCoiPass()),
    // listOf(
    //   // handling remaining function calls
    //   MemsafetyPass(parseContext),
    //   NoSideEffectPass(parseContext),
    //   NondetFunctionPass(),
    //   // LbePass(parseContext),
    //   NormalizePass(), // needed after lbe, TODO
    //   DeterministicPass(), // needed after lbe, TODO
    //   HavocPromotionAndRange(parseContext),
    //   // Final cleanup
    //   // UnusedVarPass(uniqueWarningLogger),
    //   EmptyEdgeRemovalPass(),
    //   UnusedLocRemovalPass(),
    // ),
  )
