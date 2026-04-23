package org.flux.glfw.imgui

import imgui.ImGui
import imgui.glfw.ImGuiImplGlfw
import org.flux.core.imgui.ImGuiPlatformBackend
import org.lwjgl.glfw.GLFW.*

class GLFWImGuiBackend(private val windowHandle: Long) : ImGuiPlatformBackend {

    private val imguiGlfw = ImGuiImplGlfw()

    override fun init() {
        imguiGlfw.init(windowHandle, true)
    }

    override fun newFrame() = imguiGlfw.newFrame()

    override fun dispose() = imguiGlfw.shutdown()

    override fun updateViewports() {
        val backupWindowPtr = glfwGetCurrentContext()
        ImGui.updatePlatformWindows()
        ImGui.renderPlatformWindowsDefault()
        glfwMakeContextCurrent(backupWindowPtr)
    }
}
