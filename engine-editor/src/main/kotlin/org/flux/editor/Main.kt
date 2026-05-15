package org.flux.editor

import org.flux.core.Application
import org.flux.core.FluxEngine
import org.flux.core.window.Window
import org.flux.glfw.useGLFW
import org.flux.opengl.useOpenGL

class EditorApp(window: Window) : Application(window) {
    override fun onInit() {
        pushLayer(EditorLayer())
    }
}

fun main() {
    if (System.getProperty("os.name").contains("windows", ignoreCase = true)) {
        try {
            System.loadLibrary("nvapi64")
        } catch (_: Exception) {}
    }

    FluxEngine.builder()
        .useGLFW(width = 1280, height = 720, title = "Flux Editor")
        .useOpenGL()
        .run(::EditorApp)
}
