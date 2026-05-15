package org.flux.core.util

import kotlinx.serialization.Serializable
import org.joml.Vector3f
import org.joml.Vector4f

@Serializable
data class Color(
    var r: Float,
    var g: Float,
    var b: Float,
    var a: Float = 1f
) {
    companion object {
        val White   get() = Color(1f, 1f, 1f, 1f)
        val Black   get() = Color(0f, 0f, 0f, 1f)
        val Clear   get() = Color(0f, 0f, 0f, 0f)
        val Red     get() = Color(1f, 0f, 0f, 1f)
        val Green   get() = Color(0f, 1f, 0f, 1f)
        val Blue    get() = Color(0f, 0f, 1f, 1f)
        val Magenta get() = Color(1f, 0f, 1f, 1f)
        val Yellow  get() = Color(1f, 1f, 0f, 1f)
        val Cyan    get() = Color(0f, 1f, 1f, 1f)
    }

    constructor(r: Int, g: Int, b: Int, a: Int = 255) : this(
        r = r / 255f,
        g = g / 255f,
        b = b / 255f,
        a = a / 255f
    )

    constructor(hex: Int) : this(
        r = ((hex shr 16) and 0xFF) / 255f,
        g = ((hex shr 8)  and 0xFF) / 255f,
        b = (hex and 0xFF) / 255f,
        a = if ((hex ushr 24) and 0xFF == 0) 1f else ((hex ushr 24) and 0xFF) / 255f
    )

    fun set(r: Float, g: Float, b: Float, a: Float = 1f) {
        this.r = r
        this.g = g
        this.b = b
        this.a = a
    }

    fun set(r: Int, g: Int, b: Int, a: Int = 255) = set(
        r / 255f,
        g / 255f,
        b / 255f,
        a / 255f
    )

    fun toVector4f() = Vector4f(r, g, b, a)

    fun toVector3f() = Vector3f(r, g, b)

    fun toImGuiColor(): Int {
        val ri = (r * 255).toInt()
        val gi = (g * 255).toInt()
        val bi = (b * 255).toInt()
        val ai = (a * 255).toInt()
        return (ai shl 24) or (bi shl 16) or (gi shl 8) or ri
    }
}
