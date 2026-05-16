package org.flux.core.input

import org.flux.core.event.*

object Input {

    private val keysPressed = mutableSetOf<Key>()
    private val keysDown    = mutableSetOf<Key>()
    private val keysUp      = mutableSetOf<Key>()

    private val mouseButtonsPressed = mutableSetOf<MouseButton>()
    private val mouseButtonsDown    = mutableSetOf<MouseButton>()
    private val mouseButtonsUp      = mutableSetOf<MouseButton>()

    var mouseX: Float = 0f
        private set
    var mouseY: Float = 0f
        private set

    private val connectedGamepads = mutableMapOf<Int, String>()

    private val gamepadButtonsPressed = mutableSetOf<Pair<Int, GamepadButton>>()
    private val gamepadButtonsDown    = mutableSetOf<Pair<Int, GamepadButton>>()
    private val gamepadButtonsUp      = mutableSetOf<Pair<Int, GamepadButton>>()
    private val gamepadAxes           = mutableMapOf<Pair<Int, GamepadAxis>, Float>()

    var rumbleCallback: ((id: Int, left: Float, right: Float, durationMs: Int) -> Unit)? = null

    var blocked = false

    fun getKey(key: Key)     = if (blocked) false else key in keysPressed
    fun getKeyDown(key: Key) = if (blocked) false else key in keysDown
    fun getKeyUp(key: Key)   = if (blocked) false else key in keysUp

    fun getMouseButton(button: MouseButton)     = if (blocked) false else button in mouseButtonsPressed
    fun getMouseButtonDown(button: MouseButton) = if (blocked) false else button in mouseButtonsDown
    fun getMouseButtonUp(button: MouseButton)   = if (blocked) false else button in mouseButtonsUp

    fun isGamepadConnected(id: Int) = connectedGamepads.containsKey(id)
    fun getGamepadName(id: Int) = connectedGamepads[id] ?: "Unknown Gamepad"

    fun getGamepadButton(id: Int, button: GamepadButton)     = (id to button) in gamepadButtonsPressed
    fun getGamepadButtonDown(id: Int, button: GamepadButton) = (id to button) in gamepadButtonsDown
    fun getGamepadButtonUp(id: Int, button: GamepadButton)   = (id to button) in gamepadButtonsUp
    fun getGamepadAxis(id: Int, axis: GamepadAxis)           = gamepadAxes[id to axis] ?: 0f

    fun setGamepadRumble(id: Int, leftMotor: Float, rightMotor: Float, durationMs: Int) =
        rumbleCallback?.invoke(id, leftMotor, rightMotor, durationMs)

    fun enableGamepadRumble(id: Int, leftMotor: Float, rightMotor: Float) =
        setGamepadRumble(id, leftMotor, rightMotor, 3600000)

    fun disableGamepadRumble(id: Int) = setGamepadRumble(id, 0f, 0f, 0)

    internal fun endFrame() {
        keysDown.clear()
        keysUp.clear()

        mouseButtonsDown.clear()
        mouseButtonsUp.clear()

        gamepadButtonsDown.clear()
        gamepadButtonsUp.clear()
    }

    internal fun onEvent(event: Event) {
        val dispatcher = EventDispatcher(event)
        dispatcher.dispatch<KeyPressedEvent>(::onKeyPressed)
        dispatcher.dispatch<KeyReleasedEvent>(::onKeyReleased)

        dispatcher.dispatch<MouseButtonPressedEvent>(::onMousePressed)
        dispatcher.dispatch<MouseButtonReleasedEvent>(::onMouseReleased)
        dispatcher.dispatch<MouseMovedEvent>(::onMouseMoved)

        dispatcher.dispatch<GamepadConnectedEvent>(::onGamepadConnected)
        dispatcher.dispatch<GamepadDisconnectedEvent>(::onGamepadDisconnected)

        dispatcher.dispatch<GamepadButtonPressedEvent>(::onGamepadPressed)
        dispatcher.dispatch<GamepadButtonReleasedEvent>(::onGamepadReleased)
        dispatcher.dispatch<GamepadAxisMovedEvent>(::onGamepadAxisMoved)
    }

    private fun onKeyPressed(e: KeyPressedEvent): Boolean {
        if (e.repeatCount == 0)
            keysDown.add(e.key)
        keysPressed.add(e.key)
        return false
    }

    private fun onKeyReleased(e: KeyReleasedEvent): Boolean {
        keysPressed.remove(e.key)
        keysUp.add(e.key)
        return false
    }

    private fun onMousePressed(e: MouseButtonPressedEvent): Boolean {
        mouseButtonsDown.add(e.button)
        mouseButtonsPressed.add(e.button)
        return false
    }

    private fun onMouseReleased(e: MouseButtonReleasedEvent): Boolean {
        mouseButtonsPressed.remove(e.button)
        mouseButtonsUp.add(e.button)
        return false
    }

    private fun onMouseMoved(e: MouseMovedEvent): Boolean {
        mouseX = e.x
        mouseY = e.y
        return false
    }

    private fun onGamepadConnected(e: GamepadConnectedEvent): Boolean {
        connectedGamepads[e.gamepadId] = e.gamepadName
        return false
    }

    private fun onGamepadDisconnected(e: GamepadDisconnectedEvent): Boolean {
        connectedGamepads.remove(e.gamepadId)

        gamepadButtonsPressed.removeAll { it.first == e.gamepadId }
        gamepadButtonsDown.removeAll { it.first == e.gamepadId }
        gamepadAxes.keys.removeAll { it.first == e.gamepadId }

        return false
    }

    private fun onGamepadPressed(e: GamepadButtonPressedEvent): Boolean {
        val key = e.gamepadId to e.button
        gamepadButtonsDown.add(key)
        gamepadButtonsPressed.add(key)
        return false
    }

    private fun onGamepadReleased(e: GamepadButtonReleasedEvent): Boolean {
        val key = e.gamepadId to e.button
        gamepadButtonsPressed.remove(key)
        gamepadButtonsUp.add(key)
        return false
    }

    private fun onGamepadAxisMoved(e: GamepadAxisMovedEvent): Boolean {
        gamepadAxes[e.gamepadId to e.axis] = e.value
        return false
    }
}
