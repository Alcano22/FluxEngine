package org.flux.core.scene

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.flux.core.util.Timestep
import java.util.UUID
import kotlin.reflect.full.hasAnnotation

@Serializable
class Entity(
    var name: String = "Unnamed Entity",
    val uuid: String = UUID.randomUUID().toString()
) {

    companion object {
        private var nextId = 1
    }

    @Transient
    lateinit var scene: Scene
        internal set

    @Transient
    val id = nextId++

    val components = mutableListOf<Component>()

    val transform get() = getComponent<TransformComponent>()
        ?: error("Entity '$name' has no TransformComponent")

    fun addComponent(component: Component): Component? {
        val kClass = component::class
        if (kClass.hasAnnotation<SingleComponent>() && components.any { it::class == kClass })
            return null

        component.setEntity(this)
        components.add(component)
        component.onAttach()
        return component
    }

    inline fun <reified T : Component> getComponent() =
        components.filterIsInstance<T>().firstOrNull()

    inline fun <reified T : Component> hasComponent() = getComponent<T>() != null

    internal fun start() {
        repeat(components.size) { i ->
            components[i].onStart()
        }
    }

    internal fun stop() {
        repeat(components.size) { i ->
            components[i].onStop()
        }
    }

    internal fun update(ts: Timestep) {
        repeat(components.size) { i ->
            components[i].onUpdate(ts)
        }
    }

    internal fun render2D() {
        repeat(components.size) { i ->
            components[i].onRender2D()
        }
    }

    internal fun render3D() {
        repeat(components.size) { i ->
            components[i].onRender3D()
        }
    }

    internal fun destroy() {
        components.forEach { it.onDestroy() }
        components.clear()
    }
}
