package org.flux.editor.panel

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiSelectableFlags
import imgui.flag.ImGuiTableColumnFlags
import imgui.flag.ImGuiTableFlags
import org.flux.core.imgui.ImGuiEx
import org.flux.core.logging.LogBridge
import org.flux.core.logging.LogEntry
import org.flux.core.logging.LogLevel
import org.flux.core.logging.LogListener
import org.flux.editor.util.SelectionManager

class ConsolePanel : EditorPanel("Console") {

    companion object {
        private const val MAX_LOGS = 1000
    }

    private val logs = ArrayList<LogEntry>()
    private var autoScroll = true
    private var requestScrollToBottom = false

    private val logListener = LogListener { entry ->
        synchronized(logs) {
            logs.add(entry)
            if (logs.size > MAX_LOGS)
                logs.removeAt(0)
        }
        if (autoScroll)
            requestScrollToBottom = true
    }

    init {
        LogBridge.addListener(logListener)
    }

    override fun drawContent() {
        ImGuiEx.window(title) {
            if (ImGui.button("Clear")) {
                synchronized(logs) { logs.clear() }
                if (SelectionManager.selected is LogEntry)
                    SelectionManager.clear()
            }
            ImGui.sameLine()
            checkbox("Auto-Scroll", ::autoScroll)
            ImGui.separator()

            val tableFlags = ImGuiTableFlags.Resizable or
                    ImGuiTableFlags.Reorderable or
                    ImGuiTableFlags.Hideable or
                    ImGuiTableFlags.RowBg or
                    ImGuiTableFlags.ScrollY or
                    ImGuiTableFlags.BordersOuter

            if (ImGui.beginTable("ConsoleTable", 3, tableFlags)) {
                ImGui.tableSetupColumn("Time", ImGuiTableColumnFlags.WidthFixed, 80f)
                ImGui.tableSetupColumn("Logger", ImGuiTableColumnFlags.WidthFixed, 150f)
                ImGui.tableSetupColumn("Message", ImGuiTableColumnFlags.WidthStretch)
                ImGui.tableHeadersRow()

                synchronized(logs) {
                    for ((index, log) in logs.withIndex()) {
                        ImGui.tableNextRow()
                        ImGui.tableSetColumnIndex(0)

                        val isSelected = SelectionManager.selected === log
                        val flags = ImGuiSelectableFlags.SpanAllColumns or
                                    ImGuiSelectableFlags.AllowOverlap
                        if (ImGui.selectable("${log.timestamp}##log$index", isSelected, flags))
                            SelectionManager.selected = log

                        ImGui.tableSetColumnIndex(1)
                        ImGui.textUnformatted(log.loggerName.substringAfterLast('.'))

                        pushLevelColor(log.level)
                        ImGui.tableSetColumnIndex(2)
                        ImGui.textUnformatted(log.message)
                        ImGui.popStyleColor()
                    }
                }

                if (requestScrollToBottom) {
                    ImGui.setScrollHereY(1f)
                    requestScrollToBottom = false
                }

                ImGui.endTable()
            }
        }
    }

    private fun pushLevelColor(level: LogLevel) = when (level) {
        LogLevel.TRACE -> ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1f)
        LogLevel.DEBUG -> ImGui.pushStyleColor(ImGuiCol.Text, 0.7f, 0.7f, 0.7f, 1f)
        LogLevel.INFO  -> ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1f)
        LogLevel.WARN  -> ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.8f, 0.2f, 1f)
        LogLevel.ERROR -> ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.4f, 0.4f, 1f)
    }

    override fun dispose() {
        LogBridge.removeListener(logListener)
    }
}
