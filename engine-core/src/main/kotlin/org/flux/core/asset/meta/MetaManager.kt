package org.flux.core.asset.meta

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object MetaManager {

    @PublishedApi
    internal val format = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    inline fun <reified T : AssetMeta> load(assetPath: String): T? {
        val file = File("$assetPath.meta")
        if (!file.exists())
            return null

        return runCatching {
            format.decodeFromString<T>(file.readText())
        }.getOrNull()
    }

    fun save(assetPath: String, meta: AssetMeta) {
        File("$assetPath.meta").writeText(
            format.encodeToString(meta)
        )
    }

    inline fun <reified T : AssetMeta> getOrCreate(
        assetPath: String,
        default: T
    ): T = load(assetPath) ?: default.also { save(assetPath, it) }
}
