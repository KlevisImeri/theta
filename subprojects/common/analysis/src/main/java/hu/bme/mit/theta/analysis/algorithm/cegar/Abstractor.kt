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
package hu.bme.mit.theta.analysis.algorithm.cegar

import hu.bme.mit.theta.analysis.Prec
import hu.bme.mit.theta.analysis.algorithm.Proof

/**
 * Common interface for the abstractor component. It can create an initial witness
 * and check a witness with a given precision.
 */
interface Abstractor<P : Prec, Pr : Proof> {

    /** Create initial witness */
    fun createProof(): Pr

    /** Check witness with given precision and injection */
    fun check(witness: Pr, prec: P): AbstractorResult;

    /** Check witness with given precision and injection */
    fun check(witness: Pr, prec: P, inject: () -> Unit): AbstractorResult {
      return check(witness, prec)
    }

    /** Explores the whole witness */
    fun unroll(witness: Pr, prec: P): AbstractorResult;
}
