package org.flux.opengl.renderer

import org.flux.core.renderer.RendererAPI
import org.flux.core.renderer.VertexArray
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL46C.*

class GLRendererAPI : RendererAPI {

    override val deviceName get() = glGetString(GL_RENDERER) ?: "Unknown"

    override val maxImageUnits get() = glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS)

    override fun init() {
        GL.createCapabilities()

        println("OpenGL renderer initialized")
        println("  Vendor:        ${glGetString(GL_VENDOR)}")
        println("  Renderer:      ${glGetString(GL_RENDERER)}")
        println("  Version:       ${glGetString(GL_VERSION)}")

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glEnable(GL_DEPTH_TEST)

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glFrontFace(GL_CCW)
    }

    override fun setClearColor(r: Float, g: Float, b: Float, a: Float) = glClearColor(r, g, b, a)
    override fun clear() = glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

    override fun setViewport(x: Int, y: Int, width: Int, height: Int) = glViewport(x, y, width, height)

    override fun drawIndexed(vertexArray: VertexArray, indexCount: Int) {
        vertexArray.bind()

        val count = if (indexCount > 0)
            indexCount
        else
            vertexArray.indexBuffer?.count ?: 0
        glDrawElements(GL_TRIANGLES, count, GL_UNSIGNED_INT, 0)
    }
}