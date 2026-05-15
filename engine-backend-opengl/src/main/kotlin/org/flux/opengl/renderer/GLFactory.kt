package org.flux.opengl.renderer

import org.flux.core.renderer.Framebuffer
import org.flux.core.renderer.FramebufferSpecification
import org.flux.core.renderer.GraphicsFactory
import org.flux.core.renderer.IndexBuffer
import org.flux.core.renderer.Shader
import org.flux.core.renderer.Texture2D
import org.flux.core.renderer.TextureFilter
import org.flux.core.renderer.VertexArray
import org.flux.core.renderer.VertexBuffer

class GLFactory : GraphicsFactory {

    override fun createVertexBuffer(size: Int): VertexBuffer = GLVertexBuffer(size)
    override fun createVertexBuffer(vertices: FloatArray): VertexBuffer = GLVertexBuffer(vertices)

    override fun createIndexBuffer(indices: IntArray): IndexBuffer = GLIndexBuffer(indices)

    override fun createVertexArray(): VertexArray = GLVertexArray()

    override fun createShader(
        vertexSrc: String,
        fragmentSrc: String,
        defines: Map<String, Any>
    ): Shader = GLShader(vertexSrc, fragmentSrc, defines)

    override fun createShader(
        src: String,
        defines: Map<String, Any>
    ): Shader = GLShader(src, defines)

    override fun createTexture2D(
        width: Int,
        height: Int,
        filter: TextureFilter
    ): Texture2D = GLTexture2D(width, height, filter)

    override fun createTexture2D(
        path: String,
        filter: TextureFilter
    ): Texture2D = GLTexture2D(path, filter)

    override fun createTexture2D(
        bytes: ByteArray,
        filter: TextureFilter
    ): Texture2D = GLTexture2D(bytes, filter)

    override fun createFramebuffer(spec: FramebufferSpecification): Framebuffer = GLFramebuffer(spec)
}
