package org.flux.editor

import imgui.ImGui
import imgui.extension.imguizmo.ImGuizmo
import org.flux.core.imgui.ImGuiEx
import org.flux.core.layer.Layer
import org.flux.core.logging.logger
import org.flux.core.runtime.RuntimeState
import org.flux.core.scene.CameraComponent
import org.flux.core.scene.PointLight2DComponent
import org.flux.core.scene.Scene
import org.flux.core.scene.SpriteRendererComponent
import org.flux.core.serialization.SceneSerializer
import org.flux.core.util.Color
import org.flux.core.util.Timestep
import org.flux.editor.panel.ConsolePanel
import org.flux.editor.panel.EditorManager
import org.flux.editor.panel.InspectorPanel
import org.flux.editor.panel.SceneHierarchyPanel
import org.flux.editor.panel.ScenePanel
import org.flux.editor.panel.ViewportPanel
import org.flux.editor.util.SelectionManager
import org.flux.scripting.LuaScriptComponent
import org.joml.Vector3f
import java.io.File

class EditorLayer : Layer("EditorLayer") {

    private val editorManager = EditorManager()
    private val sceneContext = SceneContext(Scene())

    override fun onAttach() {
        sceneContext.scene.createEntity("Main Camera").apply {
            addComponent(CameraComponent())
        }
        sceneContext.scene.createEntity("Player").apply {
            addComponent(SpriteRendererComponent().apply {
                color.set(0.25f, 0.88f, 0.82f, 1f)
            })
            addComponent(LuaScriptComponent("assets/scripts/player.lua"))
        }

        sceneContext.scene.createEntity("Point Light").apply {
            addComponent(PointLight2DComponent(
                intensity = 1.5f,
                radius    = 3f,
                color     = Color(1f, 0.9f, 0.7f)
            ))
            transform.position.set(1f, 0f, 0f)
        }

        editorManager.addPanel(SceneHierarchyPanel(sceneContext))
        editorManager.addPanel(InspectorPanel())
        editorManager.addPanel(ViewportPanel(sceneContext))
        editorManager.addPanel(ScenePanel(sceneContext))
        editorManager.addPanel(ConsolePanel())
    }

    override fun onDetach() {
        editorManager.dispose()
    }

    override fun onUpdate(ts: Timestep) {
        if (sceneContext.isPlaying)
            sceneContext.scene.onUpdate(ts)
        editorManager.onUpdate(ts)
    }

    override fun onRender() {
        editorManager.getPanel<ViewportPanel>()?.renderScene()
        editorManager.getPanel<ScenePanel>()?.renderScene()
    }

    override fun onImGuiRender() {
        ImGui.dockSpaceOverViewport()

        ImGuizmo.beginFrame()

        ImGuiEx.mainMenuBar {
            menu("File") {
                menuItem("Save Scene") {
                    val json = SceneSerializer.serialize(sceneContext.scene)
                    File("test_scene.flux").writeText(json)
                }

                menuItem("Load Scene") {
                    val file = File("test_scene.flux")
                    if (file.exists()) {
                        val loaded = SceneSerializer.deserialize(file.readText())
                        sceneContext.replace(loaded)
                        SelectionManager.clear()
                    }
                }
            }

            ImGui.separator()
            drawRuntimeToolbar()
        }

        editorManager.onImGuiRender()
    }

    private fun drawRuntimeToolbar() {
        val state = sceneContext.runtimeState
        when (state) {
            RuntimeState.STOPPED -> {
                if (ImGui.button("Play")) {
                    sceneContext.play()
                    editorManager.getPanel<ViewportPanel>()?.requestFocus()
                }
            }
            RuntimeState.PLAYING -> {
                if (ImGui.button("Pause"))
                    sceneContext.pause()
                ImGui.sameLine()
                if (ImGui.button("Stop"))
                    sceneContext.stop()
            }
            RuntimeState.PAUSED -> {
                if (ImGui.button("Resume"))
                    sceneContext.resume()
                ImGui.sameLine()
                if (ImGui.button("Stop"))
                    sceneContext.stop()
            }
        }

        ImGui.sameLine()
        val (label, color) = when (state) {
            RuntimeState.STOPPED -> "STOPPED" to ImGui.colorConvertFloat4ToU32(0.5f, 0.5f, 0.5f, 1f)
            RuntimeState.PLAYING -> "PLAYING" to ImGui.colorConvertFloat4ToU32(0.2f, 0.8f, 0.2f, 1f)
            RuntimeState.PAUSED  -> "PAUSED"  to ImGui.colorConvertFloat4ToU32(0.9f, 0.7f, 0.1f, 1f)
        }
        ImGui.textColored(color, label)
    }
}