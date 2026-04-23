package org.flux.core.layer

import org.flux.core.event.Event
import org.flux.core.util.Timestep

abstract class Layer(val name: String = "Layer") {

    open fun onAttach() {}
    open fun onDetach() {}

    open fun onUpdate(ts: Timestep) {}

    open fun onRender() {}
    open fun onImGuiRender() {}

    open fun onEvent(event: Event) {}

    override fun toString() = name
}
