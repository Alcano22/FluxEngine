package org.flux.opengl.renderer

import org.flux.core.renderer.IndexBuffer
import org.flux.core.renderer.ShaderDataType
import org.flux.core.renderer.VertexArray
import org.flux.core.renderer.VertexBuffer
import org.flux.core.logging.logger
import org.flux.core.logging.require
import org.lwjgl.opengl.GL46C.*

class GLVertexArray : VertexArray {

    companion object {
        private val logger = logger()
    }

    val rendererId = glCreateVertexArrays()

    private val vertexBuffers = mutableListOf<VertexBuffer>()
    private var _indexBuffer: IndexBuffer? = null

    override val indexBuffer get() = _indexBuffer

    private var attribIndex = 0

    override fun bind() = glBindVertexArray(rendererId)
    override fun unbind() = glBindVertexArray(0)

    override fun addVertexBuffer(vertexBuffer: VertexBuffer) {
        logger.require(vertexBuffer.layout.elements.isNotEmpty()) { "VertexBuffer has no layout" }

        val bindingIndex = vertexBuffers.size
        glVertexArrayVertexBuffer(
            rendererId,
            bindingIndex,
            (vertexBuffer as GLVertexBuffer).rendererId,
            0,
            vertexBuffer.layout.stride
        )

        for (element in vertexBuffer.layout.elements) {
            glEnableVertexArrayAttrib(rendererId, attribIndex)
            glVertexArrayAttribFormat(
                rendererId,
                attribIndex,
                element.componentCount,
                element.type.toGLBaseType(),
                element.normalized,
                element.offset
            )
            glVertexArrayAttribBinding(rendererId, attribIndex, bindingIndex)
            attribIndex++
        }

        vertexBuffers.add(vertexBuffer)
    }

    override fun setIndexBuffer(indexBuffer: IndexBuffer) {
        glVertexArrayElementBuffer(
            rendererId,
            (indexBuffer as GLIndexBuffer).rendererId
        )
        _indexBuffer = indexBuffer
    }

    private fun ShaderDataType.toGLBaseType(): Int = when (this) {
        ShaderDataType.None -> GL_NONE
        ShaderDataType.Bool -> GL_BOOL
        ShaderDataType.Int1,
        ShaderDataType.Int2,
        ShaderDataType.Int3,
        ShaderDataType.Int4 -> GL_INT
        ShaderDataType.Float1,
        ShaderDataType.Float2,
        ShaderDataType.Float3,
        ShaderDataType.Float4,
        ShaderDataType.Mat2,
        ShaderDataType.Mat3,
        ShaderDataType.Mat4 -> GL_FLOAT
    }

    override fun dispose() {
        vertexBuffers.forEach { it.dispose() }
        indexBuffer?.dispose()
        glDeleteVertexArrays(rendererId)
    }
}
