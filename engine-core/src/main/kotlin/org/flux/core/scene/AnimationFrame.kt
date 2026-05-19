package org.flux.core.scene

import kotlinx.serialization.Serializable
import org.flux.core.asset.TextureHandle

@Serializable
sealed class AnimationFrame {
    @Serializable
    data class SheetFrame(
        val u0: Float, val v0: Float,
        val u1: Float, val v1: Float
    ) : AnimationFrame()

    @Serializable
    data class TextureFrame(
        val handle: TextureHandle
    ) : AnimationFrame()
}
