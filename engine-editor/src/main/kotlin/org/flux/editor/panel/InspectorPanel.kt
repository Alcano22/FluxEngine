package org.flux.editor.panel

import imgui.ImGui
import org.flux.core.imgui.ImGuiEx
import org.flux.core.imgui.ReflectionInspector
import org.flux.editor.util.SelectionManager

class InspectorPanel : EditorPanel("Inspector") {

    override fun drawContent() {
        ImGuiEx.window(title) {
            val entity = SelectionManager.selectedEntity
            if (entity != null) {
                for (component in entity.components)
                    ReflectionInspector.drawComponent(component)
            } else
                ImGui.textDisabled("No entity selected")
        }
    }

}
