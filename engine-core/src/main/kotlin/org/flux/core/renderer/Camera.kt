package org.flux.core.renderer

import org.joml.Matrix4f

abstract class Camera {

    val projMatrix = Matrix4f()
    val viewMatrix = Matrix4f()
    val viewProjMatrix = Matrix4f()

    protected fun recalculateViewProjection() {
        projMatrix.mul(viewMatrix, viewProjMatrix)
    }
}
