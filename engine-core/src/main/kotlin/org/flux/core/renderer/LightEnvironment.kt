package org.flux.core.renderer

import org.flux.core.util.Color
import org.joml.Vector2f
import org.joml.Vector3f

data class PointLight2DData(
    val position: Vector2f = Vector2f(),
    val color: Color = Color.White,
    val intensity: Float = 1f,
    val radius: Float = 5f
)

class LightEnvironment {
    var ambientColor: Color = Color.White
    var ambientIntensity: Float = 0.1f
    val pointLights = mutableListOf<PointLight2DData>()
}
