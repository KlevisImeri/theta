/*logger.
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
package hu.bme.mit.theta.common.logging

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.Writer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Logger {
    private val logTypes = mapOf(
        "debug" to "DEBUG",
        "info" to "INFO",
        "warning" to "WARN",
        "warn" to "WARN",
        "error" to "ERROR",
        "result" to "RESULT",
        "benchmark" to "BENCHMARK",
        "mainstep" to "MAINSTEP",
        "substep" to "SUBSTEP",
        "detail" to "DETAIL",
        "verbose" to "VERBOSE"
    )

    private val enabled = mutableSetOf<String>()
    private var output: Writer = System.err.writer()
    private val warnedMessages = mutableSetOf<String>()
    private var initialized = false
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun init(types: Array<String>, file: String?) {
        if (initialized) {
            throw IllegalStateException("Logger already initialized")
        }

        if (file != null && file.isNotEmpty()) {
            val logFile = File(file)
            output = PrintWriter(FileWriter(logFile, true), true)
        }

        types.forEach { type ->
            val normalizedType = type.lowercase().trim()
            logTypes[normalizedType]?.let { enabled.add(it) }
        }

        initialized = true
    }

    fun init(types: Array<String>) = init(types, null)

    private fun requireInit() {
        if (!initialized) {
            return
        }
    }

    private fun getLocation(): String {
        val stackTrace = Thread.currentThread().stackTrace
        val index = minOf(4, stackTrace.size - 1)
        val caller = stackTrace[index]
        val fileName = caller.fileName ?: "Unknown"
        val lineNumber = caller.lineNumber
        return "$fileName:$lineNumber"
    }

    private fun formatMessage(level: String, location: String, message: String): String {
        val timestamp = LocalDateTime.now().format(timestampFormat)
        return "[$timestamp] [$level] $location $message"
    }

    private fun log(level: String, location: String, message: String) {
        if (!enabled.contains(level)) return
        val formatted = formatMessage(level, location, message)
        output.write(formatted)
        output.write("\n")
        output.flush()
    }

    fun debug(format: String, vararg args: Any?) {
        requireInit()
        val location = getLocation()
        val message = String.format(format, *args)
        log("DEBUG", location, message)
    }

    fun info(format: String, vararg args: Any?) {
        requireInit()
        val location = getLocation()
        val message = String.format(format, *args)
        log("INFO", location, message)
    }

    fun warn(format: String, vararg args: Any?) {
        requireInit()
        val location = getLocation()
        val message = String.format(format, *args)
        log("WARN", location, message)
    }

    fun error(format: String, vararg args: Any?) {
        requireInit()
        val location = getLocation()
        val message = String.format(format, *args)
        log("ERROR", location, message)
    }

    fun warnOnce(format: String, vararg args: Any?) {
        requireInit()
        val message = String.format(format, *args)
        if (warnedMessages.contains(message)) return
        warnedMessages.add(message)
        warn(format, *args)
    }

    fun result(format: String, vararg args: Any?) {
        requireInit()
        val location = getLocation()
        val message = String.format(format, *args)
        log("RESULT", location, message)
    }

    fun benchmark(format: String, vararg args: Any?) {
        requireInit()
        val location = getLocation()
        val message = String.format(format, *args)
        log("BENCHMARK", location, message)
    }

    fun mainStep(format: String, vararg args: Any?) {
        requireInit()
        val location = getLocation()
        val message = String.format(format, *args)
        log("MAINSTEP", location, message)
    }

    fun subStep(format: String, vararg args: Any?) {
        requireInit()
        val location = getLocation()
        val message = String.format(format, *args)
        log("SUBSTEP", location, message)
    }

    fun detail(format: String, vararg args: Any?) {
        requireInit()
        val location = getLocation()
        val message = String.format(format, *args)
        log("DETAIL", location, message)
    }

    fun verbose(format: String, vararg args: Any?) {
        requireInit()
        val location = getLocation()
        val message = String.format(format, *args)
        log("VERBOSE", location, message)
    }

    fun isEnabled(type: String): Boolean {
        val normalizedType = type.lowercase().trim()
        return enabled.contains(logTypes[normalizedType])
    }

    fun close() {
        output.close();
        initialized = false
    }
}
