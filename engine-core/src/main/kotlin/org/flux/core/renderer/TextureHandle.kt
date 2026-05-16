package org.flux.core.renderer

import kotlinx.serialization.Serializable
import org.flux.core.asset.AssetManager

@JvmInline
@Serializable
value class TextureHandle(val path: String) {
    val texture: Texture2D get() = AssetManager.getTexture(path)
}
