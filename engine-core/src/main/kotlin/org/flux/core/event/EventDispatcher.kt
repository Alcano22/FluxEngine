package org.flux.core.event

class EventDispatcher(val event: Event) {

    inline fun <reified T : Event> dispatch(handler: (T) -> Boolean): Boolean {
        if (event !is T)
            return false

        event.isHandled = event.isHandled || handler(event)
        return true
    }
}
