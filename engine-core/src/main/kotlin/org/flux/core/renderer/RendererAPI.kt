package org.flux.core.renderer

enum class ClearMask {
    COLOR, DEPTH, STENCIL
}

interface RendererAPI {

    val deviceName: String
    val maxImageUnits: Int

    fun init()

    fun setClearColor(r: Float, g: Float, b: Float, a: Float = 1f)
    fun clear(vararg masks: ClearMask = arrayOf(ClearMask.COLOR, ClearMask.DEPTH))

    fun setViewport(x: Int, y: Int, width: Int, height: Int)

    fun drawIndexed(vertexArray: VertexArray, indexCount: Int = 0)

    fun setStencilTest(enabled: Boolean)
    fun setStencilWrite(ref: Int)
    fun setStencilDrawWhere(ref: Int)
    fun setStencilMaskWrite(enabled: Boolean)
}
