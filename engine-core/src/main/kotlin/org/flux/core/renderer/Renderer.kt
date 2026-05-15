package org.flux.core.renderer

import org.flux.core.util.Disposable
import org.joml.Matrix4f

object Renderer : Disposable {

    data class SceneData(
        val viewProjMatrix: Matrix4f
    )

    private lateinit var factory: GraphicsFactory

    private var sceneData: SceneData? = null

    fun init(api: RendererAPI, factory: GraphicsFactory) {
        this.factory = factory

        RenderCommand.init(api)
        Renderer2D.init()
        Renderer3D.init()
    }

    fun beginScene(camera: Camera) {
        sceneData = SceneData(camera.viewProjMatrix)
    }

    fun endScene() {
        sceneData = null
    }

    fun submit(
        shader: Shader,
        vertexArray: VertexArray,
        transform: Matrix4f = Matrix4f()
    ) {
        shader.bind()

        sceneData?.let {
            shader.setMat4("u_ViewProjection", it.viewProjMatrix)
        }

        shader.setMat4("u_Transform", transform)

        RenderCommand.drawIndexed(vertexArray)
    }

    fun createVertexBuffer(size: Int) = factory.createVertexBuffer(size)
    fun createVertexBuffer(vertices: FloatArray) = factory.createVertexBuffer(vertices)

    fun createIndexBuffer(indices: IntArray) = factory.createIndexBuffer(indices)

    fun createVertexArray() = factory.createVertexArray()

    fun createShader(
        vertexSrc: String,
        fragmentSrc: String,
        defines: Map<String, Any> = emptyMap()
    ) = factory.createShader(vertexSrc, fragmentSrc, defines)

    fun createShader(
        src: String,
        defines: Map<String, Any> = emptyMap()
    ) = factory.createShader(src, defines)

    fun createTexture2D(
        width: Int,
        height: Int,
        filter: TextureFilter
    ) = factory.createTexture2D(width, height, filter)

    fun createTexture2D(
        path: String,
        filter: TextureFilter
    ) = factory.createTexture2D(path, filter)

    fun createTexture2D(
        bytes: ByteArray,
        filter: TextureFilter
    ) = factory.createTexture2D(bytes, filter)

    fun createFramebuffer(spec: FramebufferSpecification) = factory.createFramebuffer(spec)

    override fun dispose() {
        Renderer2D.dispose()
        Renderer3D.dispose()
    }
}