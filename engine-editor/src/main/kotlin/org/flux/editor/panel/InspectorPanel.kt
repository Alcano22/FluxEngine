package org.flux.editor.panel

import imgui.ImGui
import imgui.flag.ImGuiTreeNodeFlags
import imgui.type.ImBoolean
import org.flux.core.asset.AssetManager
import org.flux.core.asset.meta.ImageMeta
import org.flux.core.asset.meta.MetaManager
import org.flux.core.imgui.ImGuiEx
import org.flux.core.imgui.ReflectionInspector
import org.flux.editor.util.SelectionManager
import java.nio.file.Path
import kotlin.io.path.extension

class InspectorPanel : EditorPanel("Inspector") {

    override fun drawContent() {
        ImGuiEx.window(title) {
            when {
                SelectionManager.selectedEntity != null -> {
                    for (component in SelectionManager.selectedEntity!!.components)
                        ReflectionInspector.drawComponent(component)
                }
                SelectionManager.selectedPath != null ->
                    drawAssetInspector(SelectionManager.selectedPath!!)
                else -> ImGui.textDisabled("No selection")
            }
        }
    }

    private fun drawAssetInspector(path: Path) {
        val ext = path.extension.lowercase()
        when (ext) {
            "png", "jpg", "jpeg" -> drawImageMeta(path)
            else                 -> ImGui.textDisabled("No inspector for .$ext files")
        }
    }

    private fun drawImageMeta(path: Path) {
        val absPath = path.toAbsolutePath().toString()
        val meta = MetaManager.getOrCreate(absPath, ImageMeta())

        val flags = ImGuiTreeNodeFlags.DefaultOpen or ImGuiTreeNodeFlags.Framed
        ImGuiEx.treeNode("Image Import Settings", flags) {
            var changed = false

            changed = ImGuiEx.enumCombo("Min Filter", meta::minFilter) || changed
            changed = ImGuiEx.enumCombo("Mag Filter", meta::magFilter) || changed
            changed = ImGuiEx.enumCombo("Wrap S", meta::wrapS) || changed
            changed = ImGuiEx.enumCombo("Wrap T", meta::wrapT) || changed

            val mipmaps = ImBoolean(meta.generateMipmaps)
            if (ImGui.checkbox("Generate Mipmaps", mipmaps)) {
                meta.generateMipmaps = mipmaps.get()
                changed = true
            }

            if (changed) {
                MetaManager.save(absPath, meta)
                AssetManager.invalidateTexture(absPath)
            }
        }
    }
}
