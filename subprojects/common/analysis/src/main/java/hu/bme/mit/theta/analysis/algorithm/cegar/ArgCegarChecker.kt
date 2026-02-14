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

import hu.bme.mit.theta.analysis.Action
import hu.bme.mit.theta.analysis.Prec
import hu.bme.mit.theta.analysis.State
import hu.bme.mit.theta.analysis.Trace
import hu.bme.mit.theta.analysis.algorithm.arg.ARG
import hu.bme.mit.theta.analysis.utils.ArgVisualizer
import hu.bme.mit.theta.analysis.algorithm.cegar.CegarChecker.Companion.CegarParams

object ArgCegarChecker {

    @JvmStatic
    @JvmOverloads
    fun <S : State, A : Action, P : Prec> create(
        abstractor: ArgAbstractor<S, A, P>,
        refiner: ArgRefiner<S, A, P>,
        cegarParams: CegarParams = CegarParams()
    ): CegarChecker<P, ARG<S, A>, Trace<S, A>> {
        return CegarChecker.create(
            abstractor = abstractor,
            refiner = refiner,
            proofVisualizer = ArgVisualizer.getDefault(),
            cegarParams = cegarParams
        )
    }


    @Deprecated(
        message = "Use the version that takes a CegarParams object.",
        replaceWith = ReplaceWith("create(abstractor, refiner, cegarParams = CegarParams(computePartialResult = computePartialResult))")
    )
    fun <S : State, A : Action, P : Prec> create(
        abstractor: ArgAbstractor<S, A, P>,
        refiner: ArgRefiner<S, A, P>,
        computePartialResult: Boolean
    ): CegarChecker<P, ARG<S, A>, Trace<S, A>> {
        val config = CegarParams(computePartialResult = computePartialResult)
        return create(abstractor, refiner, cegarParams = config)
    }

    @Deprecated(
        message = "Use the version that takes a CegarParams object.",
        replaceWith = ReplaceWith("create(abstractor, refiner, cegarParams = CegarParams(computePartialResult = computePartialResult, softTimeoutMs = timeoutMs))")
    )
    fun <S : State, A : Action, P : Prec> create(
        abstractor: ArgAbstractor<S, A, P>,
        refiner: ArgRefiner<S, A, P>,
        computePartialResult: Boolean,
        timeoutMs: Long
    ): CegarChecker<P, ARG<S, A>, Trace<S, A>> {
        val config = CegarParams(computePartialResult = computePartialResult, softTimeoutMs = timeoutMs)
        return create(abstractor, refiner, cegarParams = config)
    }

    @Deprecated(
        message = "Use the version that takes a CegarParams object.",
        replaceWith = ReplaceWith("create(abstractor, refiner, cegarParams = CegarParams(computePartialResult = computePartialResult, softTimeoutMs = timeoutMs, afterTimeOut = { interruptSolvers.run() }))")
    )
    fun <S : State, A : Action, P : Prec> create(
        abstractor: ArgAbstractor<S, A, P>,
        refiner: ArgRefiner<S, A, P>,
        computePartialResult: Boolean,
        timeoutMs: Long,
        interruptSolvers: Runnable
    ): CegarChecker<P, ARG<S, A>, Trace<S, A>> {
        val config = CegarParams(
            computePartialResult = computePartialResult,
            softTimeoutMs = timeoutMs,
            afterTimeOut = { interruptSolvers.run() }
        )
        return create(abstractor, refiner, config)
    }
}
