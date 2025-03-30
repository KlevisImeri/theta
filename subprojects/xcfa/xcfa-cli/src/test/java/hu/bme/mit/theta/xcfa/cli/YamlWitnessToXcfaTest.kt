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
package hu.bme.mit.theta.xcfa.cli

import com.charleskorn.kaml.Yaml
import hu.bme.mit.theta.xcfa.cli.witnesses.*
import java.util.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class YamlWitnessToXcfaTest {
    companion object {
        @JvmStatic
        fun witnessExamples(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    "/witness/multivar_1-1.c",
                    "/witness/multivar_1-1.witness.yml"
                ),
                Arguments.of(
                    "/witness/safe-program-example.c",
                    "/witness/safe-program-example.witness.yml"
                ),
                Arguments.of(
                    "/witness/unsafe-program-example.c",
                    "/witness/unsafe-program-example.cpachecker.witness.yml"
                ),
                Arguments.of(
                    "/witness/unsafe-program-example.c",
                    "/witness/unsafe-program-example.symbiotic.witness.yml"
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("witnessExamples")
    fun testWitnessConversion(cFile: String, witnessFile: String) {
        val logger = NullLogger.getInstance()
        
        val (xcfa, mcm, parseContext, witnessXcfa) = frontend(
            XcfaConfig(
                inputConfig = InputConfig(
                    input = File(javaClass.getResource(cFile)!!.path)
                ),
                validateConfig = ValidateConfig(
                    enabled = true,
                    witness = File(javaClass.getResource(witnessFile)!!.path)
                )
            ),
            logger,
            logger
        )

        // TODO: add more assertoins
        Assertions.assertNotNull(witnessXcfa, "Witness XCFA should be generated")
        Assertions.assertTrue(xcfa.procedures.isNotEmpty(), "Main XCFA should have procedures")
    }
}
