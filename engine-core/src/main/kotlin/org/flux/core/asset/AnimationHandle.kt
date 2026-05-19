package org.flux.core.asset

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class AnimationHandle(val path: String) {
    val animation get() = AssetManager.getAnimation(path)
}
