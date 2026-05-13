package org.flux.editor

import imgui.ImGui
import org.flux.core.imgui.ImGuiEx
import org.flux.core.layer.Layer
import org.flux.core.scene.CameraComponent
import org.flux.core.scene.Scene
import org.flux.core.scene.SpriteRendererComponent
import org.flux.core.serialization.SceneSerializer
import org.flux.core.util.Timestep
import org.flux.editor.panel.EditorManager
import org.flux.editor.panel.InspectorPanel
import org.flux.editor.panel.SceneHierarchyPanel
import org.flux.editor.panel.ViewportPanel
import org.flux.editor.util.SelectionManager
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
        }

        editorManager.addPanel(SceneHierarchyPanel(sceneContext))
        editorManager.addPanel(InspectorPanel())
        editorManager.addPanel(ViewportPanel(sceneContext))
    }

    override fun onDetach() {
        editorManager.dispose()
    }

    override fun onUpdate(ts: Timestep) {
        sceneContext.scene.onUpdate(ts)
        editorManager.onUpdate(ts)
    }

    override fun onRender() {
        editorManager.getPanel<ViewportPanel>()?.renderScene()
    }

    override fun onImGuiRender() {
        ImGui.dockSpaceOverViewport()

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
        }

        editorManager.onImGuiRender()
    }
}