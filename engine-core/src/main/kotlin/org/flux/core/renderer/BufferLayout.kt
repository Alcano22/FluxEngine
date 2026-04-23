package org.flux.core.renderer

class BufferElement(
    val name: String,
    val type: ShaderDataType,
    val normalized: Boolean = false
) {
    val size = type.size
    val componentCount = type.componentCount
    var offset = 0
}

class BufferLayout(vararg elements: BufferElement) {

    val elements = elements.toList()

    var stride = 0
        private set

    init {
        calculateOffsetsAndStride()
    }

    private fun calculateOffsetsAndStride() {
        var offset = 0
        for (element in elements) {
            element.offset = offset
            offset += element.size
            stride += element.size
        }
    }

}
