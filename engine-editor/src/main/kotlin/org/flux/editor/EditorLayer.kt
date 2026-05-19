package org.flux.editor

import imgui.ImGui
import imgui.extension.imguizmo.ImGuizmo
import imgui.flag.ImGuiDockNodeFlags
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.flux.core.imgui.ImGuiEx
import org.flux.core.input.Input
import org.flux.core.layer.Layer
import org.flux.core.logging.logger
import org.flux.core.asset.TextureHandle
import org.flux.core.runtime.RuntimeState
import org.flux.core.scene.AnimationClip
import org.flux.core.scene.AnimationFrame
import org.flux.core.scene.CameraComponent
import org.flux.core.scene.PointLight2DComponent
import org.flux.core.scene.Scene
import org.flux.core.scene.SpriteAnimatorComponent
import org.flux.core.scene.SpriteRendererComponent
import org.flux.core.serialization.SceneSerializer
import org.flux.core.util.Color
import org.flux.core.util.MainThreadQueue
import org.flux.core.util.Timestep
import org.flux.editor.panel.*
import org.flux.editor.util.NotificationModal
import org.flux.editor.util.SelectionManager
import org.flux.scripting.compiler.ScriptCompiler
import org.flux.scripting.loader.ScriptLoader
import java.io.File

class EditorLayer : Layer("EditorLayer") {

    companion object {
        private val scriptsDir = File("Assets/Scripts")
        private val outputDir  = File(".flux/scripts/out")

        private val logger = logger()
    }

    private val editorManager = EditorManager()
    private val sceneContext = SceneContext(Scene())

    private val editorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isScriptingReady = false

    override fun onAttach() {
        setupEditor()
        initScripting { setupScene() }
    }

    private fun setupEditor() {
        editorManager.addPanel(SceneHierarchyPanel(sceneContext))
        editorManager.addPanel(InspectorPanel())
        editorManager.addPanel(ViewportPanel(sceneContext))
        editorManager.addPanel(ScenePanel(sceneContext))
        editorManager.addPanel(ConsolePanel())

        val animPanel = editorManager.addPanel(AnimationEditorPanel())
        val fileExplorerPanel = editorManager.addPanel(FileExplorerPanel())

        fileExplorerPanel.onAnimationOpen = { path ->
            animPanel.open(path)
            animPanel.requestFocus()
        }
    }

    private fun setupScene() {
        if (loadScene()) return

        sceneContext.scene.apply {
            createEntity("Main Camera").apply {
                addComponent(CameraComponent())
            }

            createEntity("Player").apply {
                transform.scale.set(3f, 3f, 1f)
                addComponent(SpriteRendererComponent().apply {
                    textureHandle = TextureHandle("Assets/Textures/player1.png")
                })
                addComponent(SpriteAnimatorComponent())
                ScriptLoader.instantiateOrNull("PlayerScript")?.let { addComponent(it) }
            }

            createEntity("Point Light").apply {
                addComponent(PointLight2DComponent(
                    intensity = 1.5f,
                    radius    = 3f,
                    color     = Color(1f, 0.9f, 0.7f)
                ))
            }
        }
    }

    private fun initScripting(onReady: () -> Unit) {
        scriptsDir.mkdirs()
        outputDir.mkdirs()

        val classpathJars = System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .map { File(it) }
            .filter { it.exists() }

        editorScope.launch {
            val success = ScriptCompiler.compile(
                scriptsDir    = scriptsDir,
                outputDir     = outputDir,
                classpathJars = classpathJars
            )

            if (success) {
                ScriptLoader.init(outputDir)
                MainThreadQueue.post(onReady)
                logger.info { "Scripting initialized - ${scriptsDir.absolutePath}" }
            } else
                logger.error { "Script compilation failed - check errors above" }

            isScriptingReady = success
        }
    }

    override fun onDetach() {
        editorScope.cancel()
        editorManager.dispose()
    }

    override fun onUpdate(ts: Timestep) {
        MainThreadQueue.flush()

        val viewportFocused = editorManager.getPanel<ViewportPanel>()?.isFocused ?: false
        Input.blocked = !viewportFocused || !sceneContext.isPlaying

        if (sceneContext.isPlaying)
            sceneContext.scene.onUpdate(ts)
        editorManager.onUpdate(ts)
    }

    override fun onRender() {
        editorManager.getPanel<ViewportPanel>()?.renderScene()
        editorManager.getPanel<ScenePanel>()?.renderScene()
    }

    override fun onImGuiRender() {
        drawDockSpace()

        ImGuizmo.beginFrame()

        ImGuiEx.mainMenuBar {
            menu("File") {
                menuItem("Save Scene") {
                    val json = SceneSerializer.serialize(sceneContext.scene)
                    File("test_scene.flux").writeText(json)
                }

                menuItem("Load Scene") {
                    loadScene()
                }
            }

            ImGui.separator()
            drawRuntimeToolbar()
        }

        editorManager.onImGuiRender()

        StatusBar.render()
        NotificationModal.render()
    }

    private fun loadScene(): Boolean {
        val file = File("test_scene.flux")
        if (file.exists()) {
            val loaded = SceneSerializer.deserialize(file.readText())
            sceneContext.replace(loaded)
            SelectionManager.clear()
            return true
        }
        return false
    }

    private fun drawDockSpace() {
        val vp = ImGui.getMainViewport()

        ImGui.setNextWindowPos(vp.posX, vp.posY)
        ImGui.setNextWindowSize(vp.sizeX, vp.sizeY - StatusBar.HEIGHT)
        ImGui.setNextWindowViewport(vp.id)

        val flags = ImGuiWindowFlags.NoCollapse or
                    ImGuiWindowFlags.NoResize or
                    ImGuiWindowFlags.NoScrollbar or
                    ImGuiWindowFlags.NoScrollWithMouse or
                    ImGuiWindowFlags.NoMove or
                    ImGuiWindowFlags.NoBringToFrontOnFocus or
                    ImGuiWindowFlags.NoNav or
                    ImGuiWindowFlags.NoSavedSettings

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0f)
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0f, 0f)
        ImGui.begin("##DockSpace", flags)
        ImGui.popStyleVar(3)
        ImGui.dockSpace(ImGui.getID("MainDockSpace"))
        ImGui.end()
    }

    private fun drawRuntimeToolbar() {
        val state = sceneContext.runtimeState
        when (state) {
            RuntimeState.STOPPED -> {
                ImGuiEx.disabled(!isScriptingReady) {
                    if (ImGui.button("Play")) {
                        sceneContext.play()
                        editorManager.getPanel<ViewportPanel>()?.requestFocus()
                    }
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