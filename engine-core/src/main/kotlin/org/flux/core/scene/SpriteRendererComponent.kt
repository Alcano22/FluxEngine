package org.flux.core.scene

import org.flux.core.renderer.Renderer2D
import org.flux.core.renderer.Texture2D
import org.joml.Vector4f

@SingleComponent
class SpriteRendererComponent(
    var texture: Texture2D? = null,
    var color: Vector4f = Vector4f(1f)
) : Component() {

    override fun onRender2D() {
        Renderer2D.drawQuad(transform.matrix, texture, color)
    }
}
