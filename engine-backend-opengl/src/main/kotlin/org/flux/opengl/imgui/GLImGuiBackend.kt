package org.flux.opengl.imgui

import imgui.ImGui
import imgui.gl3.ImGuiImplGl3
import org.flux.core.imgui.ImGuiRendererBackend

class GLImGuiBackend : ImGuiRendererBackend {

    private val imguiGl3 = ImGuiImplGl3()

    override fun init() {
        imguiGl3.init("#version 460 core")
    }

    override fun newFrame() = imguiGl3.newFrame()

    override fun renderDrawData() =
        imguiGl3.renderDrawData(ImGui.getDrawData())

    override fun dispose() = imguiGl3.shutdown()
}
