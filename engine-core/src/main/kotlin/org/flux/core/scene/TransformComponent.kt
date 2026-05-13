package org.flux.core.scene

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.joml.Matrix4f
import org.joml.Vector3f

@Serializable
@SerialName("TransformComponent")
@SingleComponent
class TransformComponent(
    @Contextual var position: Vector3f = Vector3f(0f),
    @Contextual var rotation: Vector3f = Vector3f(0f),
    @Contextual var scale: Vector3f = Vector3f(1f)
) : Component() {

    @Transient private val lastPosition = Vector3f(position)
    @Transient private val lastRotation = Vector3f(rotation)
    @Transient private val lastScale = Vector3f(scale)

    @Transient private val _matrix = Matrix4f()
    @Transient private var isDirty = true

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
