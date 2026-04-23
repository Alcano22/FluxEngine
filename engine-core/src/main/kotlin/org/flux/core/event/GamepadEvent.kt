package org.flux.core.event

import org.flux.core.input.GamepadAxis
import org.flux.core.input.GamepadButton

abstract class GamepadEvent(val gamepadId: Int) : Event() {
    override val categoryFlags = EventCategory.Input + EventCategory.Gamepad
}

class GamepadConnectedEvent(gamepadId: Int, val gamepadName: String) : GamepadEvent(gamepadId) {
    override fun toString() = "$name: [ID $gamepadId] $gamepadName"
}

class GamepadDisconnectedEvent(gamepadId: Int) : GamepadEvent(gamepadId) {
    override fun toString() = "$name: [ID $gamepadId]"
}

class GamepadButtonPressedEvent(gamepadId: Int, val button: GamepadButton) : GamepadEvent(gamepadId) {
    override fun toString() = "$name: [ID $gamepadId] $button"
}

class GamepadButtonReleasedEvent(gamepadId: Int, val button: GamepadButton) : GamepadEvent(gamepadId) {
    override fun toString() = "$name: [ID $gamepadId] $button"
}

class GamepadAxisMovedEvent(
    gamepadId: Int,
    val axis: GamepadAxis,
    val value: Float
) : GamepadEvent(gamepadId) {
    override fun toString() = "$name: [ID $gamepadId] $axis = $value"
}
