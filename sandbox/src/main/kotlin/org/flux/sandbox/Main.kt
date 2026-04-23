package org.flux.sandbox

import imgui.ImGui
import org.flux.core.Application
import org.flux.core.FluxEngine
import org.flux.core.asset.AssetManager
import org.flux.core.event.Event
import org.flux.core.event.EventDispatcher
import org.flux.core.event.WindowResizedEvent
import org.flux.core.imgui.ImGuiEx
import org.flux.core.layer.Layer
import org.flux.core.renderer.*
import org.flux.core.scene.CameraComponent
import org.flux.core.scene.CameraType
import org.flux.core.scene.Scene
import org.flux.core.scene.SpriteRendererComponent
import org.flux.core.util.Timestep
import org.flux.core.window.Window
import org.flux.glfw.useGLFW
import org.flux.opengl.useOpenGL
import org.joml.Vector4f

class SandboxLayer : Layer("Sandbox Layer") {

    private val scene = Scene()

    private val cameraEntity = scene.createEntity("Main Camera").apply {
        addComponent(CameraComponent(type = CameraType.ORTHOGRAPHIC))
        transform.position.z = 0f
    }

    private val playerEntity = scene.createEntity("Player").apply {
        transform.position.set(-1.5f, 0f, 0f)
        addComponent(SpriteRendererComponent(
            color = Vector4f(0.2f, 0.3f, 0.8f, 1f)
        ))
    }

    private val enemyEntity = scene.createEntity("Enemy").apply {
        transform.position.set(1.5f, 0f, 0f)
        addComponent(SpriteRendererComponent(
            texture = AssetManager.getTexture("assets/textures/netherrack.png", filter = TextureFilter.NEAREST)
        ))
    }

    override fun onUpdate(ts: Timestep) {
        scene.onUpdate(ts)
    }

    override fun onRender() {
        RenderCommand.setClearColor(0.1f, 0.1f, 0.1f, 1f)
        RenderCommand.clear()

        scene.onRender()
    }

    override fun onImGuiRender() {
        ImGuiEx.window("Settings") {
            treeNode("Camera") {
                val cam = cameraEntity.getComponent<CameraComponent>()!!

                if (ImGui.beginCombo("Type", cam.type.name)) {
                    for (type in CameraType.entries) {
                        if (ImGui.selectable(type.name, cam.type == type))
                            cam.type = type
                    }
                    ImGui.endCombo()
                }
                dragFloat3("Position", cameraEntity.transform.position, speed = 0.1f)
            }

            treeNode("Player") {
                dragFloat3("Position", playerEntity.transform.position, speed = 0.1f)
            }
        }

        ImGuiEx.window("Performance") {
            val fps = ImGui.getIO().framerate
            val frameTime = 1000f / fps

            ImGui.text("FPS: %.1f".format(fps))
            ImGui.text("Frame Time: %.3f ms".format(frameTime))

            ImGui.separatorText("Renderer2D Stats")
            val stats = Renderer2D.stats
            ImGui.text("Draw Calls: ${stats.drawCalls}")
            ImGui.text("Cubes: ${stats.quadCount}")
            ImGui.text("Vertices: ${stats.totalVertexCount}")
            ImGui.text("Indices: ${stats.totalIndexCount}")

            ImGui.separatorText("System Stats")
            ImGui.text("Device: ${RenderCommand.deviceName}")
        }
    }

    override fun onEvent(event: Event) {
        val dispatcher = EventDispatcher(event)
        dispatcher.dispatch<WindowResizedEvent> { e ->
            RenderCommand.setViewport(0, 0, e.width, e.height)

            scene.onViewportResize(e.width, e.height)
            true
        }
    }
}

class SandboxApp(window: Window) : Application(window) {

    override fun onInit() {
        pushLayer(SandboxLayer())
    }
}

fun main() {
    try {
        System.loadLibrary("nvapi64")
    } catch (_: Exception) {}

    FluxEngine.builder()
        .useGLFW(width = 1280, height = 720, title = "Flux Sandbox")
        .useOpenGL()
        .run(::SandboxApp)
}
