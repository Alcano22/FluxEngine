package org.flux.core.scene

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.flux.core.asset.SpriteSource
import org.flux.core.renderer.Renderer2D
import org.flux.core.asset.resolve
import org.flux.core.util.Color

@Serializable
@SerialName("SpriteRendererComponent")
@SingleComponent
class SpriteRendererComponent(
    var source: SpriteSource? = null,
    @Contextual var color: Color = Color.White
) : Component() {

    override fun onRender2D() {
        val frame = entity.getComponent<SpriteAnimatorComponent>()?.currentFrame
            ?: source

        when (frame) {
            is SpriteSource.FromTexture ->
                Renderer2D.drawQuad(transform.matrix, frame.handle.resolve(), color, entity.id)
            is SpriteSource.FromSprite ->
                Renderer2D.drawQuad(transform.matrix, frame.sprite, color, entity.id)
            null ->
                Renderer2D.drawQuad(transform.matrix, null, color, entity.id)
        }
    }
}
