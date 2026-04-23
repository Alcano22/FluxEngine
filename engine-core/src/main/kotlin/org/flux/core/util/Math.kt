package org.flux.core.util

import org.joml.*

fun Vector2ic.toArray() = intArrayOf(x(), y())
fun Vector3ic.toArray() = intArrayOf(x(), y(), z())
fun Vector4ic.toArray() = intArrayOf(x(), y(), z(), w())

fun Vector2fc.toArray() = floatArrayOf(x(), y())
fun Vector3fc.toArray() = floatArrayOf(x(), y(), z())
fun Vector4fc.toArray() = floatArrayOf(x(), y(), z(), w())
