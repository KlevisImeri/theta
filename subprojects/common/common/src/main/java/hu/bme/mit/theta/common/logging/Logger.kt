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
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

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
    private val lock = ReentrantReadWriteLock()

    private fun logBeforeInit(level: String, message: String) {
        val timestamp = LocalDateTime.now().format(timestampFormat)
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace.find { it.className == "hu.bme.mit.theta.common.logging.Logger" && it.methodName != "logBeforeInit" }
        val fileName = caller?.fileName ?: "Unknown"
        val lineNumber = caller?.lineNumber ?: -1
        val methodName = caller?.methodName ?: "unknown"
        val context = if (level == "ERROR") " - Check pattern format, file permissions, or file path" else ""
        System.err.println("[$timestamp] [$level] $fileName:$lineNumber Logger.$methodName: $message$context")
    }

    fun init(pattern: String, file: String?) {
        lock.writeLock().withLock {
            if (initialized) {
                logBeforeInit("ERROR", "Logger already initialized")
                throw IllegalStateException("Logger already initialized")
            }

            if (file != null && file.isNotEmpty()) {
                try {
                    val logFile = File(file)
                    output = PrintWriter(FileWriter(logFile, true), true)
                    logBeforeInit("INFO", "Logging to file: $file")
                } catch (e: Exception) {
                    logBeforeInit("ERROR", "Failed to open log file: ${e.message}")
                    throw e
                }
            } else {
                logBeforeInit("INFO", "Logging to stderr")
            }

            val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
            logTypes.values.forEach { type ->
                if (regex.matches(type)) {
                    enabled.add(type)
                }
            }

            if (enabled.isEmpty()) {
                logBeforeInit("WARN", "No log types matched pattern: $pattern")
            } else {
                logBeforeInit("INFO", "Enabled log types: ${enabled.joinToString(", ")}")
            }

            initialized = true
        }
    }

    fun init(pattern: String) = init(pattern, null)

    private fun requireInit(): Boolean {
        lock.readLock().withLock { 
            return initialized
        }
    }

    private fun getLocation(): String {
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace.asSequence()
            .dropWhile { 
                it.className.startsWith("hu.bme.mit.theta.common.logging.Logger") ||
                it.className.startsWith("java.") ||
                it.className.startsWith("jdk.") ||
                it.className.startsWith("sun.") ||
                it.className.startsWith("kotlin.") ||
                it.className.startsWith("kotlinx.") ||
                it.className.startsWith("org.junit.")
            }
            .firstOrNull()
        val fileName = caller?.fileName ?: "Unknown"
        val lineNumber = caller?.lineNumber ?: -1
        return "$fileName:$lineNumber"
    }

    private fun formatMessage(level: String, location: String, message: String): String {
        val timestamp = LocalDateTime.now().format(timestampFormat)
        return "[$timestamp] [$level] $location $message"
    }

    private fun log(level: String, format: String, vararg args: Any?) {
        val shouldLog = lock.readLock().withLock {
            enabled.contains(level)
        }
        if (!shouldLog) return
        val location = getLocation()
        val message = String.format(format, *args)
        val formatted = formatMessage(level, location, message)
        lock.writeLock().withLock {
            output.write(formatted)
            output.write("\n")
            output.flush()
        }
    }

    fun debug(format: String, vararg args: Any?) {
        if (!requireInit()) return
        log("DEBUG", format, *args)
    }

    fun info(format: String, vararg args: Any?) {
        if (!requireInit()) return
        log("INFO", format, *args)
    }

    fun warn(format: String, vararg args: Any?) {
        if (!requireInit()) return
        log("WARN", format, *args)
    }

    fun error(format: String, vararg args: Any?) {
        if (!requireInit()) return
        log("ERROR", format, *args)
    }

    fun warnOnce(format: String, vararg args: Any?) {
        if (!requireInit()) return
        val message = String.format(format, *args)
        val isNew = lock.writeLock().withLock {
            if (warnedMessages.contains(message)) false
            else {
                warnedMessages.add(message)
                true
            }
        }
        if (isNew) warn(format, *args)
    }

    fun result(format: String, vararg args: Any?) {
        if (!requireInit()) return
        log("RESULT", format, *args)
    }

    fun benchmark(format: String, vararg args: Any?) {
        if (!requireInit()) return
        log("BENCHMARK", format, *args)
    }

    fun mainStep(format: String, vararg args: Any?) {
        if (!requireInit()) return
        log("MAINSTEP", format, *args)
    }

    fun subStep(format: String, vararg args: Any?) {
        if (!requireInit()) return
        log("SUBSTEP", format, *args)
    }

    fun detail(format: String, vararg args: Any?) {
        if (!requireInit()) return
        log("DETAIL", format, *args)
    }

    fun verbose(format: String, vararg args: Any?) {
        if (!requireInit()) return
        log("VERBOSE", format, *args)
    }

    fun isEnabled(type: String): Boolean {
        val normalizedType = type.lowercase().trim()
        return lock.readLock().withLock {
            enabled.contains(logTypes[normalizedType])
        }
    }

    fun close() {
        lock.writeLock().withLock {
            try {
                output.close()
                logBeforeInit("INFO", "Logger closed")
            } catch (e: Exception) {
                logBeforeInit("ERROR", "Failed to close logger: ${e.message}")
            } finally {
                initialized = false
                enabled.clear()
                warnedMessages.clear()
            }
        }
    }

    @JvmStatic
    internal fun resetForTest() {
        lock.writeLock().withLock {
            enabled.clear()
            warnedMessages.clear()
            initialized = false
        }
    }
}
