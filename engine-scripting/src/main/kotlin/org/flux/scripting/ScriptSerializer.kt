package org.flux.scripting

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.flux.scripting.loader.ScriptLoader
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

object ScriptSerializer : KSerializer<Script> {

    override val descriptor = buildClassSerialDescriptor("Script") {
        element<String>("scriptClass")
        element<JsonObject>("properties")
    }

    override fun serialize(encoder: Encoder, value: Script) =
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value::class.qualifiedName!!)

            val props = mutableMapOf<String, String>()
            value::class.memberProperties.forEach { prop ->
                prop.isAccessible = true
                val v = prop.getter.call(value) ?: return@forEach
                props[prop.name] = v.toString()
            }

            val jsonProps = JsonObject(props.mapValues { JsonPrimitive(it.value) })
            encodeSerializableElement(descriptor, 1, JsonObject.serializer(), jsonProps)
        }

    override fun deserialize(decoder: Decoder): Script {
        val json = (decoder as JsonDecoder).decodeJsonElement().jsonObject

        val className = json["scriptClass"]!!.jsonPrimitive.content
        val properties = json["properties"]!!.jsonObject

        val script = ScriptLoader.instantiate(className)

        script::class.memberProperties.forEach { prop ->
            val raw = properties[prop.name]?.jsonPrimitive?.content ?: return@forEach
            prop.isAccessible = true
            if (prop is KMutableProperty1<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val mutProp = prop as KMutableProperty1<Script, Any?>
                val converted: Any? = when (prop.returnType.classifier) {
                    Float::class   -> raw.toFloatOrNull()
                    Int::class     -> raw.toIntOrNull()
                    Boolean::class -> raw.toBooleanStrictOrNull()
                    String::class  -> raw
                    else           -> null
                }
                if (converted != null)
                    mutProp.set(script, converted)
            }
        }

        return script
    }

    fun withSerialName(name: String): KSerializer<Script> = object : KSerializer<Script> {
        override val descriptor = buildClassSerialDescriptor(name) {
            element<String>("scriptClass")
            element<JsonObject>("properties")
        }

        override fun serialize(encoder: Encoder, value: Script) =
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value::class.qualifiedName!!)

                val props = mutableMapOf<String, String>()
                value::class.memberProperties.forEach { prop ->
                    prop.isAccessible = true
                    val v = prop.getter.call(value) ?: return@forEach
                    props[prop.name] = v.toString()
                }

                val jsonProps = JsonObject(props.mapValues { JsonPrimitive(it.value) })
                encodeSerializableElement(descriptor, 1, JsonObject.serializer(), jsonProps)
            }

        override fun deserialize(decoder: Decoder) = ScriptSerializer.deserialize(decoder)
    }
}
