package org.flux.core.imgui

import imgui.ImFontConfig
import imgui.ImGui
import imgui.flag.ImGuiConfigFlags
import org.flux.core.asset.AssetLocation
import org.flux.core.asset.AssetManager
import org.flux.core.layer.Layer

class ImGuiLayer(
    private val platformBackend: ImGuiPlatformBackend,
    private val rendererBackend: ImGuiRendererBackend
) : Layer("ImGuiLayer") {

    override fun onAttach() {
        ImGui.createContext()

        val io = ImGui.getIO()
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard)
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable)
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable)

        setupFonts()

        platformBackend.init()
        rendererBackend.init()
    }

    private fun setupFonts() {
        val io = ImGui.getIO()
        val fontCfg = ImFontConfig().apply {
            pixelSnapH = true
        }

        try {
            val fontData = AssetManager.getFont("fonts/Inter_18pt-Regular.ttf", location = AssetLocation.INTERNAL)
            io.fonts.addFontFromMemoryTTF(fontData, 18f, fontCfg)
            io.fonts.build()
        } catch (e: Exception) {
            println("Warning: Could not load font, falling back to default. Error: ${e.message}")
        } finally {
            fontCfg.destroy()
        }
    }

    override fun onDetach() {
        rendererBackend.dispose()
        platformBackend.dispose()
        ImGui.destroyContext()
    }

    fun use(block: () -> Unit) {
        begin()
        try {
            block()
        } finally {
            end()
        }
    }

    fun begin() {
        platformBackend.newFrame()
        rendererBackend.newFrame()
        ImGui.newFrame()
    }

    fun end() {
        val io = ImGui.getIO()
        ImGui.render()

        rendererBackend.renderDrawData()

        if (io.hasConfigFlags(ImGuiConfigFlags.ViewportsEnable))
            platformBackend.updateViewports()
    }
}
