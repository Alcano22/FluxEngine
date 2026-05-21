package org.flux.editor.panel

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiTreeNodeFlags
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean
import kotlinx.serialization.encodeToString
import org.flux.core.asset.AssetData
import org.flux.core.asset.AssetManager
import org.flux.core.asset.Sprite
import org.flux.core.asset.SpritesheetHandle
import org.flux.core.asset.TextureHandle
import org.flux.core.asset.meta.ImageMeta
import org.flux.core.asset.meta.MetaManager
import org.flux.core.asset.resolve
import org.flux.core.imgui.ImGuiEx
import org.flux.core.imgui.ReflectionInspector
import org.flux.core.scene.Entity
import org.flux.core.serialization.AssetSerializer
import org.flux.editor.util.DnDPayload
import org.flux.editor.util.NotificationModal
import org.flux.editor.util.SelectionManager
import org.flux.scripting.loader.ScriptLoader
import java.io.File
import java.nio.file.Path
import kotlin.io.path.extension

class InspectorPanel : EditorPanel("Inspector") {

    override fun drawContent() {
        ImGuiEx.window(title) {
            when {
                SelectionManager.selectedEntity != null ->
                    drawEntityInspector(SelectionManager.selectedEntity!!)
                SelectionManager.selectedPath != null ->
                    drawAssetInspector(SelectionManager.selectedPath!!)
                else -> ImGui.textDisabled("No selection")
            }
        }
    }

    private fun drawEntityInspector(entity: Entity) {
        entity.components.forEach { ReflectionInspector.drawComponent(it) }

        ImGui.separator()

        val availX = ImGui.getContentRegionAvailX()
        val availY = ImGui.getContentRegionAvailY()

        ImGui.pushStyleColor(ImGuiCol.Button,        0f, 0f, 0f, 0f)
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.2f, 0.2f, 0.2f, 0.3f)
        ImGui.pushStyleColor(ImGuiCol.ButtonActive,  0.2f, 0.2f, 0.2f, 0.5f)
        ImGui.invisibleButton("##script_drop", availX, availY.coerceAtLeast(40f))
        ImGui.popStyleColor(3)

        val text = "Drop script here..."
        val textW = ImGui.calcTextSizeX(text)
        val btnMin = ImGui.getItemRectMin()
        val btnMax = ImGui.getItemRectMax()
        val textX = btnMin.x + (btnMax.x - btnMin.x - textW) * 0.5f
        val textY = btnMin.y + (btnMax.y - btnMin.y - ImGui.getTextLineHeight()) * 0.5f
        ImGui.getWindowDrawList().addText(textX, textY, ImGui.colorConvertFloat4ToU32(0.5f, 0.5f, 0.5f, 1f), text)

        if (ImGui.beginDragDropTarget()) {
            val payload = ImGui.acceptDragDropPayload<String>(DnDPayload.SCRIPT)
            if (payload != null) {
                val className = File(payload).nameWithoutExtension
                val script = ScriptLoader.instantiateOrNull(className)
                if (script == null)
                    NotificationModal.error("Failed to load script '$className'.\nMake sure it extends Script and is compiled.")
                else if (entity.addComponent(script) == null)
                    NotificationModal.error("Failed to add '$className'.\nA script with this name is already attached.")
            }
            ImGui.endDragDropTarget()
        }
    }

    private fun drawAssetInspector(path: Path) {
        when (val ext = path.extension.lowercase()) {
            "png", "jpg", "jpeg" -> drawImageMeta(path)
            "asset" -> when (AssetManager.getAssetType(path.toAbsolutePath().toString())) {
                "SPRITESHEET" -> drawSpritesheetInspector(path)
                else          -> ImGui.textDisabled("No inspector for this asset type")
            }
            else -> ImGui.textDisabled("No inspector for .$ext files")
        }
    }

    private fun drawImageMeta(path: Path) {
        val absPath = path.toAbsolutePath().toString()
        val meta = MetaManager.getOrCreate(absPath, ImageMeta())

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

    private fun drawSpritesheetInspector(path: Path) {
        val absPath = path.toAbsolutePath().toString()
        val sheet = runCatching {
            AssetManager.getAsset<AssetData.Spritesheet>(absPath)
        }.getOrElse {
            ImGui.textDisabled("Failed to load spritesheet")
            return
        }

        val tex = runCatching { sheet.texture.resolve() }.getOrNull()

        if (tex == null) {
            ImGui.textDisabled("No texture loaded")
            return
        }

        val cols = ((tex.width - sheet.offsetX) / (sheet.cellWidth + sheet.paddingX).coerceAtLeast(1)).coerceAtLeast(1)
        val rows = ((tex.height - sheet.offsetY) / (sheet.cellHeight + sheet.paddingY).coerceAtLeast(1)).coerceAtLeast(1)

        val emptyFrames = sheet.getEmptyFrames()
        val validFrames = (0 until rows).flatMap { row ->
            (0 until cols).mapNotNull { col ->
                val x1 = sheet.offsetX + col * (sheet.cellWidth + sheet.paddingX) + sheet.cellWidth
                val y1 = sheet.offsetY + row * (sheet.cellHeight + sheet.paddingY) + sheet.cellHeight
                val frameIndex = row * cols + col
                if (x1 <= tex.width && y1 <= tex.height && frameIndex !in emptyFrames)
                    frameIndex
                else null
            }
        }

        ImGui.textDisabled("${validFrames.size} frames")
        ImGui.separator()

        val thumbSize = 48f
        val availW = ImGui.getContentRegionAvailX()
        val perRow = ((availW + 4f) / (thumbSize + 4f)).toInt().coerceAtLeast(1)

        validFrames.forEachIndexed { idx, frameIndex ->
            ImGui.pushID(frameIndex)

            val uvs = sheet.computeUVs(frameIndex)
            val aspect = sheet.cellWidth.toFloat() / sheet.cellHeight.toFloat()
            val thumbW = thumbSize * aspect

            val cursorPos = ImGui.getCursorPos()
            ImGui.image(tex.rendererId.toLong(), thumbW, thumbSize, uvs[0], uvs[3], uvs[2], uvs[1])
            ImGui.setCursorPos(cursorPos)
            ImGui.invisibleButton("##frame_$frameIndex", thumbW, thumbSize)

            if (ImGui.beginDragDropSource()) {
                val sprite = Sprite(
                    spritesheet = SpritesheetHandle(absPath),
                    frameIndex  = frameIndex
                )
                val json = AssetSerializer.format.encodeToString(sprite)
                ImGui.setDragDropPayload(DnDPayload.SPRITE, json)
                ImGui.image(tex.rendererId.toLong(), thumbW, thumbSize, uvs[0], uvs[3], uvs[2], uvs[1])
                ImGui.sameLine()
                ImGui.text("Frame $frameIndex")
                ImGui.endDragDropSource()
            }

            if (ImGui.isItemHovered())
                ImGui.setTooltip("Frame $frameIndex")

            if ((idx + 1) % perRow != 0)
                ImGui.sameLine()

            ImGui.popID()
        }
    }
}
