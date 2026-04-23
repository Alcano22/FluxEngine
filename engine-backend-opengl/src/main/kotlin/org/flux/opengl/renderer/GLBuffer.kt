package org.flux.opengl.renderer

import org.flux.core.renderer.BufferLayout
import org.flux.core.renderer.IndexBuffer
import org.flux.core.renderer.VertexBuffer
import org.lwjgl.opengl.GL46C.*

class GLVertexBuffer : VertexBuffer {

    val rendererId = glCreateBuffers()

    override var layout = BufferLayout()

    constructor(size: Int) {
        glNamedBufferData(rendererId, size.toLong(), GL_DYNAMIC_DRAW)
    }

    constructor(vertices: FloatArray) {
        glNamedBufferData(rendererId, vertices, GL_STATIC_DRAW)
    }

    override fun setData(data: FloatArray) =
        glNamedBufferSubData(rendererId, 0L, data)

    override fun bind() = glBindBuffer(GL_ARRAY_BUFFER, rendererId)
    override fun unbind() = glBindBuffer(GL_ARRAY_BUFFER, 0)

    override fun dispose() = glDeleteBuffers(rendererId)
}

class GLIndexBuffer(indices: IntArray) : IndexBuffer {

    val rendererId = glCreateBuffers()

    override val count = indices.size

    init {
        glNamedBufferData(rendererId, indices, GL_STATIC_DRAW)
    }

    override fun bind() = glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, rendererId)
    override fun unbind() = glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

    override fun dispose() = glDeleteBuffers(rendererId)
}
