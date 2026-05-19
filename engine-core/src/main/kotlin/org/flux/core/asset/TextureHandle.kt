package org.flux.core.asset

import kotlinx.serialization.Serializable
import org.flux.core.renderer.Texture2D

@JvmInline
@Serializable
value class TextureHandle(val path: String) {
    val texture: Texture2D get() = AssetManager.getTexture(path)
}