package org.flux.core.asset

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SpriteSource {

    val texture get() = when (this) {
        is FromTexture -> handle.resolve()
        is FromSprite  -> sprite.texture
    }

    @Serializable
    @SerialName("TEXTURE")
    data class FromTexture(val handle: TextureHandle) : SpriteSource()

    @Serializable
    @SerialName("SPRITE")
    data class FromSprite(val sprite: Sprite) : SpriteSource()
}
