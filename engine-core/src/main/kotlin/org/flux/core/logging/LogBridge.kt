package org.flux.core.logging

import ch.qos.logback.classic.Level
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

enum class LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR
}

data class LogEntry(
    val level: LogLevel,
    val message: String,
    val loggerName: String,
    val timestamp: String,
    val className: String? = null,
    val fileName: String? = null,
    val lineNumber: Int = -1
)

fun interface LogListener {
    fun onLog(entry: LogEntry)
}

object LogBridge {

    private const val HISTORY_SIZE = 256

    private val listeners = mutableListOf<LogListener>()
    private val historyBuffer = LinkedList<LogEntry>()
    private val dtf = DateTimeFormatter
        .ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    fun addListener(listener: LogListener) {
        synchronized(listeners) {
            listeners.add(listener)
            val historyCopy = ArrayList(historyBuffer)
            for (entry in historyCopy)
                listener.onLog(entry)
        }
    }

    fun removeListener(listener: LogListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    internal fun submitLog(
        levelInt: Int,
        message: String,
        loggerName: String,
        timestampEpoch: Long,
        className: String?,
        fileName: String?,
        lineNumber: Int
    ) {
        val level = mapLevel(levelInt)
        val timeStr = dtf.format(Instant.ofEpochMilli(timestampEpoch))
        val entry = LogEntry(level, message, loggerName, timeStr, className, fileName, lineNumber)

        synchronized(listeners) {
            historyBuffer.add(entry)
            if (historyBuffer.size > HISTORY_SIZE)
                historyBuffer.removeFirst()
            val activeListeners = ArrayList(listeners)
            activeListeners.forEach { it.onLog(entry) }
        }
    }

    private fun mapLevel(levelInt: Int): LogLevel = when (levelInt) {
        Level.TRACE_INT -> LogLevel.TRACE
        Level.DEBUG_INT -> LogLevel.DEBUG
        Level.INFO_INT  -> LogLevel.INFO
        Level.WARN_INT  -> LogLevel.WARN
        Level.ERROR_INT -> LogLevel.ERROR
        else            -> LogLevel.INFO
    }
}
