package org.flux.core.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.flux.core.asset.AssetData

object AssetSerializer {

    val format = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        classDiscriminator = "type"

        serializersModule = SerializersModule {
            contextual(AssetHandleSerializer::class) { args -> AssetHandleSerializer(args[0]) }
        }
    }

    fun serialize(asset: AssetData) = format.encodeToString(asset)

    fun deserialize(json: String) = format.decodeFromString<AssetData>(json)
}
