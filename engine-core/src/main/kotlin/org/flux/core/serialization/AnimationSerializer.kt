package org.flux.core.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.flux.core.asset.AnimationAsset

object AnimationSerializer {

    val format = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    fun serialize(asset: AnimationAsset) = format.encodeToString(asset)

    fun deserialize(json: String) = format.decodeFromString<AnimationAsset>(json)
}
