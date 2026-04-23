package org.flux.core.event

import org.flux.core.util.BitEnum

enum class EventCategory : BitEnum {
    Window,
    Input,
    Keyboard,
    Mouse,
    MouseButton,
    Gamepad
}

abstract class Event {

    var isHandled = false

    abstract val categoryFlags: Int

    open val name get() = this::class.simpleName ?: "UnknownEvent"

    fun isInCategory(category: EventCategory) =
        (categoryFlags and category.bit) != 0

    override fun toString() = name
}
