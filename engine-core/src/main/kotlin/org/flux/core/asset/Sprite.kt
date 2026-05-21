package org.flux.core.asset

import kotlinx.serialization.Serializable

@Serializable
data class Sprite(
    val spritesheet: SpritesheetHandle,
    val frameIndex: Int
) {
    val texture get() = spritesheet.resolve().texture.resolve()

    fun computeUVs() = spritesheet.resolve().computeUVs(frameIndex)
}
