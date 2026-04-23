package org.flux.core.renderer

object RenderCommand {

    private lateinit var api: RendererAPI

    val deviceName get() = api.deviceName
    val maxImageUnits get() = api.maxImageUnits

    fun init(api: RendererAPI) {
        this.api = api
        this.api.init()
    }

    fun setClearColor(r: Float, g: Float, b: Float, a: Float = 1f) = api.setClearColor(r, g, b, a)
    fun clear() = api.clear()

    fun setViewport(x: Int, y: Int, width: Int, height: Int) = api.setViewport(x, y, width, height)

    fun drawIndexed(vertexArray: VertexArray, indexCount: Int = 0) = api.drawIndexed(vertexArray, indexCount)
}
