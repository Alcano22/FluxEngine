package org.flux.core.logging

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object EngineLogger {

    private class BridgeAppender : AppenderBase<ILoggingEvent>() {
        override fun append(event: ILoggingEvent) {
            val callerData = event.callerData
            val firstCaller = callerData?.firstOrNull()
            LogBridge.submitLog(
                event.level.toInt(),
                event.formattedMessage,
                event.loggerName,
                event.timeStamp,
                firstCaller?.className,
                firstCaller?.fileName ?: "Unknown",
                firstCaller?.lineNumber ?: -1
            )
        }
    }

    fun attachBridge() {
        val ctx = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return
        val bridge = BridgeAppender()
        bridge.context = ctx
        bridge.name = "ENGINE_BRIDGE_INTERNAL"
        bridge.start()
        val rootLogger = ctx.getLogger(Logger.ROOT_LOGGER_NAME)
        rootLogger.addAppender(bridge)
    }
}
