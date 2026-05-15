package org.flux.core.scene

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.flux.core.util.Color
import org.joml.Vector3f

@Serializable
@SerialName("PointLight2DComponent")
@SingleComponent
class PointLight2DComponent(
    var intensity: Float = 1f,
    var radius: Float = 5f,
    @Contextual var color: Color = Color.White
) : Component()
