package org.flux.core.scene

import kotlinx.serialization.Serializable
import org.flux.core.asset.SpriteSource
import org.flux.core.asset.TextureHandle

@Serializable
data class AnimationClip(
    val name: String,
    val frames: List<SpriteSource> = emptyList(),
    val fps: Float = 12f,
    val loop: Boolean = true
)
