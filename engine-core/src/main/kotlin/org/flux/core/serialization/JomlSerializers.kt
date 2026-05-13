package org.flux.core.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.FloatArraySerializer
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.flux.core.util.toArray
import org.joml.*

object Vector2iSerializer : KSerializer<Vector2i> {

    private val delegateSerializer = IntArraySerializer()

    override val descriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Vector2i) =
        delegateSerializer.serialize(encoder, value.toArray())

    override fun deserialize(decoder: Decoder) =
        Vector2i(delegateSerializer.deserialize(decoder))
}

object Vector3iSerializer : KSerializer<Vector3i> {

    private val delegateSerializer = IntArraySerializer()

    override val descriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Vector3i) =
        delegateSerializer.serialize(encoder, value.toArray())

    override fun deserialize(decoder: Decoder) =
        Vector3i(delegateSerializer.deserialize(decoder))
}

object Vector4iSerializer : KSerializer<Vector4i> {

    private val delegateSerializer = IntArraySerializer()

    override val descriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Vector4i) =
        delegateSerializer.serialize(encoder, value.toArray())

    override fun deserialize(decoder: Decoder) =
        Vector4i(delegateSerializer.deserialize(decoder))
}

object Vector2fSerializer : KSerializer<Vector2f> {

    private val delegateSerializer = FloatArraySerializer()

    override val descriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Vector2f) =
        delegateSerializer.serialize(encoder, value.toArray())

    override fun deserialize(decoder: Decoder) =
        Vector2f(delegateSerializer.deserialize(decoder))
}

object Vector3fSerializer : KSerializer<Vector3f> {

    private val delegateSerializer = FloatArraySerializer()

    override val descriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Vector3f) =
        delegateSerializer.serialize(encoder, value.toArray())

    override fun deserialize(decoder: Decoder) =
        Vector3f(delegateSerializer.deserialize(decoder))
}

object Vector4fSerializer : KSerializer<Vector4f> {

    private val delegateSerializer = FloatArraySerializer()

    override val descriptor = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: Vector4f) =
        delegateSerializer.serialize(encoder, value.toArray())

    override fun deserialize(decoder: Decoder) =
        Vector4f(delegateSerializer.deserialize(decoder))
}
