package org.flux.core.asset

import org.flux.core.renderer.Shader
import org.flux.core.renderer.Texture2D
import org.flux.core.renderer.TextureFilter
import org.flux.core.util.Disposable
import java.io.File

enum class AssetLocation {
    INTERNAL,
    EXTERNAL
}

object AssetManager : Disposable {

    private val textCache = mutableMapOf<String, String>()
    private val byteCache = mutableMapOf<String, ByteArray>()
    private val shaderCache = mutableMapOf<String, Shader>()
    private val textureCache = mutableMapOf<String, Texture2D>()

    fun readBytes(path: String, location: AssetLocation = AssetLocation.EXTERNAL): ByteArray {
        val key = "${location.name}:$path"
        return byteCache.getOrPut(key) {
            when (location) {
                AssetLocation.INTERNAL -> {
                    val stream = AssetManager::class.java.classLoader.getResourceAsStream(path)
                        ?: throw IllegalArgumentException("Internal asset not found: $path")
                    stream.use { it.readAllBytes() }
                }
                AssetLocation.EXTERNAL -> {
                    val file = File(path)
                    require(file.exists()) { "External asset not found: $path" }
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
                        ?: throw IllegalArgumentException("Internal asset not found: $path")
                    stream.bufferedReader().use { it.readText() }
                }
                AssetLocation.EXTERNAL -> {
                    val file = File(path)
                    require(file.exists()) { "External asset not found: $path" }
                    file.readText()
                }
            }
        }
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
        filter: TextureFilter = TextureFilter.LINEAR
    ): Texture2D {
        val key = "${location.name}:$path:${filter.name}"
        return textureCache.getOrPut(key) {
            when (location) {
                AssetLocation.INTERNAL -> {
                    throw NotImplementedError("Internal textures are not supported yet")
                }
                AssetLocation.EXTERNAL -> {
                    val file = File(path)
                    require(file.exists()) { "Texture not found: $path" }

                    Texture2D.create(path, filter)
                }
            }
        }
    }

    fun getFont(path: String, location: AssetLocation = AssetLocation.EXTERNAL) = readBytes(path, location)

    override fun dispose() {
        textCache.clear()
        byteCache.clear()

        shaderCache.values.forEach { it.dispose() }
        shaderCache.clear()

        textureCache.values.forEach { it.dispose() }
        textureCache.clear()
    }
}
