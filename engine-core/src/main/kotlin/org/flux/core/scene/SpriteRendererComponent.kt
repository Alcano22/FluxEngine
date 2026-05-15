package org.flux.core.scene

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.flux.core.renderer.Renderer2D
import org.flux.core.renderer.Texture2D
import org.flux.core.util.Color
import org.joml.Vector4f

@Serializable
@SerialName("SpriteRendererComponent")
@SingleComponent
class SpriteRendererComponent(
    @Transient var texture: Texture2D? = null,
    @Contextual var color: Color = Color.White
) : Component() {

    override fun onRender2D() {
        Renderer2D.drawQuad(transform.matrix, texture, color, entity.id)
    }
}
