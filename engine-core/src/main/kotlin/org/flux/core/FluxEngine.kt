package org.flux.core

import org.flux.core.imgui.ImGuiLayer
import org.flux.core.imgui.ImGuiPlatformBackend
import org.flux.core.imgui.ImGuiRendererBackend
import org.flux.core.logging.EngineLogger
import org.flux.core.renderer.GraphicsFactory
import org.flux.core.renderer.Renderer
import org.flux.core.renderer.RendererAPI
import org.flux.core.logging.logger
import org.flux.core.window.Window

class EngineBuilder {

    companion object {
        private val logger = logger()
    }

    var window: Window? = null
    var rendererApi: RendererAPI? = null
    var graphicsFactory: GraphicsFactory? = null

    var imguiPlatformBackend: ImGuiPlatformBackend? = null
    var imguiRendererBackend: ImGuiRendererBackend? = null

    fun run(appFactory: (Window) -> Application) {
        val validWindow = window ?: throw logger.throwing(IllegalStateException("Window backend not configured"))
        val api = rendererApi ?: throw logger.throwing(IllegalStateException("Renderer backend not configured"))
        val factory = graphicsFactory ?: throw logger.throwing(IllegalStateException("Graphics factory not configured"))

        EngineLogger.attachBridge()

        Renderer.init(api, factory)

        val app = appFactory(validWindow)
        if (imguiPlatformBackend != null && imguiRendererBackend != null) {
            val imguiLayer = ImGuiLayer(imguiPlatformBackend!!, imguiRendererBackend!!)
            app.setImGuiLayer(imguiLayer)
        }
        app.run()
    }
}

object FluxEngine {
    fun builder() = EngineBuilder()
}
