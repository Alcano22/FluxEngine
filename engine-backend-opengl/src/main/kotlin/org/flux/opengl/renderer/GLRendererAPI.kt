package org.flux.opengl.renderer

import org.flux.core.renderer.ClearMask
import org.flux.core.renderer.RendererAPI
import org.flux.core.renderer.VertexArray
import org.flux.core.logging.logger
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL46C.*

class GLRendererAPI : RendererAPI {

    companion object {
        private val logger = logger()
    }

    override val deviceName get() = glGetString(GL_RENDERER) ?: "Unknown"

    override val maxImageUnits get() = glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS)

    override fun init() {
        GL.createCapabilities()

        logger.info {
            "OpenGL renderer initialized (Vendor: ${glGetString(GL_VENDOR)}, " +
                    "Renderer: ${glGetString(GL_RENDERER)}, " +
                    "Version: ${glGetString(GL_VERSION)})"
        }

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glEnable(GL_DEPTH_TEST)

        glDisable(GL_CULL_FACE)
    }

    override fun setClearColor(r: Float, g: Float, b: Float, a: Float) = glClearColor(r, g, b, a)

    override fun clear(vararg masks: ClearMask) {
        var bits = 0
        masks.forEach { bits = bits or it.toGLBit() }
        glClear(bits)
    }

    override fun setViewport(x: Int, y: Int, width: Int, height: Int) = glViewport(x, y, width, height)

    override fun drawIndexed(vertexArray: VertexArray, indexCount: Int) {
        vertexArray.bind()

        val count = if (indexCount > 0)
            indexCount
        else
            vertexArray.indexBuffer?.count ?: 0
        glDrawElements(GL_TRIANGLES, count, GL_UNSIGNED_INT, 0)
    }

    override fun setStencilTest(enabled: Boolean) {
        if (enabled)
            glEnable(GL_STENCIL_TEST)
        else
            glDisable(GL_STENCIL_TEST)
    }

    override fun setStencilWrite(ref: Int) {
        glStencilFunc(GL_ALWAYS, ref, 0xFF)
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
        glStencilMask(0xFF)
    }

    override fun setStencilDrawWhere(ref: Int) {
        glStencilFunc(GL_NOTEQUAL, ref, 0xFF)
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
        glStencilMask(0x00)
    }

    override fun setStencilMaskWrite(enabled: Boolean) {
        glStencilMask(if (enabled) 0xFF else 0x00)
    }

    private fun ClearMask.toGLBit() = when (this) {
        ClearMask.COLOR   -> GL_COLOR_BUFFER_BIT
        ClearMask.DEPTH   -> GL_DEPTH_BUFFER_BIT
        ClearMask.STENCIL -> GL_STENCIL_BUFFER_BIT
    }
}