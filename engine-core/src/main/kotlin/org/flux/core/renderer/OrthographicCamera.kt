package org.flux.core.renderer

import org.joml.*

class OrthographicCamera(
    var size: Float = 1f,
    var aspectRatio: Float = 16f / 9f,
    var nearPlane: Float = -1f,
    var farPlane: Float = 1f
) : Camera() {

    var position = Vector3f(0f, 0f, 0f)
        set(value) {
            field.set(value)
            recalculateView()
        }

    var rotation = 0f
        set(value) {
            field = value
            recalculateView()
        }

    init {
        recalculateProjection()
    }

    fun setProjection(size: Float, aspectRatio: Float, nearPlane: Float = -1f, farPlane: Float = 1f) {
        this.size = size
        this.aspectRatio = aspectRatio
        this.nearPlane = nearPlane
        this.farPlane = farPlane
        recalculateProjection()
    }

    fun recalculateProjection() {
        val left = -size * aspectRatio
        val right = size * aspectRatio
        val bottom = -size
        val top = size

        projMatrix.setOrtho(left, right, bottom, top, nearPlane, farPlane)
        recalculateViewProjection()
    }

    private fun recalculateView() {
        val transform = Matrix4f()
            .translate(position)
            .rotateZ(rotation)

        transform.invert(viewMatrix)
        recalculateViewProjection()
    }
}
