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
import hu.bme.mit.theta.xcfa.witnesses.*
import java.util.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import hu.bme.mit.theta.common.logging.*
import hu.bme.mit.theta.xcfa.cli.params.*
import java.io.File
import hu.bme.mit.theta.frontend.transformation.ArchitectureConfig;
import hu.bme.mit.theta.xcfa.model.*

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
    fun testWitnessConversion(cFile: String, witnessFile: String) { //TODO : finish erro chekcing also the outptu of XCFA is not good
        val logger = ConsoleLabelledLogger()

        val (xcfa, mcm, parseContext, witnessXcfa) = frontend(
            XcfaConfig(
                inputConfig = InputConfig(
                    input = File(javaClass.getResource(cFile)!!.path)
                ),
                validateConfig = ValidateConfig(
                  enabled = true,
                  witness = File(javaClass.getResource(witnessFile)!!.path)
                ),
                debugConfig = DebugConfig(
                  debug = true,
                  stacktrace = true,
                  logLevel = Logger.Level.VERBOSE,
                ),
                //--- fill up uninmportant ---//
                frontendConfig = FrontendConfig(
                  specConfig = CFrontendConfig(arithmetic = ArchitectureConfig.ArithmeticType.efficient),
                ),
                backendConfig = BackendConfig(
                  backend = Backend.BOUNDED,
                ),
                outputConfig = OutputConfig(),
                //--- fill up uninmportant ---//
            ),
            logger,
            logger
        )

        // println("------------------------------------")
        // println("The Program XCFA is:")
        // println("------------------------------------")
        // println(xcfa.toDot())
        // println(xcfa.toStringFormatted())
        // println("\n------------------------------------")
        // println("The Witness XCFA is:")
        // println("------------------------------------")
        // println(witnessXcfa!!.toDot())
        // println("------------------------------------")

        Assertions.assertNotNull(witnessXcfa, "Witness XCFA should be generated")
        Assertions.assertTrue(xcfa.procedures.isNotEmpty(), "Main XCFA should have procedures")

    }

}
