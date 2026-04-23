package org.flux.core.renderer

import org.flux.core.util.Disposable

interface VertexArray : Disposable {

    companion object {
        fun create(): VertexArray = Renderer.createVertexArray()
    }

    val indexBuffer: IndexBuffer?

    fun bind()
    fun unbind()

    fun addVertexBuffer(vertexBuffer: VertexBuffer)
    fun setIndexBuffer(indexBuffer: IndexBuffer)
}
