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
package hu.bme.mit.theta.xcfa.gson

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import hu.bme.mit.theta.analysis.expr.ExprState
import hu.bme.mit.theta.xcfa.cli.utils.LocationInvariants
import hu.bme.mit.theta.xcfa.model.XcfaLocation

class LocationInvariantsAdapter(private val gsonSupplier: () -> Gson) :
  TypeAdapter<LocationInvariants>() {

  private val gson by lazy { gsonSupplier() }

  override fun write(writer: JsonWriter, value: LocationInvariants?) {
    if (value == null) {
      writer.nullValue()
      return
    }

    writer.beginArray()
    value.getPartitions().forEach { (location, states) ->
      writer.beginObject()
      writer.name("location")
      gson.toJson(location, XcfaLocation::class.java, writer)
      writer.name("invariants")
      gson.toJson(states, object : TypeToken<Collection<ExprState>>() {}.type, writer)
      writer.endObject()
    }
    writer.endArray()
  }

  override fun read(reader: JsonReader): LocationInvariants {
    if (reader.peek() == JsonToken.NULL) {
      reader.nextNull()
      return LocationInvariants(emptyMap())
    }

    val invariantsMap = mutableMapOf<XcfaLocation, Collection<ExprState>>()
    reader.beginArray()
    while (reader.hasNext()) {
      reader.beginObject()
      lateinit var location: XcfaLocation
      lateinit var states: Collection<ExprState>

      while (reader.hasNext()) {
        when (reader.nextName()) {
          "location" -> location = gson.fromJson(reader, XcfaLocation::class.java)
          "invariants" ->
            states = gson.fromJson(reader, object : TypeToken<Collection<ExprState>>() {}.type)
          else -> reader.skipValue()
        }
      }
      invariantsMap[location] = states
      reader.endObject()
    }
    reader.endArray()
    return LocationInvariants(invariantsMap)
  }
}
