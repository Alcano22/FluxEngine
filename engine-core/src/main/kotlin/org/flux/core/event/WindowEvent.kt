package org.flux.core.event

class WindowCloseEvent : Event() {
    override val categoryFlags = EventCategory.Window.bit
}

class WindowResizedEvent(val width: Int, val height: Int) : Event() {
    override val categoryFlags = EventCategory.Window.bit

    override fun toString() = "$name: ${width}x$height"
}
