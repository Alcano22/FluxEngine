package org.flux.opengl.renderer

import org.flux.core.renderer.Texture2D
import org.flux.core.renderer.TextureFilter
import org.flux.core.logging.logger
import org.flux.core.util.memScoped
import org.flux.core.logging.require
import org.flux.core.renderer.TextureParams
import org.flux.core.renderer.TextureWrap
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL46C.*
import org.lwjgl.stb.STBImage.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

class GLTexture2D : Texture2D {

    companion object {
        val logger = logger()
    }

    override val rendererId: Int

    override val width: Int
    override val height: Int

    private var internalFormat = 0
    private var dataFormat = 0

    constructor(width: Int, height: Int, params: TextureParams) {
        this.width = width
        this.height = height
        internalFormat = GL_RGBA8
        dataFormat = GL_RGBA

        rendererId = glCreateTextures(GL_TEXTURE_2D)
        glTextureStorage2D(rendererId, 1, internalFormat, width, height)
        applyParams(rendererId, params, hasMipmaps = false)
    }

    constructor(path: String, params: TextureParams) {
        var tmpId = 0
        var tmpWidth = 0
        var tmpHeight = 0

        memScoped {
            val w = mallocInt(1)
            val h = mallocInt(1)
            val channels = mallocInt(1)

            stbi_set_flip_vertically_on_load(true)
            val pixels = stbi_load(path, w, h, channels, 0)
                ?: throw logger.throwing(RuntimeException("Failed to load texture: $path:\n${stbi_failure_reason()}"))
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
                else -> throw logger.throwing(RuntimeException("Unsupported number of channels: ${channels[0]}"))
            }

            val levels = if (params.generateMipmaps) mipLevels(tmpWidth, tmpHeight) else 1
            tmpId = glCreateTextures(GL_TEXTURE_2D)
            glTextureStorage2D(tmpId, levels, internalFormat, tmpWidth, tmpHeight)
            glTextureSubImage2D(tmpId, 0, 0, 0, tmpWidth, tmpHeight, dataFormat, GL_UNSIGNED_BYTE, pixels)

            if (params.generateMipmaps)
                glGenerateTextureMipmap(tmpId)
            applyParams(tmpId, params, hasMipmaps = params.generateMipmaps)

            stbi_image_free(pixels)
        }

        rendererId = tmpId
        width = tmpWidth
        height = tmpHeight
    }

    constructor(bytes: ByteArray, params: TextureParams) {
        var tmpId = 0
        var tmpWidth = 0
        var tmpHeight = 0

        var buffer: ByteBuffer? = null
        try {
            buffer = MemoryUtil.memAlloc(bytes.size)
                .put(bytes)
                .flip()

            memScoped {
                val w = mallocInt(1)
                val h = mallocInt(1)
                val channels = mallocInt(1)

                stbi_set_flip_vertically_on_load(true)
                val pixels = stbi_load_from_memory(buffer, w, h, channels, 0)
                    ?: throw logger.throwing(
                        RuntimeException("Failed to load texture from memory:\n${stbi_failure_reason()}")
                    )
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
                    else -> throw logger.throwing(RuntimeException("Unsupported number of channels: ${channels[0]}"))
                }

                val levels = if (params.generateMipmaps) mipLevels(tmpWidth, tmpHeight) else 1
                tmpId = glCreateTextures(GL_TEXTURE_2D)
                glTextureStorage2D(tmpId, levels, internalFormat, tmpWidth, tmpHeight)
                glTextureSubImage2D(tmpId, 0, 0, 0, tmpWidth, tmpHeight, dataFormat, GL_UNSIGNED_BYTE, pixels)

                if (params.generateMipmaps)
                    glGenerateTextureMipmap(tmpId)
                applyParams(tmpId, params, hasMipmaps = params.generateMipmaps)

                stbi_image_free(pixels)
            }
        } finally {
            if (buffer != null)
                MemoryUtil.memFree(buffer)
        }

        rendererId = tmpId
        width = tmpWidth
        height = tmpHeight
    }

    private fun applyParams(id: Int, params: TextureParams, hasMipmaps: Boolean) {
        val minFilter = when {
            hasMipmaps && params.minFilter == TextureFilter.LINEAR  -> GL_LINEAR_MIPMAP_LINEAR
            hasMipmaps && params.minFilter == TextureFilter.NEAREST -> GL_NEAREST_MIPMAP_NEAREST
            else                                                    -> params.minFilter.toGL()
        }
        glTextureParameteri(id, GL_TEXTURE_MIN_FILTER, minFilter)
        glTextureParameteri(id, GL_TEXTURE_MAG_FILTER, params.magFilter.toGL())
        glTextureParameteri(id, GL_TEXTURE_WRAP_S, params.wrapS.toGL())
        glTextureParameteri(id, GL_TEXTURE_WRAP_T, params.wrapT.toGL())
    }

    private fun TextureFilter.toGL(): Int = when (this) {
        TextureFilter.LINEAR  -> GL_LINEAR
        TextureFilter.NEAREST -> GL_NEAREST
    }

    private fun TextureWrap.toGL(): Int = when (this) {
        TextureWrap.REPEAT          -> GL_REPEAT
        TextureWrap.CLAMP_TO_EDGE   -> GL_CLAMP_TO_EDGE
        TextureWrap.MIRRORED_REPEAT -> GL_MIRRORED_REPEAT
    }

    private fun mipLevels(width: Int, height: Int): Int {
        var levels = 1
        var w = width
        var h = height
        while (w > 1 || h > 1) {
            w = maxOf(1, w / 2)
            h = maxOf(1, h / 2)
            levels++
        }
        return levels
    }

    override fun setData(data: ByteArray) {
        val bpp = if (dataFormat == GL_RGBA) 4 else 3
        logger.require(data.size == width * height * bpp) {
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
