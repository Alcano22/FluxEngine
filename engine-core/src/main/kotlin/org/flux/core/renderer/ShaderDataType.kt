package org.flux.core.renderer

enum class ShaderDataType(
    val componentSize: Int,
    val componentCount: Int
) {
    None(0, 0),
    Bool(1, 1),
    Int1(Int.SIZE_BYTES, 1),
    Int2(Int.SIZE_BYTES, 2),
    Int3(Int.SIZE_BYTES, 3),
    Int4(Int.SIZE_BYTES, 4),
    Float1(Float.SIZE_BYTES, 1),
    Float2(Float.SIZE_BYTES, 2),
    Float3(Float.SIZE_BYTES, 3),
    Float4(Float.SIZE_BYTES, 4),
    Mat2(Float.SIZE_BYTES, 2 * 2),
    Mat3(Float.SIZE_BYTES, 3 * 3),
    Mat4(Float.SIZE_BYTES, 4 * 4);

    val size = componentSize * componentCount
}
