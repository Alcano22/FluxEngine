package org.flux.editor.util

import imgui.ImGui
import imgui.flag.ImGuiCond
import imgui.flag.ImGuiWindowFlags

enum class ModalLevel {
    INFO,
    WARNING,
    ERROR
}

object NotificationModal {

    private var pendingLevel: ModalLevel? = null
    private var pendingMessage: String? = null
    private var shouldOpen = false

    fun show(msg: String, level: ModalLevel) {
        pendingMessage = msg
        pendingLevel = level
        shouldOpen = true
    }

    fun info(msg: String)    = show(msg, ModalLevel.INFO)
    fun warning(msg: String) = show(msg, ModalLevel.WARNING)
    fun error(msg: String)   = show(msg, ModalLevel.ERROR)

    fun render() {
        if (shouldOpen) {
            ImGui.openPopup("##notification_modal")
            shouldOpen = false
        }

        val center = ImGui.getMainViewport().center
        ImGui.setNextWindowPos(center.x, center.y, ImGuiCond.Appearing, 0.5f, 0.5f)
        ImGui.setNextWindowSizeConstraints(300f, 0f, 600f, 400f)
        if (ImGui.beginPopupModal("##notification_modal", ImGuiWindowFlags.AlwaysAutoResize or ImGuiWindowFlags.NoTitleBar)) {
            val level = pendingLevel ?: ModalLevel.INFO
            val msg = pendingMessage ?: ""

            val (title, color) = when (level) {
                ModalLevel.INFO    -> "Info"    to ImGui.colorConvertFloat4ToU32(0.4f, 0.7f, 1.0f, 1f)
                ModalLevel.WARNING -> "Warning" to ImGui.colorConvertFloat4ToU32(1.0f, 0.8f, 0.0f, 1f)
                ModalLevel.ERROR   -> "Error"   to ImGui.colorConvertFloat4ToU32(1.0f, 0.3f, 0.3f, 1f)
            }

            ImGui.textColored(color, title)
            ImGui.spacing()

            ImGui.textWrapped(msg)
            if (ImGui.button("Copy"))
                ImGui.setClipboardText(msg)

            ImGui.separator()

            val buttonW = 120f
            ImGui.setCursorPosX(ImGui.getCursorPosX() + (ImGui.getContentRegionAvailX() - buttonW) * 0.5f)
            if (ImGui.button("OK", buttonW, 0f)) {
                ImGui.closeCurrentPopup()
                pendingMessage = null
                pendingLevel = null
            }

            ImGui.endPopup()
        }
    }
}
