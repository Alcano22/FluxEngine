package org.flux.core.renderer

import org.flux.core.util.Disposable

enum class FramebufferTextureFormat {
    NONE,
    RGBA8,
    R32I,
    DEPTH24STENCIL8
}

data class FramebufferTextureSpecification(val format: FramebufferTextureFormat)

data class FramebufferAttachmentSpecification(val attachments: List<FramebufferTextureSpecification>)

data class FramebufferSpecification(
    var width: Int = 0,
    var height: Int = 0,
    var attachments: FramebufferAttachmentSpecification = FramebufferAttachmentSpecification(emptyList()),
    var samples: Int = 1,
    var swapChainTarget: Boolean = false
)

interface Framebuffer : Disposable {

    companion object {
        fun create(spec: FramebufferSpecification) = Renderer.createFramebuffer(spec)
    }

    val specification: FramebufferSpecification
    val colorAttachmentRendererId: Int

    fun bind()
    fun unbind()

    fun resize(width: Int, height: Int)

    fun readPixel(attachmentIndex: Int, x: Int, y: Int): Int
}
