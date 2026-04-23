package org.flux.core.window

import org.flux.core.event.Event

interface Window {

    val width: Int
    val height: Int

    val nativeHandle: Long

    var eventCallback: ((Event) -> Unit)?

    fun update()

    fun destroy()
}
