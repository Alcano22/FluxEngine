package org.flux.core.renderer

enum class TextureFilter {
    LINEAR,
    NEAREST
}

enum class TextureWrap {
    REPEAT,
    CLAMP_TO_EDGE,
    MIRRORED_REPEAT
}

data class TextureParams(
    val minFilter: TextureFilter = TextureFilter.LINEAR,
    val magFilter: TextureFilter = TextureFilter.LINEAR,
    val wrapS: TextureWrap = TextureWrap.REPEAT,
    val wrapT: TextureWrap = TextureWrap.REPEAT,
    val generateMipmaps: Boolean = false
)

interface Texture2D : Texture {

    companion object {
        fun create(
            width: Int,
            height: Int,
            params: TextureParams = TextureParams()
        ) = Renderer.createTexture2D(width, height, params)

        fun create(
            path: String,
            params: TextureParams = TextureParams()
        ) = Renderer.createTexture2D(path, params)

        fun create(
            bytes: ByteArray,
            params: TextureParams = TextureParams()
        ) = Renderer.createTexture2D(bytes, params)
    }

    fun setData(data: ByteArray)

    fun getPixels(): ByteArray
}
