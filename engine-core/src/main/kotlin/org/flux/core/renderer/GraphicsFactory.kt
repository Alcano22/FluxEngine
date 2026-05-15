package org.flux.core.renderer

interface GraphicsFactory {

    fun createVertexBuffer(size: Int): VertexBuffer
    fun createVertexBuffer(vertices: FloatArray): VertexBuffer

    fun createIndexBuffer(indices: IntArray): IndexBuffer

    fun createVertexArray(): VertexArray

    fun createShader(vertexSrc: String, fragmentSrc: String, defines: Map<String, Any>): Shader
    fun createShader(src: String, defines: Map<String, Any>): Shader

    fun createTexture2D(width: Int, height: Int, filter: TextureFilter): Texture2D
    fun createTexture2D(path: String, filter: TextureFilter): Texture2D
    fun createTexture2D(bytes: ByteArray, filter: TextureFilter): Texture2D

    fun createFramebuffer(spec: FramebufferSpecification): Framebuffer
}
