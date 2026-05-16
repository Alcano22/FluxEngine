package org.flux.editor.panel

import imgui.ImGui
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import org.flux.core.imgui.ImGuiEx
import org.flux.core.util.TaskManager

object StatusBar {

    const val HEIGHT = 26f

    fun render() {
        val vp = ImGui.getMainViewport()
        val x = vp.posX
        val y = vp.posY + vp.sizeY - HEIGHT
        val width = vp.sizeX

        ImGui.setNextWindowPos(x, y)
        ImGui.setNextWindowSize(width, HEIGHT)
        ImGui.setNextWindowViewport(vp.id)

        val flags = ImGuiWindowFlags.NoDecoration or
                    ImGuiWindowFlags.NoMove or
                    ImGuiWindowFlags.NoSavedSettings or
                    ImGuiWindowFlags.NoBringToFrontOnFocus or
                    ImGuiWindowFlags.NoNav or
                    ImGuiWindowFlags.NoScrollbar

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8f, 4f)

        ImGuiEx.window("##StatusBar", flags = flags) {
            val tasks = TaskManager.getTasks()
            if (tasks.isEmpty())
                ImGui.textDisabled("Ready")
            else {
                val task = tasks.first()

                val progress = task.progress
                if (progress != null)
                    ImGui.progressBar(progress, 150f, ImGui.getFrameHeight(), "")
                else {
                    val t = (ImGui.getTime() % 2.0).toFloat()
                    val ping = if (t < 1f) t else 2f - t
                    ImGui.progressBar(ping, 150f, ImGui.getFrameHeight(), "")
                }

                ImGui.sameLine()
                ImGui.text(task.label)
                if (tasks.size > 1) {
                    ImGui.sameLine()
                    ImGui.textDisabled("+${tasks.size - 1} more")
                }
            }
        }
        ImGui.popStyleVar(3)
    }
}
