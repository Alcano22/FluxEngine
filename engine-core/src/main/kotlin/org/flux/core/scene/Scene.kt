package org.flux.core.scene

import kotlinx.serialization.Serializable
import org.flux.core.renderer.Renderer2D
import org.flux.core.renderer.Renderer3D
import org.flux.core.util.Timestep

@Serializable
class Scene {

    val entities = mutableListOf<Entity>()

    fun createEntity(name: String = "Unnamed Entity"): Entity {
        val entity = Entity(name)
        entity.scene = this
        entity.addComponent(TransformComponent())
        entities.add(entity)
        return entity
    }

    fun destroyEntity(entity: Entity) {
        entity.destroy()
        entities.remove(entity)
    }

    inline fun <reified T : Component> findEntityWithComponent(): Entity? =
        entities.firstOrNull { it.hasComponent<T>() }

    inline fun <reified T : Component> findAllEntitiesWithComponent(): List<Entity> =
        entities.filter { it.hasComponent<T>() }

    inline fun <reified T : Component> findAllComponentsOfType(): List<T> {
        val result = mutableListOf<T>()
        repeat(entities.size) { i ->
            val component = entities[i].getComponent<T>()
            if (component != null)
                result.add(component)
        }
        return result
    }

    inline fun <reified T : Component> getComponentInScene(): T? =
        findEntityWithComponent<T>()?.getComponent<T>()

    fun findEntityByName(name: String): Entity? =
        entities.firstOrNull { it.name == name }

    fun onUpdate(ts: Timestep) {
        repeat(entities.size) { i ->
            entities[i].update(ts)
        }
    }

    fun onRender() {
        val mainCam = getComponentInScene<CameraComponent>()?.camera
        if (mainCam == null) return

        Renderer2D.beginScene(mainCam)
        repeat(entities.size) { i ->
            entities[i].render2D()
        }
        Renderer2D.endScene()

        Renderer3D.beginScene(mainCam)
        repeat(entities.size) { i ->
            entities[i].render3D()
        }
        Renderer3D.endScene()
    }

    fun onViewportResize(width: Int, height: Int) {
        val aspectRatio = width.toFloat() / height.toFloat()
        findAllComponentsOfType<CameraComponent>().forEach { cam ->
            cam.aspectRatio = aspectRatio
            cam.recalculateCamera()
        }
    }
}
