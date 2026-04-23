package org.flux.opengl.renderer

import org.flux.core.renderer.Texture2D
import org.flux.core.renderer.TextureFilter
import org.flux.core.util.memScoped
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL46C.*
import org.lwjgl.stb.STBImage.*

class GLTexture2D : Texture2D {

    private val rendererId: Int

    override val width: Int
    override val height: Int

    private var internalFormat = 0
    private var dataFormat = 0

    constructor(width: Int, height: Int, filter: TextureFilter) {
        this.width = width
        this.height = height
        internalFormat = GL_RGBA8
        dataFormat = GL_RGBA

        rendererId = glCreateTextures(GL_TEXTURE_2D)
        glTextureStorage2D(rendererId, 1, internalFormat, width, height)

        val glFilter = filter.toGL()
        glTextureParameteri(rendererId, GL_TEXTURE_MIN_FILTER, glFilter)
        glTextureParameteri(rendererId, GL_TEXTURE_MAG_FILTER, glFilter)
        glTextureParameteri(rendererId, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTextureParameteri(rendererId, GL_TEXTURE_WRAP_T, GL_REPEAT)
    }

    constructor(path: String, filter: TextureFilter) {
        var tmpId = 0
        var tmpWidth = 0
        var tmpHeight = 0

        memScoped {
            val w = mallocInt(1)
            val h = mallocInt(1)
            val channels = mallocInt(1)

            stbi_set_flip_vertically_on_load(true)
            val pixels = stbi_load(path, w, h, channels, 0)
                ?: throw RuntimeException("Failed to load texture: $path:\n${stbi_failure_reason()}")
            stbi_set_flip_vertically_on_load(false)

            tmpWidth = w[0]
            tmpHeight = h[0]

            when (channels[0]) {
                4 -> {
                    internalFormat = GL_RGBA8
                    dataFormat = GL_RGBA
                }
                3 -> {
                    internalFormat = GL_RGB8
                    dataFormat = GL_RGB
                }
                else -> throw RuntimeException("Unsupported number of channels: ${channels[0]}")
            }

            tmpId = glCreateTextures(GL_TEXTURE_2D)
            glTextureStorage2D(tmpId, 1, internalFormat, tmpWidth, tmpHeight)

            val glFilter = filter.toGL()
            glTextureParameteri(tmpId, GL_TEXTURE_MIN_FILTER, glFilter)
            glTextureParameteri(tmpId, GL_TEXTURE_MAG_FILTER, glFilter)
            glTextureParameteri(tmpId, GL_TEXTURE_WRAP_S, GL_REPEAT)
            glTextureParameteri(tmpId, GL_TEXTURE_WRAP_T, GL_REPEAT)

            glTextureSubImage2D(tmpId, 0, 0, 0, tmpWidth, tmpHeight, dataFormat, GL_UNSIGNED_BYTE, pixels)

            stbi_image_free(pixels)
        }

        rendererId = tmpId
        width = tmpWidth
        height = tmpHeight
    }

    private fun TextureFilter.toGL(): Int = when (this) {
        TextureFilter.LINEAR  -> GL_LINEAR
        TextureFilter.NEAREST -> GL_NEAREST
    }

    override fun setData(data: ByteArray) {
        val bpp = if (dataFormat == GL_RGBA) 4 else 3
        require(data.size == width * height * bpp) {
            "Data size does not match texture dimensions"
        }

        val buf = BufferUtils.createByteBuffer(data.size)
        buf.put(data)
        buf.flip()

        glTextureSubImage2D(rendererId, 0, 0, 0, width, height, dataFormat, GL_UNSIGNED_BYTE, buf)
    }

    override fun bind(slot: Int) = glBindTextureUnit(slot, rendererId)

    override fun dispose() = glDeleteTextures(rendererId)
}
