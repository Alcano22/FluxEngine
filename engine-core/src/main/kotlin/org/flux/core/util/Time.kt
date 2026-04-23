package org.flux.core.util

object Time {

    private val startTime = System.nanoTime()

    val time get() = (System.nanoTime() - startTime) / 1_000_000_000f
}

@JvmInline
value class Timestep(val time: Float = 0f) {
    val seconds get() = time
    val milliseconds get() = time * 1000f
}
