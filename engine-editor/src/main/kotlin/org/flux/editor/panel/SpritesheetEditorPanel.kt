package org.flux.editor.panel

import imgui.ImGui
import imgui.flag.ImGuiKey
import org.flux.core.asset.AssetData
import org.flux.core.asset.AssetManager
import org.flux.core.asset.TextureHandle
import org.flux.core.asset.resolve
import org.flux.core.imgui.ImGuiEx
import org.flux.core.serialization.AssetSerializer
import org.flux.editor.util.DnDPayload
import org.flux.editor.util.NotificationModal
import java.io.File

class SpritesheetEditorPanel : EditorPanel("Spritesheet Editor") {

    private var currentPath: String? = null
    private var sheet: AssetData.Spritesheet? = null
    private var isDirty = false

    fun open(path: String) {
        currentPath = path
        sheet = runCatching {
            val json = File(path).readText()
            AssetSerializer.deserialize(json) as? AssetData.Spritesheet
        }.getOrElse {
            NotificationModal.error("Failed to load spritesheet: ${it.message}")
            null
        }
        isDirty = false
    }

    override fun drawContent() {
        ImGuiEx.window(title) {
            val s = sheet
            if (s == null) {
                ImGui.textDisabled("No spritesheet open. Double-click a spritesheet .asset file.")
                return@window
            }

            drawToolbar(s)
            ImGui.separator()

            val settingsPaneW = 250f
            if (ImGui.beginChild("##settings", settingsPaneW, 0f, true))
                drawSettings(s)
            ImGui.endChild()

            ImGui.sameLine()

            if (ImGui.beginChild("##preview", 0f, 0f, false))
                drawPreview(s)
            ImGui.endChild()
        }
    }

    private fun drawToolbar(s: AssetData.Spritesheet) {
        if (ImGui.button("Save") || (ImGui.isKeyDown(ImGuiKey.LeftCtrl) && ImGui.isKeyPressed(ImGuiKey.S)))
            save(s)

        if (isDirty) {
            ImGui.sameLine()
            ImGui.textColored(ImGui.colorConvertFloat4ToU32(1f, 0.6f, 0.2f, 1f), "*unsaved changes")
        }
    }

    private fun drawSettings(s: AssetData.Spritesheet) {
        var updated = s

        ImGui.text("Texture")
        ImGui.separator()

        val tex = runCatching { s.texture.resolve() }.getOrNull()
        if (tex != null) {
            ImGuiEx.imageFlipped(tex.rendererId, 48f, 48f)
            ImGui.sameLine()
            ImGui.text(File(s.texture.path).name)
        } else {
            ImGui.textDisabled("[None]")
            ImGui.sameLine()
            ImGui.text(if (s.texture.path.isEmpty()) "Drop texture here" else File(s.texture.path).name)
        }

        if (ImGui.beginDragDropTarget()) {
            val payload = ImGui.acceptDragDropPayload<String>(DnDPayload.TEXTURE)
            if (payload != null) {
                val relative = File(payload).relativeTo(File("").absoluteFile).path
                updated = updated.copy(texture = TextureHandle(relative))
                update(updated)
            }
            ImGui.endDragDropTarget()
        }

        ImGui.separator()
        ImGui.text("Cell Size")
        ImGui.separator()

        val maxW = tex?.width ?: 4096
        val maxH = tex?.height ?: 4096

        val cellW = intArrayOf(s.cellWidth)
        if (ImGui.dragInt("Cell Width", cellW, 1f, 1, maxW)) {
            updated = updated.copy(cellWidth = cellW[0])
            update(updated)
        }

        val cellH = intArrayOf(s.cellHeight)
        if (ImGui.dragInt("Cell Height", cellH, 1f, 1, maxH)) {
            updated = updated.copy(cellHeight = cellH[0])
            update(updated)
        }

        ImGui.separator()
        ImGui.text("Padding")
        ImGui.separator()

        val padX = intArrayOf(s.paddingX)
        if (ImGui.dragInt("Padding X", padX, 1f, 0, maxW)) {
            updated = updated.copy(paddingX = padX[0])
            update(updated)
        }

        val padY = intArrayOf(s.paddingY)
        if (ImGui.dragInt("Padding Y", padY, 1f, 0, maxH)) {
            updated = updated.copy(paddingY = padY[0])
            update(updated)
        }

        ImGui.separator()
        ImGui.text("Offset")
        ImGui.separator()

        val offX = intArrayOf(s.offsetX)
        if (ImGui.dragInt("Offset X", offX, 1f, 0, maxW)) {
            updated = updated.copy(offsetX = offX[0])
            update(updated)
        }

        val offY = intArrayOf(s.offsetY)
        if (ImGui.dragInt("Offset Y", offY, 1f, 0, maxH)) {
            updated = updated.copy(offsetY = offY[0])
            update(updated)
        }

        if (tex != null) {
            ImGui.separator()
            val cols = ((tex.width - s.offsetX) / (s.cellWidth + s.paddingX).coerceAtLeast(1)).coerceAtLeast(1)
            val rows = ((tex.height - s.offsetY) / (s.cellHeight + s.paddingY).coerceAtLeast(1)).coerceAtLeast(1)
            ImGui.textDisabled("$cols x $rows = ${cols * rows} frames")
        }
    }

