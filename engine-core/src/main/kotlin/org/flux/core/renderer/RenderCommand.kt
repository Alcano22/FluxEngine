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
    fun clear(vararg masks: ClearMask = arrayOf(ClearMask.COLOR, ClearMask.DEPTH)) = api.clear(*masks)

    fun setViewport(x: Int, y: Int, width: Int, height: Int) = api.setViewport(x, y, width, height)

    fun drawIndexed(vertexArray: VertexArray, indexCount: Int = 0) = api.drawIndexed(vertexArray, indexCount)

    fun setStencilTest(enabled: Boolean) = api.setStencilTest(enabled)
    fun setStencilWrite(ref: Int) = api.setStencilWrite(ref)
    fun setStencilDrawWhere(ref: Int) = api.setStencilDrawWhere(ref)
    fun setStencilMaskWrite(enabled: Boolean) = api.setStencilMaskWrite(enabled)
}
