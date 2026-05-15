package org.flux.glfw.window

import org.flux.core.event.*
import org.flux.core.logging.logger
import org.flux.core.util.nullptr
import org.flux.core.window.Window
import org.flux.glfw.input.GamepadTracker
import org.flux.glfw.input.toFluxKey
import org.flux.glfw.input.toFluxMouseButton
import org.lwjgl.glfw.GLFW.*

class GLFWWindow(
    override val width: Int,
    override val height: Int,
    val title: String
) : Window {

    companion object {
        private val logger = logger()
    }

    private val handle: Long

    override val nativeHandle: Long
        get() = handle

    private var gamepadTracker: GamepadTracker? = null

    override var eventCallback: ((Event) -> Unit)? = null
        set(value) {
            field = value
            if (value != null)
                gamepadTracker = GamepadTracker(value)
        }

    init {
        if (!glfwInit())
            throw logger.throwing(IllegalStateException("Unable to initialize GLFW"))

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)

        handle = glfwCreateWindow(width, height, title, nullptr, nullptr)
        if (handle == nullptr)
            throw logger.throwing(RuntimeException("Failed to create GLFW window"))

        glfwMakeContextCurrent(handle)
        glfwSwapInterval(0)

        setupCallbacks()
    }

    private fun setupCallbacks() {
        glfwSetWindowSizeCallback(handle) { _, w, h ->
            eventCallback?.invoke(WindowResizedEvent(w, h))
        }

        glfwSetWindowCloseCallback(handle) { _ ->
            eventCallback?.invoke(WindowCloseEvent())
        }

        glfwSetKeyCallback(handle) { _, key, _, action, _ ->
            val fluxKey = key.toFluxKey()
            when (action) {
                GLFW_PRESS   -> eventCallback?.invoke(KeyPressedEvent(fluxKey, 0))
                GLFW_RELEASE -> eventCallback?.invoke(KeyReleasedEvent(fluxKey))
                GLFW_REPEAT  -> eventCallback?.invoke(KeyPressedEvent(fluxKey, 1))
            }
        }

        glfwSetCharCallback(handle) { _, codepoint ->
            eventCallback?.invoke(KeyTypedEvent(codepoint.toChar()))
        }

        glfwSetMouseButtonCallback(handle) { _, button, action, _ ->
            val fluxButton = button.toFluxMouseButton()
            when (action) {
                GLFW_PRESS   -> eventCallback?.invoke(MouseButtonPressedEvent(fluxButton))
                GLFW_RELEASE -> eventCallback?.invoke(MouseButtonReleasedEvent(fluxButton))
            }
        }

        glfwSetScrollCallback(handle) { _, offsetX, offsetY ->
            eventCallback?.invoke(MouseScrolledEvent(offsetX.toFloat(), offsetY.toFloat()))
        }

        glfwSetCursorPosCallback(handle) { _, posX, posY ->
            eventCallback?.invoke(MouseMovedEvent(posX.toFloat(), posY.toFloat()))
        }
    }

    override fun update() {
        glfwSwapBuffers(handle)
        glfwPollEvents()

        gamepadTracker?.update()
    }

    override fun destroy() {
        gamepadTracker?.destroy()

        glfwDestroyWindow(handle)
        glfwTerminate()
    }
}
