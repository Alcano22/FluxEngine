package org.flux.editor.panel

import imgui.ImGui
import imgui.flag.ImGuiInputTextFlags
import imgui.flag.ImGuiTreeNodeFlags
import org.flux.core.imgui.ImGuiEx
import org.flux.core.scene.Entity
import org.flux.core.scene.Scene
import org.flux.editor.SceneContext
import org.flux.editor.util.SelectionManager

class SceneHierarchyPanel(sceneContext: SceneContext) : EditorPanel("Scene Hierarchy") {

    var context = sceneContext.scene

    private var renamingEntity: Entity? = null

    init {
        sceneContext.onSceneChange { newScene ->
            context = newScene
        }
    }

    override fun drawContent() {
        ImGuiEx.window(title) {
            context.entities.forEach { drawEntityNode(it) }

            if (ImGui.isMouseDown(0) && ImGui.isWindowHovered())
                SelectionManager.clear()

            if (ImGui.beginPopupContextWindow("##HierarchyContext")) {
                if (ImGui.menuItem("Create Empty Entity"))
                    context.createEntity("New Entity")
                ImGui.endPopup()
            }
        }
    }

    private fun drawEntityNode(entity: Entity) {
        val isSelected = (SelectionManager.selectedEntity == entity)
        val isRenaming = renamingEntity == entity

        var flags = ImGuiTreeNodeFlags.OpenOnArrow or ImGuiTreeNodeFlags.SpanAvailWidth
        if (isSelected)
            flags = flags or ImGuiTreeNodeFlags.Selected
        flags = flags or ImGuiTreeNodeFlags.Leaf

        if (isRenaming) {
            ImGui.indent(ImGui.getTreeNodeToLabelSpacing())
            ImGui.setNextItemWidth(-1f)
            ImGui.setKeyboardFocusHere()
            ImGuiEx.inputText(
                "##rename_${entity.id}",
                entity::name,
                flags = ImGuiInputTextFlags.EnterReturnsTrue or ImGuiInputTextFlags.AutoSelectAll
            )

            if (ImGui.isItemDeactivated())
                renamingEntity = null
            ImGui.unindent(ImGui.getTreeNodeToLabelSpacing())
        } else {
            val nodeOpen = ImGui.treeNodeEx(entity.id.toString(), flags, entity.name)

            if (ImGui.isItemClicked())
                SelectionManager.selected = entity

            if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0))
                renamingEntity = entity

            if (nodeOpen)
                ImGui.treePop()
        }
    }
}
