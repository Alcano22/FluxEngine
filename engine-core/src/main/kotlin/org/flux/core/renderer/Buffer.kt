package org.flux.core.renderer

import org.flux.core.util.Disposable

interface VertexBuffer : Disposable {

    companion object {
        fun create(size: Int) = Renderer.createVertexBuffer(size)

        fun create(vertices: FloatArray) = Renderer.createVertexBuffer(vertices)
    }

    var layout: BufferLayout

    fun setData(data: FloatArray)

    fun bind()
    fun unbind()
}

interface IndexBuffer : Disposable {

    companion object {
        fun create(indices: IntArray) = Renderer.createIndexBuffer(indices)
    }

    val count: Int

    fun bind()
    fun unbind()
}
