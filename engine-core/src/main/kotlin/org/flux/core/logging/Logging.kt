package org.flux.core.logging

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

fun Any.logger(info: String? = null) = KotlinLogging.logger(
    with(this::class) {
        val name = if (isCompanion) java.enclosingClass.simpleName else simpleName
        "$name${if (info != null) "($info)" else ""}"
    }
)

fun KLogger.require(condition: Boolean, msg: () -> String) {
    if (!condition)
        throw throwing(IllegalArgumentException(msg()))
}
