package org.flux.editor.panel

import imgui.ImGui
import imgui.flag.ImGuiStyleVar
import org.flux.core.util.Timestep

abstract class EditorPanel(
    val title: String,
    val noPadding: Boolean = false
) {

    var isOpen = true

    open fun onUpdate(ts: Timestep) {}

    fun onImGuiRender() {
        if (noPadding)
            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
        try {
            drawContent()
        } finally {
            if (noPadding)
                ImGui.popStyleVar()
        }
    }

    abstract fun drawContent()

    open fun dispose() {}
}
