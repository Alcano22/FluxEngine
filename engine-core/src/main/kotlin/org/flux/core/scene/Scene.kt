package org.flux.core.scene

import kotlinx.serialization.Serializable
import org.flux.core.renderer.Camera
import org.flux.core.renderer.LightEnvironment
import org.flux.core.renderer.PointLight2DData
import org.flux.core.renderer.Renderer2D
import org.flux.core.renderer.Renderer3D
import org.flux.core.util.Timestep
import org.joml.Vector2f

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

    fun findEntityById(id: Int): Entity? =
        entities.firstOrNull { it.id == id }

    fun findEntityByUuid(uuid: String): Entity? =
        entities.firstOrNull { it.uuid == uuid }

    fun onStart() {
        repeat(entities.size) { i ->
            entities[i].start()
        }
    }

    fun onStop() {
        repeat(entities.size) { i ->
            entities[i].stop()
        }
    }

    fun onUpdate(ts: Timestep) {
        repeat(entities.size) { i ->
            entities[i].update(ts)
        }
    }

    fun onRender(){
        val mainCam = getComponentInScene<CameraComponent>()?.camera ?: return
        render2D(mainCam)
        render3D(mainCam)
    }

    fun onRenderWithCamera(camera: Camera) {
        render2DUnlit(camera)
        render3D(camera)
    }

    fun onRenderEntityIDs(camera: Camera) {
        Renderer2D.beginSceneEntityID(camera)
        repeat(entities.size) { i -> entities[i].render2D() }
        Renderer2D.endScene()
    }

    private fun render2D(camera: Camera) {
        val lightComponents = findAllComponentsOfType<PointLight2DComponent>()
        if (lightComponents.isEmpty())
            Renderer2D.beginScene(camera)
        else {
            val env = LightEnvironment()
            lightComponents.forEach { light ->
                val pos = light.transform.position
                env.pointLights.add(
                    PointLight2DData(
                        position  = Vector2f(pos.x, pos.y),
                        color     = light.color,
                        intensity = light.intensity,
                        radius    = light.radius
                    )
                )
            }
            Renderer2D.beginScene(camera, env)
        }

        repeat(entities.size) { i -> entities[i].render2D() }
        Renderer2D.endScene()
    }

    private fun render2DUnlit(camera: Camera) {
        Renderer2D.beginScene(camera)
        repeat(entities.size) { i -> entities[i].render2D() }
        Renderer2D.endScene()
    }

    private fun render3D(camera: Camera) {
        Renderer3D.beginScene(camera)
        repeat(entities.size) { i -> entities[i].render3D() }
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
