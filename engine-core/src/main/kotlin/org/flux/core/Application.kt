package org.flux.core

import org.flux.core.asset.AssetManager
import org.flux.core.event.Event
import org.flux.core.event.EventDispatcher
import org.flux.core.event.WindowCloseEvent
import org.flux.core.imgui.ImGuiLayer
import org.flux.core.input.Input
import org.flux.core.layer.Layer
import org.flux.core.layer.LayerStack
import org.flux.core.logging.EngineLogger
import org.flux.core.renderer.RenderCommand
import org.flux.core.renderer.Renderer
import org.flux.core.renderer.RendererAPI
import org.flux.core.util.Time
import org.flux.core.util.Timestep
import org.flux.core.window.Window

abstract class Application(val window: Window) {

    companion object {
        lateinit var instance: Application
            private set
    }

    private val layerStack = LayerStack()

    private var isRunning = true
    private var lastFrameTime = 0f

    private var imguiLayer: ImGuiLayer? = null

    private var eventQueue = mutableListOf<Event>()
    private var processingQueue = mutableListOf<Event>()

    init {
        instance = this

        window.eventCallback = { event ->
            synchronized(eventQueue) {
                eventQueue.add(event)
            }
        }
    }

    fun pushLayer(layer: Layer) = layerStack.pushLayer(layer)
    fun pushOverlay(overlay: Layer) = layerStack.pushOverlay(overlay)

    fun setImGuiLayer(layer: ImGuiLayer) {
        imguiLayer = layer
        pushOverlay(layer)
    }

    open fun onInit() {}

    open fun onUpdate() {}

    private fun processEvent(event: Event) {
        val dispatcher = EventDispatcher(event)
        dispatcher.dispatch<WindowCloseEvent> {
            close()
            true
        }

        Input.onEvent(event)

        layerStack.reversed().forEach { layer ->
            if (!event.isHandled)
                layer.onEvent(event)
        }
    }

    fun run() {
        onInit()

        while (isRunning) {
            val time = Time.time
            val timestep = Timestep(time - lastFrameTime)
            lastFrameTime = time

            synchronized(eventQueue) {
                val tmp = processingQueue
                processingQueue = eventQueue
                eventQueue = tmp
            }

            processingQueue.forEach { processEvent(it) }
            processingQueue.clear()

            layerStack.forEach { it.onUpdate(timestep) }
            layerStack.forEach { it.onRender() }

            imguiLayer?.use {
                layerStack.forEach { it.onImGuiRender() }
            }

            Input.endFrame()
            window.update()
        }

        Renderer.dispose()
        layerStack.clear()
        window.destroy()
        AssetManager.dispose()
    }

    fun close() {
        isRunning = false
    }
}