    private fun drawPreview(s: AssetData.Spritesheet) {
        val tex = runCatching { s.texture.resolve() }.getOrNull()
        if (tex == null) {
            ImGui.textDisabled("No texture set")
            return
        }

        val availW = ImGui.getContentRegionAvailX()
        val availH = ImGui.getContentRegionAvailY()

        val aspect = tex.width.toFloat() / tex.height.toFloat()
        val displayW: Float
        val displayH: Float
        if (availW / aspect <= availH) {
            displayW = availW
            displayH = availW / aspect
        } else {
            displayH = availH
            displayW = availH * aspect
        }

        val cursorPos = ImGui.getCursorScreenPos()
        ImGuiEx.imageFlipped(tex.rendererId, displayW, displayH)

        val cols = ((tex.width - s.offsetX) / (s.cellWidth + s.paddingX).coerceAtLeast(1)).coerceAtLeast(1)
        val rows = ((tex.height - s.offsetY) / (s.cellHeight + s.paddingY).coerceAtLeast(1)).coerceAtLeast(1)

        val scaleX = displayW / tex.width
        val scaleY = displayH / tex.height

        val drawList = ImGui.getWindowDrawList()
        val cellColor = ImGui.colorConvertFloat4ToU32(1f, 1f, 0f, 0.6f)
        val paddingColor = ImGui.colorConvertFloat4ToU32(1f, 0.3f, 0.3f, 0.4f)

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x0 = cursorPos.x + (s.offsetX + col * (s.cellWidth + s.paddingX)) * scaleX
                val y0 = cursorPos.y + (s.offsetY + row * (s.cellHeight + s.paddingY)) * scaleY
                val x1 = x0 + s.cellWidth * scaleX
                val y1 = y0 + s.cellHeight * scaleY
                drawList.addRect(x0, y0, x1, y1, cellColor)

                val frameIndex = row * cols + col
                drawList.addText(
                    x0 + 5f, y0 + 5f,
                    ImGui.colorConvertFloat4ToU32(1f, 1f, 1f, 0.8f),
                    frameIndex.toString()
                )
            }
        }
    }

    private fun update(updated: AssetData.Spritesheet) {
        sheet = updated
        isDirty = true
    }

    private fun save(s: AssetData.Spritesheet) {
        val path = currentPath ?: return
        runCatching {
            File(path).writeText(AssetSerializer.serialize(s))
            AssetManager.invalidateAsset(path)
            isDirty = false
        }.onFailure {
            NotificationModal.error("Failed to save: ${it.message}")
        }
    }
}
