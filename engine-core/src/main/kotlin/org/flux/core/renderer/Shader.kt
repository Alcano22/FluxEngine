package org.flux.core.renderer

import org.flux.core.util.Disposable
import org.joml.*

interface Shader : Disposable {

    companion object {
        fun create(
            vertexSrc: String,
            fragmentSrc: String,
            defines: Map<String, Any> = emptyMap()
        ) = Renderer.createShader(vertexSrc, fragmentSrc, defines)

        fun create(
            src: String,
            defines: Map<String, Any> = emptyMap()
        ) = Renderer.createShader(src, defines)
    }

    val rendererId: Int

    fun bind()
    fun unbind()

    fun setBool(name: String, value: Boolean) = setInt(name, if (value) 1 else 0)

    fun setInt(name: String, value: Int)
    fun setIntArray(name: String, values: IntArray)

    fun setInt2(name: String, x: Int, y: Int)
    fun setInt2(name: String, value: Vector2ic) = setInt2(name, value.x(), value.y())

    fun setInt3(name: String, x: Int, y: Int, z: Int)
    fun setInt3(name: String, value: Vector3ic) = setInt3(name, value.x(), value.y(), value.z())

    fun setInt4(name: String, x: Int, y: Int, z: Int, w: Int)
    fun setInt4(name: String, value: Vector4ic) = setInt4(name, value.x(), value.y(), value.z(), value.w())

    fun setFloat(name: String, value: Float)
    fun setFloatArray(name: String, values: FloatArray)

    fun setFloat2(name: String, x: Float, y: Float)
    fun setFloat2(name: String, value: Vector2fc) = setFloat2(name, value.x(), value.y())

    fun setFloat3(name: String, x: Float, y: Float, z: Float)
    fun setFloat3(name: String, value: Vector3fc) = setFloat3(name, value.x(), value.y(), value.z())

    fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float)
    fun setFloat4(name: String, value: Vector4fc) = setFloat4(name, value.x(), value.y(), value.z(), value.w())

    fun setMat2(name: String, matrix: Matrix2fc)
    fun setMat3(name: String, matrix: Matrix3fc)
    fun setMat4(name: String, matrix: Matrix4fc)
}