package org.flux.core.scene

import org.flux.core.util.Timestep
import kotlin.reflect.full.hasAnnotation

class Entity(
    val name: String,
    val scene: Scene
) {

    val transform = TransformComponent()

    @PublishedApi
    internal val components = mutableListOf<Component>()

    init {
        transform.setEntity(this)
        components.add(transform)
        transform.onAttach()
    }

    inline fun <reified T : Component> addComponent(component: T): T {
        if (T::class.hasAnnotation<SingleComponent>() && hasComponent<T>())
            return getComponent<T>()!!

        component.setEntity(this)
        components.add(component)
        component.onAttach()
        return component
    }

    inline fun <reified T : Component> getComponent(): T? {
        if (T::class == TransformComponent::class)
            return transform as T
        return components.filterIsInstance<T>().firstOrNull()
    }

    inline fun <reified T : Component> hasComponent() = getComponent<T>() != null

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
