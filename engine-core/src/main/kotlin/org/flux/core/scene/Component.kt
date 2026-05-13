package org.flux.core.scene

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.flux.core.util.Timestep

@Serializable
abstract class Component {

    @Transient
    lateinit var entity: Entity
        private set

    val transform get() = entity.transform

    @PublishedApi
    internal fun setEntity(entity: Entity) {
        this.entity = entity
    }

    open fun onAttach() {}

    open fun onUpdate(ts: Timestep) {}
    open fun onRender2D() {}
    open fun onRender3D() {}

    open fun onDestroy() {}
}
