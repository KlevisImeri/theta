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
package hu.bme.mit.theta.xcfa.cli.utils

import com.google.gson.Gson
import hu.bme.mit.theta.analysis.algorithm.PartitionedInvariantProof
import hu.bme.mit.theta.analysis.expr.ExprState
import hu.bme.mit.theta.common.logging.Logger
import hu.bme.mit.theta.common.logging.Logger.Level.*
import hu.bme.mit.theta.xcfa.model.XcfaLocation
import java.io.File

data class LocationInvariants(
  private val locationInvariants: Map<XcfaLocation, Collection<ExprState>>
) : PartitionedInvariantProof<XcfaLocation> {

  constructor() : this(emptyMap())

  override fun getPartitions(): Map<XcfaLocation, Collection<ExprState>> = locationInvariants

  override fun toString(): String {
    val formattedInvariants =
      getPartitions()
        .filter { (_, states) -> states.isNotEmpty() }
        .entries
        // .sortedBy { it.key.name }
        .joinToString(separator = " ") { entry ->
          val invariantsString = entry.value.joinToString(separator = "")
          "\n  ${entry.key.name}[$invariantsString]"
        }

    return "${this::class.simpleName?.substringBefore('@')?.substringAfterLast('.')}($formattedInvariants\n)"
  }

  fun merge(other: LocationInvariants): LocationInvariants {
    val mergedMap = this.locationInvariants + other.locationInvariants
    return LocationInvariants(mergedMap)
  }

  fun toJsonFile(file: File, gson: Gson, logger: Logger) {
    try {
      val jsonString = gson.toJson(this)
      file.writeText(jsonString)
      logger.write(INFO, "Successfully wrote LocationInvariants to ${file.name}")
    } catch (e: Exception) {
      logger.write(
        INFO,
        "[Error] Could not write LocationInvariants to file '${file.name}'. Reason: ${e.message}",
      )
    }
  }

  companion object {
    fun fromFile(file: File, gson: Gson, logger: Logger): LocationInvariants? {
      if (!file.exists() || !file.isFile) {
        return null
      }

      return try {
        val invariants =
          file.reader().use { fileReader ->
            gson.fromJson(fileReader, LocationInvariants::class.java)
          }
        logger.write(INFO, "Successfully loaded LocationInvariants from ${file.name}\n")
        invariants
      } catch (e: Exception) {
        logger.write(
          INFO,
          "[Error] Could not parse LocationInvariants file '${file.name}'. Reason: ${e.message}\n",
        )
        null
      }
    }
  }
}
