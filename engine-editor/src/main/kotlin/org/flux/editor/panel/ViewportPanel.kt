package org.flux.editor.panel

import imgui.ImGui
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiWindowFlags
import org.flux.core.imgui.ImGuiEx
import org.flux.core.renderer.Framebuffer
import org.flux.core.renderer.FramebufferAttachmentSpecification
import org.flux.core.renderer.FramebufferSpecification
import org.flux.core.renderer.FramebufferTextureFormat
import org.flux.core.renderer.FramebufferTextureSpecification
import org.flux.core.renderer.RenderCommand
import org.flux.core.scene.Scene
import org.flux.editor.SceneContext

enum class AspectRatio(val ratio: Float, val displayName: String) {
    RATIO_16_9(16f / 9f, "16:9"),
    RATIO_16_10(16f / 10f, "16:10"),
    RATIO_21_9(21f / 9f, "21:9"),
    RATIO_4_3(4f / 3f, "4:3")
}

class ViewportPanel(private val sceneContext: SceneContext) : EditorPanel("Viewport", noPadding = true) {

    private val framebuffer: Framebuffer
    private val targetWidth = 1920f
    private val targetHeight = 1080f

    var selectedAspectRatio = AspectRatio.RATIO_16_9

    var isFocused = false
        private set

    init {
        val spec = FramebufferSpecification(
            width = targetWidth.toInt(),
            height = targetHeight.toInt(),
            attachments = FramebufferAttachmentSpecification(
                listOf(
                    FramebufferTextureSpecification(FramebufferTextureFormat.RGBA8),
                    FramebufferTextureSpecification(FramebufferTextureFormat.DEPTH24STENCIL8)
                )
            )
        )
        framebuffer = Framebuffer.create(spec)

        sceneContext.onSceneChange { newScene ->
            newScene.onViewportResize(targetWidth.toInt(), targetHeight.toInt())
        }
    }

    fun renderScene() {
        framebuffer.bind()

        RenderCommand.setClearColor(0.1f, 0.1f, 0.1f, 1f)
        RenderCommand.clear()

        sceneContext.scene.onRender()

        framebuffer.unbind()
    }

    override fun drawContent() {
        ImGuiEx.window(title, flags = ImGuiWindowFlags.MenuBar) {
            isFocused = ImGui.isWindowFocused()

            menuBar {
                itemWidth(100f) {
                    enumCombo(
                        label = "##Aspect",
                        property = ::selectedAspectRatio,
                        nameSelector = { it.displayName }
                    )
                }
            }

            val availSize = contentRegionAvail
            val currentRatio = selectedAspectRatio.ratio

            var displayWidth = availSize.x
            var displayHeight = displayWidth / currentRatio
            if (displayHeight > availSize.y) {
                displayHeight = availSize.y
                displayWidth = displayHeight * currentRatio
            }

            val offsetX = (availSize.x - displayWidth) * 0.5f
            val offsetY = (availSize.y - displayHeight) * 0.5f
            setCursorPos(offsetX, cursorPosY + offsetY)

            imageFlipped(framebuffer.colorAttachmentRendererId, displayWidth, displayHeight)
        }
    }

    override fun dispose() {
        framebuffer.dispose()
    }
}
