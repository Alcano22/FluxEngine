package org.flux.core.asset.meta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.flux.core.renderer.TextureFilter
import org.flux.core.renderer.TextureWrap

@Serializable
@SerialName("ImageMeta")
data class ImageMeta(
    var minFilter: TextureFilter = TextureFilter.LINEAR,
    var magFilter: TextureFilter = TextureFilter.LINEAR,
    var wrapS: TextureWrap = TextureWrap.REPEAT,
    var wrapT: TextureWrap = TextureWrap.REPEAT,
    var generateMipmaps: Boolean = false
) : AssetMeta()
