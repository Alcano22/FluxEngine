package org.flux.opengl.renderer

import org.flux.core.renderer.Framebuffer
import org.flux.core.renderer.FramebufferSpecification
import org.flux.core.renderer.FramebufferTextureFormat
import org.flux.core.logging.logger
import org.flux.core.logging.require
import org.lwjgl.opengl.GL46C.*

class GLFramebuffer(
    override var specification: FramebufferSpecification
) : Framebuffer {

    companion object {
        private val logger = logger()
    }

    private var rendererId = 0
    private val colorAttachments = mutableListOf<Int>()
    private var depthAttachment = 0

    override val colorAttachmentRendererId get() = colorAttachments[0]

    init {
        invalidate()
    }

    private fun invalidate() {
        if (rendererId != 0) {
            glDeleteFramebuffers(rendererId)
            colorAttachments.forEach { glDeleteTextures(it) }
            glDeleteTextures(depthAttachment)
            colorAttachments.clear()
        }

        rendererId = glCreateFramebuffers()

        val attachmentSpecs = specification.attachments.attachments
        var depthFormat = FramebufferTextureFormat.NONE

        attachmentSpecs.forEachIndexed { index, spec ->
            if (spec.format == FramebufferTextureFormat.DEPTH24STENCIL8)
                depthFormat = spec.format
            else {
                val texId = createColorTexture(specification.width, specification.height, spec.format)
                glNamedFramebufferTexture(rendererId, GL_COLOR_ATTACHMENT0 + index, texId, 0)
                colorAttachments.add(texId)
            }
        }

        if (depthFormat != FramebufferTextureFormat.NONE) {
            depthAttachment = createDepthTexture(specification.width, specification.height)
            glNamedFramebufferTexture(rendererId, GL_DEPTH_STENCIL_ATTACHMENT, depthAttachment, 0)
        }

        if (colorAttachments.size > 1) {
            val buffers = IntArray(colorAttachments.size) { GL_COLOR_ATTACHMENT0 + it }
            glNamedFramebufferDrawBuffers(rendererId, buffers)
        } else if (colorAttachments.isEmpty())
            glNamedFramebufferDrawBuffer(rendererId, GL_NONE)

        val status = glCheckNamedFramebufferStatus(rendererId, GL_FRAMEBUFFER)
        logger.require(status == GL_FRAMEBUFFER_COMPLETE) {
            "Framebuffer $rendererId is incomplete: $status"
        }
    }

    private fun createDepthTexture(width: Int, height: Int): Int {
        val id = glCreateTextures(GL_TEXTURE_2D)
        glTextureStorage2D(id, 1, GL_DEPTH24_STENCIL8, width, height)
        return id
    }

    private fun createColorTexture(width: Int, height: Int, format: FramebufferTextureFormat): Int {
        val id = glCreateTextures(GL_TEXTURE_2D)

        when (format) {
            FramebufferTextureFormat.RGBA8 -> {
                glTextureStorage2D(id, 1, GL_RGBA8, width, height)
                glTextureParameteri(id, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
                glTextureParameteri(id, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            }
            FramebufferTextureFormat.R32I -> {
                glTextureStorage2D(id, 1, GL_R32I, width, height)
                glTextureParameteri(id, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
                glTextureParameteri(id, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            }
            else -> throw logger.throwing(
                IllegalArgumentException("Format $format is not supported for color attachment")
            )
        }

        glTextureParameteri(id, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTextureParameteri(id, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        return id
    }

    override fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, rendererId)
        glViewport(0, 0, specification.width, specification.height)
    }

    override fun unbind() = glBindFramebuffer(GL_FRAMEBUFFER, 0)

    override fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return

        specification.width = width
        specification.height = height
        invalidate()
    }

    override fun readPixel(attachmentIndex: Int, x: Int, y: Int): Int {
        require(attachmentIndex < colorAttachments.size)

        glBindFramebuffer(GL_READ_FRAMEBUFFER, rendererId)
        glReadBuffer(GL_COLOR_ATTACHMENT0 + attachmentIndex)

        val pixelData = IntArray(1)
        glReadPixels(x, y, 1, 1, GL_RED_INTEGER, GL_INT, pixelData)

        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0)
        return pixelData[0]
    }

    override fun setDrawBuffers(vararg attachmentIndices: Int) {
        val buffers = IntArray(attachmentIndices.size) { GL_COLOR_ATTACHMENT0 + attachmentIndices[it] }
        glNamedFramebufferDrawBuffers(rendererId, buffers)
    }

    override fun clearColorAttachmentInt(attachmentIndex: Int, value: Int) {
        require(attachmentIndex < colorAttachments.size)
        glClearNamedFramebufferiv(rendererId, GL_COLOR, attachmentIndex, intArrayOf(value))
    }

    override fun dispose() {
        glDeleteFramebuffers(rendererId)
        colorAttachments.forEach { glDeleteTextures(it) }
        glDeleteTextures(depthAttachment)
    }
}
