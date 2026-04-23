package org.flux.core.scene

import org.flux.core.renderer.Camera
import org.flux.core.renderer.OrthographicCamera
import org.flux.core.renderer.PerspectiveCamera
import org.flux.core.util.Timestep

enum class CameraType {
    ORTHOGRAPHIC,
    PERSPECTIVE
}

@SingleComponent
class CameraComponent(
    type: CameraType = CameraType.ORTHOGRAPHIC,
    var isPrimary: Boolean = true
) : Component() {

    var aspectRatio = 16f / 9f

    var orthographicSize = 5f

    var perspectiveFov = 45f
    var perspectiveNear = 0.1f
    var perspectiveFar = 1000f

    lateinit var camera: Camera
        private set

    var type = type
        set(value) {
            field = value
            recalculateCamera()
        }

    init {
        recalculateCamera()
    }

    fun recalculateCamera() {
        camera = when (type) {
            CameraType.ORTHOGRAPHIC -> OrthographicCamera(
                size = orthographicSize,
                aspectRatio = aspectRatio
            )
            CameraType.PERSPECTIVE -> PerspectiveCamera(
                fov = perspectiveFov,
                aspectRatio = aspectRatio,
                nearClip = perspectiveNear,
                farClip = perspectiveFar
            )
        }
    }

    override fun onUpdate(ts: Timestep) {
        when (val cam = camera) {
            is OrthographicCamera -> {
                cam.position = transform.position
                cam.rotation = transform.rotation.z
            }
            is PerspectiveCamera -> {
                cam.position = transform.position
                cam.rotation = transform.rotation
            }
        }
    }
}
