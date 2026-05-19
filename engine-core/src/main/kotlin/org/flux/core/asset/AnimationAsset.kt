package org.flux.core.asset

import kotlinx.serialization.Serializable
import org.flux.core.scene.AnimationClip

@Serializable
data class AnimationAsset(
    val clips: List<AnimationClip> = emptyList()
)
