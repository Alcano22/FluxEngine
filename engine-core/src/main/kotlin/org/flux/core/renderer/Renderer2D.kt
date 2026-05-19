package org.flux.core.renderer

import org.flux.core.asset.AssetLocation
import org.flux.core.asset.AssetManager
import org.flux.core.scene.AnimationFrame
import org.flux.core.util.Color
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
    private const val MAX_LIGHTS = 16
    private const val VERTEX_SIZE = 11

    private lateinit var quadVertexArray: VertexArray
    private lateinit var quadVertexBuffer: VertexBuffer
    private lateinit var unlitShader: Shader
    private lateinit var litShader: Shader
    private lateinit var entityIdShader: Shader
    private lateinit var activeShader: Shader
    private lateinit var whiteTexture: Texture2D

    private val quadVertexData = FloatArray(MAX_VERTICES * VERTEX_SIZE)
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

        val defines = mapOf(
            "MAX_TEXTURE_SLOTS" to maxTextureSlots
        )

        unlitShader = AssetManager.getShader("shaders/Unlit2D.glsl", AssetLocation.INTERNAL, defines)
        litShader = AssetManager.getShader(
            "shaders/Lit2D.glsl", AssetLocation.INTERNAL,
            defines + mapOf("MAX_POINT_LIGHTS" to MAX_LIGHTS)
        )
        entityIdShader = AssetManager.getShader("shaders/EntityID.glsl", AssetLocation.INTERNAL)

        quadVertexArray = VertexArray.create()

        quadVertexBuffer = VertexBuffer.create(MAX_VERTICES * VERTEX_SIZE * Float.SIZE_BYTES)
        quadVertexBuffer.layout = BufferLayout(
            BufferElement("a_Position", ShaderDataType.Float3),
            BufferElement("a_Color", ShaderDataType.Float4),
            BufferElement("a_TexCoord", ShaderDataType.Float2),
            BufferElement("a_TexIndex", ShaderDataType.Float1),
            BufferElement("a_EntityID", ShaderDataType.Float1)
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
        activeShader = unlitShader
        setupShader(camera)
    }

    fun beginScene(camera: Camera, lights: LightEnvironment) {
        activeShader = litShader
        activeShader.bind()
        activeShader.setMat4("u_ViewProjection", camera.viewProjMatrix)
        activeShader.setIntArray("u_Textures", IntArray(maxTextureSlots) { it })

        activeShader.setFloat3("u_AmbientColor", lights.ambientColor.toVector3f())
        activeShader.setFloat("u_AmbientIntensity", lights.ambientIntensity)

        val lightCount = lights.pointLights.size.coerceAtMost(MAX_LIGHTS)
        activeShader.setInt("u_LightCount", lightCount)

        lights.pointLights.take(MAX_LIGHTS).forEachIndexed { i, light ->
            activeShader.setFloat2("u_Lights[$i].position", light.position)
            activeShader.setFloat3("u_Lights[$i].color", light.color.toVector3f())
            activeShader.setFloat("u_Lights[$i].intensity", light.intensity)
            activeShader.setFloat("u_Lights[$i].radius", light.radius)
        }

        stats.reset()
        startBatch()
    }

    fun beginSceneEntityID(camera: Camera) {
        activeShader = entityIdShader
        activeShader.bind()
        activeShader.setMat4("u_ViewProjection", camera.viewProjMatrix)

        stats.reset()
        startBatch()
    }

    private fun setupShader(camera: Camera) {
        activeShader.bind()
        activeShader.setMat4("u_ViewProjection", camera.viewProjMatrix)
        activeShader.setIntArray("u_Textures", IntArray(maxTextureSlots) { it })

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
        transform: Matrix4fc,
        texture: Texture2D? = null,
        frame: AnimationFrame.SheetFrame? = null,
        color: Color = Color.White,
        entityId: Int = -1
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

        val texCoords = if (frame != null)
            arrayOf(
                Vector2f(frame.u0, frame.v0),
                Vector2f(frame.u1, frame.v0),
                Vector2f(frame.u1, frame.v1),
                Vector2f(frame.u0, frame.v1)
            )
        else quadTexCoords

        repeat(4) { i ->
            val transformedPos = Vector4f(quadVertexPositions[i]).mul(transform)

            quadVertexData[quadVertexPtr++] = transformedPos.x
            quadVertexData[quadVertexPtr++] = transformedPos.y
            quadVertexData[quadVertexPtr++] = transformedPos.z

            quadVertexData[quadVertexPtr++] = color.r
            quadVertexData[quadVertexPtr++] = color.g
            quadVertexData[quadVertexPtr++] = color.b
            quadVertexData[quadVertexPtr++] = color.a

            quadVertexData[quadVertexPtr++] = texCoords[i].x
            quadVertexData[quadVertexPtr++] = texCoords[i].y

            quadVertexData[quadVertexPtr++] = texIndex

            quadVertexData[quadVertexPtr++] = entityId.toFloat()
        }

        quadIndexCount += 6
        stats.quadCount++
    }

    override fun dispose() {
        quadVertexArray.dispose()
        whiteTexture.dispose()
    }
}
