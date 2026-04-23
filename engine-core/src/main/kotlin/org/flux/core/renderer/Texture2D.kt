package org.flux.core.renderer

enum class TextureFilter {
    LINEAR,
    NEAREST
}

interface Texture2D : Texture {

    companion object {
        fun create(
            width: Int,
            height: Int,
            filter: TextureFilter = TextureFilter.LINEAR
        ) = Renderer.createTexture2D(width, height, filter)

        fun create(
            path: String,
            filter: TextureFilter = TextureFilter.LINEAR
        ) = Renderer.createTexture2D(path, filter)
    }

    fun setData(data: ByteArray)
}
