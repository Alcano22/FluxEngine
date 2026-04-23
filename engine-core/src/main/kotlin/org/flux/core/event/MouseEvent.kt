package org.flux.core.event

import org.flux.core.input.MouseButton
import org.flux.core.util.plus

abstract class MouseButtonEvent(val button: MouseButton) : Event() {
    override val categoryFlags = EventCategory.Input + EventCategory.Mouse + EventCategory.MouseButton
}

class MouseButtonPressedEvent(button: MouseButton) : MouseButtonEvent(button) {
    override fun toString() = "$name: $button"
}

class MouseButtonReleasedEvent(button: MouseButton) : MouseButtonEvent(button) {
    override fun toString() = "$name: $button"
}

class MouseMovedEvent(val x: Float, val y: Float) : Event() {
    override val categoryFlags = EventCategory.Input + EventCategory.Mouse

    override fun toString() = "$name: $x, $y"
}

class MouseScrolledEvent(val offsetX: Float, val offsetY: Float) : Event() {
    override val categoryFlags = EventCategory.Input + EventCategory.Mouse

    override fun toString() = "$name: $offsetX, $offsetY"
}
