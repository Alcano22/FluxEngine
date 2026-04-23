package org.flux.core.renderer

import org.joml.*

class PerspectiveCamera(
    var fov: Float = 60f,
    var aspectRatio: Float = 16f / 9f,
    var nearClip: Float = 0.1f,
    var farClip: Float = 1000f
) : Camera() {

    var position = Vector3f(0f)
        set(value) {
            field.set(value)
            recalculateView()
        }

    var rotation = Vector3f(0f)
        set(value) {
            field.set(value)
            recalculateView()
        }

    init {
        recalculateProjection()
        recalculateView()
    }

    fun recalculateProjection() {
        projMatrix
            .identity()
            .perspective(Math.toRadians(fov), aspectRatio, nearClip, farClip)
        recalculateViewProjection()
    }

    private fun recalculateView() {
        val transform = Matrix4f()
            .translate(position)
            .rotateXYZ(rotation)

        transform.invert(viewMatrix)
        recalculateViewProjection()
    }
}
