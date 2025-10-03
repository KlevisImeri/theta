/*
 *  Copyright 2025 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may not use this file except in compliance with the License.
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
package hu.bme.mit.theta.analysis.algorithm.predictors

import kotlin.math.exp
import kotlin.math.log

class ExpRLSPredictor(forgettingFactor: Double) {

    private val linearPredictor = RlsPredictor1D(forgettingFactor)

    val weight: Double
        get() = linearPredictor.weight
    
    var predictTidPoint();

    fun predict(feature: Double): Double {
        val logFeature = log(feature, E)
        val logPrediction = linearPredictor.predict(logFeature)
        return exp(logPrediction)
    }

    fun update(feature: Double, actualValue: Double) {
        val logFeature = log(feature, E)
        val logActualValue = log(actualValue, E)
        linearPredictor.update(logFeature, logActualValue)
    }


    private companion object {
        private const val E = Math.E
    }
}
