package org.flux.core.scene

import org.flux.core.util.Timestep

abstract class Component {

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
