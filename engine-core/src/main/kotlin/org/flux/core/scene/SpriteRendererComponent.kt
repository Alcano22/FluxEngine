package org.flux.core.scene

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.flux.core.logging.logger
import org.flux.core.renderer.Renderer2D
import org.flux.core.renderer.Texture2D
import org.flux.core.asset.TextureHandle
import org.flux.core.util.Color
import org.joml.Vector4f

@Serializable
@SerialName("SpriteRendererComponent")
@SingleComponent
class SpriteRendererComponent(
    var textureHandle: TextureHandle? = null,
    @Contextual var color: Color = Color.White
) : Component() {

    override fun onRender2D() {
        val animator = entity.getComponent<SpriteAnimatorComponent>()
        val frame = animator?.currentFrame
        val clip = animator?.currentClip

        val texture = (clip?.textureHandle ?: textureHandle)?.texture

        when (frame) {
            is AnimationFrame.TextureFrame -> Renderer2D.drawQuad(
                transform = transform.matrix,
                texture   = frame.handle.texture,
                color     = color,
                entityId  = entity.id
            )
            is AnimationFrame.SheetFrame -> Renderer2D.drawQuad(
                transform = transform.matrix,
                texture   = texture,
                frame     = frame,
                color     = color,
                entityId  = entity.id
            )
            null -> Renderer2D.drawQuad(
                transform = transform.matrix,
                texture   = textureHandle?.texture,
                color     = color,
                entityId  = entity.id
            )
        }
    }
}
