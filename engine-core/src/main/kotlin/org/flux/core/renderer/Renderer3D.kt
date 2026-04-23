package org.flux.core.renderer

import org.flux.core.asset.AssetLocation
import org.flux.core.asset.AssetManager
import org.flux.core.util.Disposable
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import org.joml.Vector4fc

object Renderer3D : Disposable {

    class Statistics {
        var drawCalls = 0
        var cubeCount = 0

        val totalVertexCount get() = cubeCount * 24
        val totalIndexCount get() = cubeCount * 36

        fun reset() {
            drawCalls = 0
            cubeCount = 0
        }
    }

    private const val MAX_CUBES = 10_000
    private const val MAX_VERTICES = MAX_CUBES * 24
    private const val MAX_INDICES = MAX_CUBES * 36

    private lateinit var cubeVertexArray: VertexArray
    private lateinit var cubeVertexBuffer: VertexBuffer
    private lateinit var textureShader: Shader
    private lateinit var whiteTexture: Texture2D

    private val cubeVertexData = FloatArray(MAX_VERTICES * 10)
    private var cubeVertexPtr = 0
    private var cubeIndexCount = 0

    private lateinit var textureSlots: Array<Texture2D?>
    private var textureSlotIndex = 1
    private var maxTextureSlots = 0

    private val transformMatrix = Matrix4f()
    private val tmpVertexPos = Vector4f()

    private val cubeVertexPositions = arrayOf(
        // Front
        Vector4f(-0.5f, -0.5f,  0.5f, 1.0f), Vector4f( 0.5f, -0.5f,  0.5f, 1.0f),
        Vector4f( 0.5f,  0.5f,  0.5f, 1.0f), Vector4f(-0.5f,  0.5f,  0.5f, 1.0f),
        // Back
        Vector4f( 0.5f, -0.5f, -0.5f, 1.0f), Vector4f(-0.5f, -0.5f, -0.5f, 1.0f),
        Vector4f(-0.5f,  0.5f, -0.5f, 1.0f), Vector4f( 0.5f,  0.5f, -0.5f, 1.0f),
        // Left
        Vector4f(-0.5f, -0.5f, -0.5f, 1.0f), Vector4f(-0.5f, -0.5f,  0.5f, 1.0f),
        Vector4f(-0.5f,  0.5f,  0.5f, 1.0f), Vector4f(-0.5f,  0.5f, -0.5f, 1.0f),
        // Right
        Vector4f( 0.5f, -0.5f,  0.5f, 1.0f), Vector4f( 0.5f, -0.5f, -0.5f, 1.0f),
        Vector4f( 0.5f,  0.5f, -0.5f, 1.0f), Vector4f( 0.5f,  0.5f,  0.5f, 1.0f),
        // Top
        Vector4f(-0.5f,  0.5f,  0.5f, 1.0f), Vector4f( 0.5f,  0.5f,  0.5f, 1.0f),
        Vector4f( 0.5f,  0.5f, -0.5f, 1.0f), Vector4f(-0.5f,  0.5f, -0.5f, 1.0f),
        // Bottom
        Vector4f(-0.5f, -0.5f, -0.5f, 1.0f), Vector4f( 0.5f, -0.5f, -0.5f, 1.0f),
        Vector4f( 0.5f, -0.5f,  0.5f, 1.0f), Vector4f(-0.5f, -0.5f,  0.5f, 1.0f)
    )

    private val cubeTexCoords = Array(24) { i ->
        when (i % 4) {
            0    -> Vector2f(0f, 0f)
            1    -> Vector2f(1f, 0f)
            2    -> Vector2f(1f, 1f)
            else -> Vector2f(0f, 1f)
        }
    }

    val stats = Statistics()

    fun init() {
        maxTextureSlots = RenderCommand.maxImageUnits.coerceAtMost(32)
        textureSlots = Array(maxTextureSlots) { null }

        textureShader = AssetManager.getShader(
            "shaders/Batch3D.glsl",
            AssetLocation.INTERNAL,
            mapOf(
                "MAX_TEXTURE_SLOTS" to maxTextureSlots
            )
        )

        cubeVertexArray = VertexArray.create()

        cubeVertexBuffer = VertexBuffer.create(MAX_VERTICES * 10 * Float.SIZE_BYTES)
        cubeVertexBuffer.layout = BufferLayout(
            BufferElement("a_Position", ShaderDataType.Float3),
            BufferElement("a_Color", ShaderDataType.Float4),
            BufferElement("a_TexCoord", ShaderDataType.Float2),
            BufferElement("a_TexIndex", ShaderDataType.Float1)
        )
        cubeVertexArray.addVertexBuffer(cubeVertexBuffer)

        val indices = IntArray(MAX_INDICES)
        var offset = 0
        for (i in 0 until MAX_INDICES step 6) {
            indices[i + 0] = offset + 0
            indices[i + 1] = offset + 1
            indices[i + 2] = offset + 2
            indices[i + 3] = offset + 2
            indices[i + 4] = offset + 3
            indices[i + 5] = offset + 0
            offset += 4
        }
        cubeVertexArray.setIndexBuffer(IndexBuffer.create(indices))

        whiteTexture = Texture2D.create(1, 1, filter = TextureFilter.NEAREST)
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
        cubeIndexCount = 0
        cubeVertexPtr = 0
        textureSlotIndex = 1
    }

    fun endScene() {
        flush()
    }

    private fun flush() {
        if (cubeIndexCount == 0) return

        repeat(textureSlotIndex) { i ->
            textureSlots[i]?.bind(i)
        }

        cubeVertexBuffer.setData(cubeVertexData.copyOfRange(0, cubeVertexPtr))
        RenderCommand.drawIndexed(cubeVertexArray, cubeIndexCount)
        stats.drawCalls++
    }

    fun drawCube(
        position: Vector3fc,
        rotation: Vector3fc = Vector3f(0f),
        size: Vector3fc = Vector3f(1f),
        texture: Texture2D? = null,
        color: Vector4fc = Vector4f(1f)
    ) {
        if (cubeIndexCount >= MAX_INDICES || textureSlotIndex >= maxTextureSlots) {
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

        transformMatrix.identity()
            .translate(position)
            .rotateXYZ(rotation)
            .scale(size)

        repeat(24) { i ->
            tmpVertexPos.set(cubeVertexPositions[i]).mul(transformMatrix)

            cubeVertexData[cubeVertexPtr++] = tmpVertexPos.x
            cubeVertexData[cubeVertexPtr++] = tmpVertexPos.y
            cubeVertexData[cubeVertexPtr++] = tmpVertexPos.z

            cubeVertexData[cubeVertexPtr++] = color.x()
            cubeVertexData[cubeVertexPtr++] = color.y()
            cubeVertexData[cubeVertexPtr++] = color.z()
            cubeVertexData[cubeVertexPtr++] = color.w()

            cubeVertexData[cubeVertexPtr++] = cubeTexCoords[i].x
            cubeVertexData[cubeVertexPtr++] = cubeTexCoords[i].y

            cubeVertexData[cubeVertexPtr++] = texIndex
        }

        cubeIndexCount += 36
        stats.cubeCount++
    }

    override fun dispose() {
        cubeVertexArray.dispose()
        whiteTexture.dispose()
    }
}
