package org.flux.core.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.flux.core.asset.AssetHandle

class AssetHandleSerializer<T>(
    @Suppress("UNUSED_PARAMETER") dataSerializer: KSerializer<T>
) : KSerializer<AssetHandle<T>> {

    override val descriptor = PrimitiveSerialDescriptor("AssetHandle", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AssetHandle<T>) =
        encoder.encodeString(value.path)

    override fun deserialize(decoder: Decoder) = AssetHandle<T>(decoder.decodeString())
}
