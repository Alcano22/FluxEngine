package org.flux.glfw.input

import com.studiohartman.jamepad.Configuration
import com.studiohartman.jamepad.ControllerManager
import com.studiohartman.jamepad.ControllerState
import org.flux.core.event.Event
import org.flux.core.event.GamepadAxisMovedEvent
import org.flux.core.event.GamepadButtonPressedEvent
import org.flux.core.event.GamepadButtonReleasedEvent
import org.flux.core.event.GamepadConnectedEvent
import org.flux.core.event.GamepadDisconnectedEvent
import org.flux.core.input.GamepadAxis
import org.flux.core.input.GamepadButton
import org.flux.core.input.Input
import org.flux.core.logging.logger
import kotlin.math.abs

class GamepadTracker(private val eventCallback: (Event) -> Unit) {

    companion object {
        private const val MAX_GAMEPADS = 4

        private val logger = logger()
    }

    private val controllerManager = ControllerManager(
        Configuration().apply {
            maxNumControllers = MAX_GAMEPADS
        }
    )

    private val isConnected = BooleanArray(MAX_GAMEPADS)

    private val previousButtons = Array(MAX_GAMEPADS) { BooleanArray(15) }
    private val previousAxes = Array(MAX_GAMEPADS) { FloatArray(6) }

    init {
        controllerManager.initSDLGamepad()

        Input.rumbleCallback = { id, left, right, durationMs ->
            if (id < MAX_GAMEPADS && isConnected[id]) {
                try {
                    val controller = controllerManager.getControllerIndex(id)
                    controller.doVibration(left, right, durationMs)
                } catch (e: Exception) {
                    logger.error { "Failed to rumble gamepad $id: ${e.message}" }
                }
            }
        }
    }

    fun update() {
        controllerManager.update()

        repeat(MAX_GAMEPADS) { id ->
            val state = controllerManager.getState(id)
            val currentlyConnected = state.isConnected

            if (currentlyConnected && !isConnected[id]) {
                isConnected[id] = true
                val name = state.controllerType ?: "Gamepad $id"
                eventCallback(GamepadConnectedEvent(id, name))
            } else if (!currentlyConnected && isConnected[id]) {
                isConnected[id] = false
                eventCallback(GamepadDisconnectedEvent(id))
            }

            if (currentlyConnected) {
                checkButtons(id, state)
                checkAxes(id, state)
            }
        }
    }

    private fun checkButtons(id: Int, state: ControllerState) {
        val currentStates = booleanArrayOf(
            state.a, state.b, state.x, state.y,
            state.lb, state.rb,
            state.back, state.start, state.guide,
            state.leftStickClick, state.rightStickClick,
            state.dpadUp, state.dpadRight, state.dpadDown, state.dpadLeft
        )

        for (i in GamepadButton.entries.indices) {
            val btn = GamepadButton.entries[i]
            val isPressed = currentStates[i]
            val wasPressed = previousButtons[id][i]

            if (isPressed && !wasPressed)
                eventCallback(GamepadButtonPressedEvent(id, btn))
            else if (!isPressed && wasPressed)
                eventCallback(GamepadButtonReleasedEvent(id, btn))

            previousButtons[id][i] = isPressed
        }
    }

    private fun checkAxes(id: Int, state: ControllerState) {
        val currentStates = floatArrayOf(
            state.leftStickX, state.leftStickY,
            state.rightStickX, state.rightStickY,
            state.leftTrigger, state.rightTrigger
        )

        for (i in GamepadAxis.entries.indices) {
            val axis = GamepadAxis.entries[i]
            val currentValue = currentStates[i]
            val previousValue = previousAxes[id][i]

            if (abs(currentValue - previousValue) > 0.001f) {
                eventCallback(GamepadAxisMovedEvent(id, axis, currentValue))
                previousAxes[id][i] = currentValue
            }
        }
    }

    fun destroy() = controllerManager.quitSDLGamepad()
}
