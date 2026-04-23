package org.flux.glfw

import org.flux.core.EngineBuilder
import org.flux.glfw.imgui.GLFWImGuiBackend
import org.flux.glfw.window.GLFWWindow

fun EngineBuilder.useGLFW(
    width: Int = 1280,
    height: Int = 720,
    title: String = "Flux Engine"
): EngineBuilder {
    val window = GLFWWindow(width, height, title)
    this.window = window
    this.imguiPlatformBackend = GLFWImGuiBackend(window.nativeHandle)
    return this
}
