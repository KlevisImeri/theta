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

import java.io.File

object CachingFileSerializer {

    private val cache: MutableMap<Pair<String, Any>, File> = LinkedHashMap()
    private val shmDir = File("/dev/shm")

    /**
     * key: unique id for serialization groups obj: object to serialize func: generator function if a
     * cache miss occurs
     */
    fun serialize(key: String, obj: Any, func: (Any) -> String): File =
        cache.getOrPut(Pair(key, obj)) {
            val content = func(obj)
            val pattern = key.split(".")
            val tempDir = if (shmDir.exists() && shmDir.isDirectory && shmDir.canWrite()) { //TODO: do not check everytime
                shmDir
            } else {
                File(System.getProperty("java.io.tmpdir"))
            }
            // val tempDir = File(System.getProperty("java.io.tmpdir")) 
            val file = File.createTempFile(
                pattern.subList(0, pattern.size - 1).joinToString("."),
                ".${pattern.last()}",
                tempDir
            )
            file.deleteOnExit()
            file.writeText(content)
            file
        }

    fun clear() {
        cache.values.forEach { it.delete() }
        cache.clear()
    }
}
