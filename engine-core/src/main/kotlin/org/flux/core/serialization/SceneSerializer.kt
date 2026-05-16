package org.flux.core.serialization

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializerOrNull
import org.flux.core.scene.Component
import org.flux.core.scene.Scene
import org.reflections.Reflections
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

object SceneSerializer {

    val additionalSerializers = mutableListOf<SerializersModuleBuilder.() -> Unit>()

    @OptIn(InternalSerializationApi::class)
    val format: Json by lazy {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            classDiscriminator = "class"

            serializersModule = SerializersModule {
                polymorphic(Component::class) {
                    val reflections = Reflections(
                        ConfigurationBuilder()
                            .forPackages("org.flux.core")
                            .addClassLoaders(Component::class.java.classLoader)
                    )
                    val componentClasses = reflections.getSubTypesOf(Component::class.java)

                    for (cls in componentClasses) {
                        val kClass = cls.kotlin

                        val serializer = kClass.serializerOrNull()
                        if (serializer != null) {
                            @Suppress("UNCHECKED_CAST")
                            subclass(
                                kClass as KClass<Component>,
                                serializer as KSerializer<Component>
                            )
                        }
                    }
                }

                additionalSerializers.forEach { it() }

                contextual(Vector2iSerializer)
                contextual(Vector3iSerializer)
                contextual(Vector4iSerializer)
                contextual(Vector2fSerializer)
                contextual(Vector3fSerializer)
                contextual(Vector4fSerializer)
            }
        }
    }

    fun serialize(scene: Scene) = format.encodeToString(scene)

    fun deserialize(json: String): Scene {
        val scene = format.decodeFromString<Scene>(json)

        for (entity in scene.entities) {
            entity.scene = scene

            for (component in entity.components) {
                component.setEntity(entity)
                component.onAttach()
            }
        }

        return scene
    }
}
