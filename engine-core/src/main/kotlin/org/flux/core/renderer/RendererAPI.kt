package org.flux.core.renderer

interface RendererAPI {

    val deviceName: String
    val maxImageUnits: Int

    fun init()

    fun setClearColor(r: Float, g: Float, b: Float, a: Float = 1f)
    fun clear()

    fun setViewport(x: Int, y: Int, width: Int, height: Int)

    fun drawIndexed(vertexArray: VertexArray, indexCount: Int = 0)
}
