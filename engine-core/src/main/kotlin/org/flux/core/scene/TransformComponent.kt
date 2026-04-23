package org.flux.core.scene

import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector3fc

@SingleComponent
class TransformComponent(
    position: Vector3fc = Vector3f(0f),
    rotation: Vector3fc = Vector3f(0f),
    scale: Vector3fc = Vector3f(1f)
) : Component() {

    val position = Vector3f(position)
    val rotation = Vector3f(rotation)
    val scale = Vector3f(scale)

    private val lastPosition = Vector3f(position)
    private val lastRotation = Vector3f(rotation)
    private val lastScale = Vector3f(scale)

    private val _matrix = Matrix4f()
    private var isDirty = true

    val matrix: Matrix4f
        get() {
            if (position != lastPosition || rotation != lastRotation || scale != lastScale)
                isDirty = true

            if (isDirty) {
                _matrix.identity()
                    .translate(position)
                    .rotateXYZ(rotation)
                    .scale(scale)

                lastPosition.set(position)
                lastRotation.set(rotation)
                lastScale.set(scale)

                isDirty = false
            }

            return _matrix
        }

}
