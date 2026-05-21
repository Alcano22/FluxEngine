package org.flux.core.asset

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.flux.core.asset.meta.ImageMeta
import org.flux.core.asset.meta.MetaManager
import org.flux.core.renderer.Shader
import org.flux.core.renderer.Texture2D
import org.flux.core.util.Disposable
import org.flux.core.logging.logger
import org.flux.core.logging.require
import org.flux.core.renderer.TextureParams
import org.flux.core.serialization.AssetSerializer
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class AssetLocation {
    INTERNAL,
    EXTERNAL
}

object AssetManager : Disposable {

    private val logger = logger()

    private val textCache      = ConcurrentHashMap<String, String>()
    private val byteCache      = ConcurrentHashMap<String, ByteArray>()
    private val shaderCache    = ConcurrentHashMap<String, Shader>()
    private val textureCache   = ConcurrentHashMap<String, Texture2D>()
    @PublishedApi
    internal val assetCache    = ConcurrentHashMap<String, AssetData>()
    private val assetTypeCache = ConcurrentHashMap<String, String>()

    fun readBytes(path: String, location: AssetLocation = AssetLocation.EXTERNAL): ByteArray {
        val key = "${location.name}:$path"
        return byteCache.getOrPut(key) {
            when (location) {
                AssetLocation.INTERNAL -> {
                    val stream = AssetManager::class.java.classLoader.getResourceAsStream(path)
                        ?: throw logger.throwing(
                            IllegalArgumentException("Internal asset not found: $path")
                        )
                    stream.use { it.readAllBytes() }
                }
                AssetLocation.EXTERNAL -> {
                    val file = File(path)
                    logger.require(file.exists()) { "External asset not found: $path" }
                    file.readBytes()
                }
            }
        }
    }

    fun readText(path: String, location: AssetLocation = AssetLocation.EXTERNAL): String {
        val key = "${location.name}:$path"
        return textCache.getOrPut(key) {
            when (location) {
                AssetLocation.INTERNAL -> {
                    val stream = AssetManager::class.java.classLoader.getResourceAsStream(path)
                        ?: throw logger.throwing(
                            IllegalArgumentException("Internal asset not found: $path")
                        )
                    stream.bufferedReader().use { it.readText() }
                }
                AssetLocation.EXTERNAL -> {
                    val file = File(path)
                    logger.require(file.exists()) { "External asset not found: $path" }
                    file.readText()
                }
            }
        }
    }

    fun invalidateText(path: String) {
        textCache.remove("EXTERNAL:$path")
    }

    fun getShader(
        path: String,
        location: AssetLocation = AssetLocation.EXTERNAL,
        defines: Map<String, Any> = emptyMap()
    ): Shader {
        val key = "${location.name}:$path:$defines"
        return shaderCache.getOrPut(key) {
            val src = readText(path, location)
            Shader.create(src, defines)
        }
    }

    fun getTexture(
        path: String,
        location: AssetLocation = AssetLocation.EXTERNAL,
        params: TextureParams? = null
    ): Texture2D {
        val resolvedParams = params ?: run {
            if (location == AssetLocation.EXTERNAL) {
                val meta = MetaManager.getOrCreate(path, ImageMeta())
                TextureParams(
                    minFilter       = meta.minFilter,
                    magFilter       = meta.magFilter,
                    wrapS           = meta.wrapS,
                    wrapT           = meta.wrapT,
                    generateMipmaps = meta.generateMipmaps
                )
            } else TextureParams()
        }

        val key = "${location.name}:$path:$resolvedParams"
        return textureCache.getOrPut(key) {
            when (location) {
                AssetLocation.INTERNAL -> {
                    val bytes = readBytes(path, location)
                    Texture2D.create(bytes, resolvedParams)
                }
                AssetLocation.EXTERNAL -> {
                    val file = File(path)
                    logger.require(file.exists()) { "Texture not found: $path" }
                    Texture2D.create(path, resolvedParams)
                }
            }
        }
    }

    fun invalidateTexture(path: String) {
        textureCache.remove("EXTERNAL:$path")
    }

    fun getAssetType(path: String): String? =
        assetTypeCache.getOrPut(path) {
            runCatching {
                Json.parseToJsonElement(File(path).readText())
                    .jsonObject["type"]
                    ?.jsonPrimitive
                    ?.content
                    ?: return null
            }.getOrNull() ?: return null
        }

    inline fun <reified T : AssetData> getAsset(path: String): T {
        val cached = assetCache.getOrPut(path) {
            val json = readText(path)
            AssetSerializer.deserialize(json)
        }
        return cached as? T
            ?: error("Asset at '$path' is ${cached::class.simpleName}, expected ${T::class.simpleName}")
    }

    fun invalidateAsset(path: String) {
        assetCache.remove(path)
        invalidateText(path)
    }

    fun getFont(path: String, location: AssetLocation = AssetLocation.EXTERNAL) = readBytes(path, location)

    override fun dispose() {
        textCache.clear()
        byteCache.clear()

        shaderCache.values.forEach { it.dispose() }
        shaderCache.clear()

        textureCache.values.forEach { it.dispose() }
        textureCache.clear()

        assetCache.clear()
        assetTypeCache.clear()
    }
}
