package org.flux.core.event

import org.flux.core.input.Key

abstract class KeyEvent(val key: Key) : Event() {
    override val categoryFlags = EventCategory.Input + EventCategory.Keyboard
}

class KeyPressedEvent(key: Key, val repeatCount: Int) : KeyEvent(key) {
    override fun toString() = "$name: $key ($repeatCount repeats)"
}

class KeyReleasedEvent(key: Key) : KeyEvent(key) {
    override fun toString() = "$name: $key"
}

class KeyTypedEvent(val codepoint: Char) : Event() {
    override val categoryFlags = EventCategory.Input + EventCategory.Keyboard

    override fun toString() = "$name: $codepoint"
}
