package org.flux.core.renderer

import org.flux.core.asset.AssetLocation
import org.flux.core.asset.AssetManager
import org.flux.core.util.Disposable
import org.joml.*

object Renderer2D : Disposable {

    class Statistics {
        var drawCalls = 0
        var quadCount = 0

        val totalVertexCount get() = quadCount * 4
        val totalIndexCount get() = quadCount * 6

        fun reset() {
            drawCalls = 0
            quadCount = 0
        }
    }

    private const val MAX_QUADS = 10_000
    private const val MAX_VERTICES = MAX_QUADS * 4
    private const val MAX_INDICES = MAX_QUADS * 6

    private lateinit var quadVertexArray: VertexArray
    private lateinit var quadVertexBuffer: VertexBuffer
    private lateinit var textureShader: Shader
    private lateinit var whiteTexture: Texture2D

    private val quadVertexData = FloatArray(MAX_VERTICES * 10)
    private var quadVertexPtr = 0
    private var quadIndexCount = 0

    private var maxTextureSlots = 0
    private lateinit var textureSlots: Array<Texture2D?>
    private var textureSlotIndex = 1

    private val quadVertexPositions = arrayOf(
        Vector4f(-0.5f, -0.5f, 0.0f, 1.0f),
        Vector4f( 0.5f, -0.5f, 0.0f, 1.0f),
        Vector4f( 0.5f,  0.5f, 0.0f, 1.0f),
        Vector4f(-0.5f,  0.5f, 0.0f, 1.0f)
    )

    private val quadTexCoords = arrayOf(
        Vector2f(0f, 0f),
        Vector2f(1f, 0f),
        Vector2f(1f, 1f),
        Vector2f(0f, 1f)
    )

    val stats = Statistics()

    fun init() {
        maxTextureSlots = RenderCommand.maxImageUnits.coerceAtMost(32)
        textureSlots = Array(maxTextureSlots) { null }

        textureShader = AssetManager.getShader(
            "shaders/Batch2D.glsl",
            AssetLocation.INTERNAL,
            mapOf(
                "MAX_TEXTURE_SLOTS" to maxTextureSlots
            )
        )

        quadVertexArray = VertexArray.create()

        quadVertexBuffer = VertexBuffer.create(MAX_VERTICES * 10 * Float.SIZE_BYTES)
        quadVertexBuffer.layout = BufferLayout(
            BufferElement("a_Position", ShaderDataType.Float3),
            BufferElement("a_Color", ShaderDataType.Float4),
            BufferElement("a_TexCoord", ShaderDataType.Float2),
            BufferElement("a_TexIndex", ShaderDataType.Float1)
        )
        quadVertexArray.addVertexBuffer(quadVertexBuffer)

        val quadIndices = IntArray(MAX_INDICES)
        var offset = 0
        for (i in 0 until MAX_INDICES step 6) {
            quadIndices[i + 0] = offset + 0
            quadIndices[i + 1] = offset + 1
            quadIndices[i + 2] = offset + 2
            quadIndices[i + 3] = offset + 2
            quadIndices[i + 4] = offset + 3
            quadIndices[i + 5] = offset + 0
            offset += 4
        }
        quadVertexArray.setIndexBuffer(IndexBuffer.create(quadIndices))

        whiteTexture = Texture2D.create(1, 1)
        whiteTexture.setData(byteArrayOf(-1, -1, -1, -1))
        textureSlots[0] = whiteTexture
    }

    fun beginScene(camera: Camera) {
        textureShader.bind()
        textureShader.setMat4("u_ViewProjection", camera.viewProjMatrix)

        val samplers = IntArray(maxTextureSlots) { it }
        textureShader.setIntArray("u_Textures", samplers)

        stats.reset()
        startBatch()
    }

    private fun startBatch() {
        quadIndexCount = 0
        quadVertexPtr = 0
        textureSlotIndex = 1
    }

    fun endScene() {
        flush()
    }

    private fun flush() {
        if (quadIndexCount == 0) return

        repeat(textureSlotIndex) { i ->
            textureSlots[i]?.bind(i)
        }

        quadVertexBuffer.setData(quadVertexData.copyOfRange(0, quadVertexPtr))
        RenderCommand.drawIndexed(quadVertexArray, quadIndexCount)
        stats.drawCalls++
    }

    fun drawQuad(
        position: Vector2fc,
        rotation: Float,
        size: Vector2fc,
        color: Vector4fc
    ) = drawQuad(Vector3f(position, 0f), rotation, size, null, color)

    fun drawQuad(
        position: Vector3fc,
        rotation: Float,
        size: Vector2fc,
        color: Vector4fc
    ) = drawQuad(position, rotation, size, null, color)

    fun drawQuad(
        position: Vector2fc,
        rotation: Float,
        size: Vector2fc,
        texture: Texture2D,
        color: Vector4fc = Vector4f(1f)
    ) = drawQuad(Vector3f(position, 0f), rotation, size, texture, color)

    fun drawQuad(
        position: Vector3fc,
        rotation: Float,
        size: Vector2fc,
        texture: Texture2D? = null,
        color: Vector4fc = Vector4f(1f)
    ) {
        val transform = Matrix4f()
            .translate(position)
            .rotateZ(rotation)
            .scaleXY(size.x(), size.y())

        drawQuad(transform, texture, color)
    }

    fun drawQuad(
        transform: Matrix4f,
        texture: Texture2D? = null,
        color: Vector4fc = Vector4f(1f)
    ) {
        if (quadIndexCount >= MAX_INDICES || textureSlotIndex >= maxTextureSlots) {
            flush()
            startBatch()
        }

        var texIndex = 0f
        if (texture != null) {
            for (i in 1 until textureSlotIndex) {
                if (textureSlots[i] == texture) {
                    texIndex = i.toFloat()
                    break
                }
            }

            if (texIndex == 0f) {
                texIndex = textureSlotIndex.toFloat()
                textureSlots[textureSlotIndex] = texture
                textureSlotIndex++
            }
        }

        repeat(4) { i ->
            val transformedPos = Vector4f(quadVertexPositions[i]).mul(transform)

            quadVertexData[quadVertexPtr++] = transformedPos.x
            quadVertexData[quadVertexPtr++] = transformedPos.y
            quadVertexData[quadVertexPtr++] = transformedPos.z

            quadVertexData[quadVertexPtr++] = color.x()
            quadVertexData[quadVertexPtr++] = color.y()
            quadVertexData[quadVertexPtr++] = color.z()
            quadVertexData[quadVertexPtr++] = color.w()

            quadVertexData[quadVertexPtr++] = quadTexCoords[i].x
            quadVertexData[quadVertexPtr++] = quadTexCoords[i].y

            quadVertexData[quadVertexPtr++] = texIndex
        }

        quadIndexCount += 6
        stats.quadCount++
    }

    override fun dispose() {
        quadVertexArray.dispose()
        whiteTexture.dispose()
    }
}
