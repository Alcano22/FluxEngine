package org.flux.core.util

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

const val nullptr = MemoryUtil.NULL

inline fun <T> memScoped(block: MemoryStack.() -> T): T =
    MemoryStack.stackPush().use { it.block() }
