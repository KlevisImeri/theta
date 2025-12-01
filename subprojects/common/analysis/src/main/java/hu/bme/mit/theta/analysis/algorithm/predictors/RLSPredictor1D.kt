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
package hu.bme.mit.theta.analysis.algorithm.predictors

class RlsPredictor1D(
  private val forgettingFactor: Double = 96.0,
  private val initialWeight: Double = 1.0 
) {
    /**
     * A guide to tuning the `forgettingFactor` (λ). This value controls the memory
     * of the RLS filter, balancing stability against responsiveness. It must be
     * between 0.0 and 1.0. A common starting point is between 0.95 and 0.99.
     *
     * | `forgettingFactor` (λ) | Memory | Stability | Responsiveness | Best For                                    |
     * |:-----------------------|:-------|:----------|:---------------|:--------------------------------------------|
     * | High (e.g., 0.99)      | Long   | High      | Low            | Stable, slowly changing, or noisy systems   |
     * | Low (e.g., 0.90)       | Short  | Low       | High           | Dynamic systems that change behavior quickly|
     */
    var weight: Double = initialWeight
        private set
    var prevWeight: Double = weight
        private set
    private var covariance: Double = 1000.0 // High initial uncertainty
    private var prevCovariance: Double = covariance 

    fun predict(feature: Double): Double = weight * feature

    fun update(feature: Double, actualValue: Double) {
        val error = actualValue - predict(feature)
        val denominator = forgettingFactor + feature * feature * covariance

        if (denominator == 0.0) return

        val gain = (covariance * feature) / denominator

        prevWeight = weight
        prevCovariance = covariance

        weight += gain * error
        covariance = (covariance - gain * feature * covariance) / forgettingFactor
    }

    fun undoOnce() {
        weight = prevWeight 
        covariance = prevCovariance
    }
}
