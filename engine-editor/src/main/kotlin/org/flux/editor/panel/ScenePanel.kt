package org.flux.editor.panel

import imgui.ImGui
import imgui.extension.imguizmo.ImGuizmo
import imgui.extension.imguizmo.flag.Mode
import imgui.extension.imguizmo.flag.Operation
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiKey
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiWindowFlags
import org.flux.core.asset.AssetLocation
import org.flux.core.asset.AssetManager
import org.flux.core.imgui.ImGuiEx
import org.flux.core.input.Input
import org.flux.core.input.Key
import org.flux.core.renderer.*
import org.flux.core.scene.CameraComponent
import org.flux.core.scene.PointLight2DComponent
import org.flux.core.scene.SpriteRendererComponent
import org.flux.core.util.Color
import org.flux.editor.SceneContext
import org.flux.editor.util.SelectionManager
import org.joml.Math
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector2fc
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f

enum class GizmoOperation(val label: String, val imGuizmoOp: Int) {
    Translate("Translate", Operation.TRANSLATE),
    Rotate("Rotate", Operation.ROTATE),
    Scale("Scale", Operation.SCALE)
}

class ScenePanel(
    private val sceneContext: SceneContext
) : EditorPanel("Scene", noPadding = true) {

    val camera = OrthographicCamera(size = 5f, aspectRatio = 16f / 9f)

    private val framebuffer: Framebuffer

    private var isPanning = false
    private val lastMousePos = Vector2f()
    private val cameraPos = Vector2f()

    private val minZoom = 0.5f
    private val maxZoom = 50f

    var gizmoOperation = GizmoOperation.Translate

    private val viewMatArr = FloatArray(16)
    private val projMatArr = FloatArray(16)
    private val modelMatArr = FloatArray(16)
    private val translation = FloatArray(3)
    private val rotation = FloatArray(3)
    private val scale = FloatArray(3)

    private var imageScreenX = 0f
    private var imageScreenY = 0f
    private var imageWidth = 0f
    private var imageHeight = 0f

    private val cameraIcon = AssetManager.getTexture("textures/icon_camera.png", location = AssetLocation.INTERNAL)
    private val lightIcon = AssetManager.getTexture("textures/icon_light.png", location = AssetLocation.INTERNAL)

    init {
        val spec = FramebufferSpecification(
            width = 1920,
            height = 1080,
            attachments = FramebufferAttachmentSpecification(
                listOf(
                    FramebufferTextureSpecification(FramebufferTextureFormat.RGBA8),
                    FramebufferTextureSpecification(FramebufferTextureFormat.R32I),
                    FramebufferTextureSpecification(FramebufferTextureFormat.DEPTH24STENCIL8)
                )
            )
        )
        framebuffer = Framebuffer.create(spec)
    }

    fun renderScene() {
        framebuffer.bind()

        RenderCommand.setClearColor(0.15f, 0.15f, 0.15f, 1f)
        RenderCommand.clear()

        framebuffer.clearColorAttachmentInt(1, -1)

        framebuffer.setDrawBuffers(0)
        sceneContext.scene.onRenderWithCamera(camera)

        SelectionManager.selectedEntity
            ?.getComponent<SpriteRendererComponent>()
            ?.let { drawOutline(it.entity.transform.matrix) }

        RenderCommand.clear(ClearMask.DEPTH)

        framebuffer.setDrawBuffers(1)
        sceneContext.scene.onRenderEntityIDs(camera)

        framebuffer.setDrawBuffers(0, 1)
        framebuffer.unbind()
    }

    private fun drawOutline(transform: Matrix4f) {
        val outlineColor = Color(1f, 0.5f, 0f)
        val outlineThickness = 0.075f

        val currentScale = Vector3f()
        transform.getScale(currentScale)
        if (currentScale.x == 0f || currentScale.y == 0f) return

        val scaleFactorX = (currentScale.x + outlineThickness) / currentScale.x
        val scaleFactorY = (currentScale.y + outlineThickness) / currentScale.y

        RenderCommand.setStencilTest(true)
        RenderCommand.setStencilWrite(1)

        Renderer2D.beginScene(camera)
        Renderer2D.drawQuad(transform = transform, color = Color.White)
        Renderer2D.endScene()

        RenderCommand.setStencilDrawWhere(1)

        val scaledTransform = Matrix4f(transform).scale(scaleFactorX, scaleFactorY, 1f)
        Renderer2D.beginScene(camera)
        Renderer2D.drawQuad(transform = scaledTransform, color = outlineColor)
        Renderer2D.endScene()

        RenderCommand.setStencilMaskWrite(true)
        RenderCommand.clear(ClearMask.STENCIL)
        RenderCommand.setStencilTest(false)
    }

    override fun drawContent() {
        ImGuiEx.window(title, flags = ImGuiWindowFlags.MenuBar) {
            menuBar {
                GizmoOperation.entries.forEach { op ->
                    val isActive = gizmoOperation == op
                    if (isActive)
                        ImGui.pushStyleColor(ImGuiCol.Button, ImGui.colorConvertFloat4ToU32(0.26f, 0.59f, 0.98f, 1f))

                    if (ImGui.button(op.label))
                        gizmoOperation = op
                    if (isActive)
                        ImGui.popStyleColor()
                    ImGui.sameLine()
                }

                ImGui.separator()
                ImGui.sameLine()
                ImGui.textDisabled(
                    "  (%.1f, %.1f)  Zoom: %.2f  [MMB] Pan  [Scroll] Zoom  [F] Reset  [T/R/S] Gizmo"
                        .format(cameraPos.x, cameraPos.y, camera.size)
                )
            }

            val availSize = contentRegionAvail
            if (availSize.x > 0 && availSize.y > 0) {
                val newRatio = availSize.x / availSize.y
                if (newRatio != camera.aspectRatio) {
                    camera.aspectRatio = newRatio
                    camera.recalculateProjection()
                }
            }

            val screenPos = ImGui.getCursorScreenPos()
            imageScreenX = screenPos.x
            imageScreenY = screenPos.y
            imageWidth = availSize.x
            imageHeight = availSize.y

            imageFlipped(framebuffer.colorAttachmentRendererId, availSize.x, availSize.y)

            drawGizmo(imageScreenX, imageScreenY, imageWidth, imageHeight)

            val selectedEntity = SelectionManager.selectedEntity
            if (selectedEntity != null && selectedEntity.hasComponent<CameraComponent>())
                drawCameraFrustum(imageScreenX, imageScreenY, imageWidth, imageHeight)

            handleInput()
            renderEditorGizmos(imageScreenX, imageScreenY, imageWidth, imageHeight)
        }
    }

    private fun renderEditorGizmos(x: Float, y: Float, width: Float, height: Float) {
        val editorViewProj = camera.viewProjMatrix

        fun worldToScreen(worldPos: Vector3fc): Vector2fc? {
            val clipSpace = Vector4f(worldPos, 1f).mul(editorViewProj)
            val ndcX = clipSpace.x / clipSpace.w
            val ndcY = clipSpace.y / clipSpace.w

            val sx = (ndcX * 0.5f + 0.5f) * width + x
            val sy = (1f - (ndcY * 0.5f + 0.5f)) * height + y
            return Vector2f(sx, sy)
        }

        sceneContext.scene.findAllComponentsOfType<CameraComponent>().forEach { cam ->
            if (cam.camera != camera) {
                val screenPos = worldToScreen(cam.entity.transform.position)
                if (screenPos != null) {
                    ImGuiEx.iconButton(cameraIcon.rendererId, screenPos) {
                        SelectionManager.selected = cam.entity
                    }
                }
            }
        }

        sceneContext.scene.findAllComponentsOfType<PointLight2DComponent>().forEach { light ->
            val screenPos = worldToScreen(light.entity.transform.position)
            if (screenPos != null) {
                ImGuiEx.iconButton(lightIcon.rendererId, screenPos) {
                    SelectionManager.selected = light.entity
                }
            }
        }
    }

    private fun drawGizmo(x: Float, y: Float, width: Float, height: Float) {
        val entity = SelectionManager.selectedEntity ?: return

        ImGuizmo.setOrthographic(true)
        ImGuizmo.setDrawList()
        ImGuizmo.setRect(x, y, width, height)

        camera.viewMatrix.get(viewMatArr)
        camera.projMatrix.get(projMatArr)
        entity.transform.matrix.get(modelMatArr)

        ImGuizmo.manipulate(
            viewMatArr,
            projMatArr,
            gizmoOperation.imGuizmoOp,
            Mode.LOCAL,
            modelMatArr
        )

        if (ImGuizmo.isUsing()) {
            ImGuizmo.decomposeMatrixToComponents(modelMatArr, translation, rotation, scale)

            val t = entity.transform
            t.position.set(translation[0], translation[1], translation[2])
            t.rotation.set(
                Math.toRadians(rotation[0]),
                Math.toRadians(rotation[1]),
                Math.toRadians(rotation[2])
            )
            t.scale.set(scale[0], scale[1], scale[2])
            t.isDirty = true
        }
    }

    private fun drawCameraFrustum(x: Float, y: Float, width: Float, height: Float) {
        val camComponent = SelectionManager.selectedEntity?.getComponent<CameraComponent>() ?: return
        val gameCamera = camComponent.camera

        val ndcCorners = arrayOf(
            Vector4f(-1f, -1f, 0f, 1f),
            Vector4f( 1f, -1f, 0f, 1f),
            Vector4f( 1f,  1f, 0f, 1f),
            Vector4f(-1f,  1f, 0f, 1f)
        )

        val invViewProj = Matrix4f(gameCamera.viewProjMatrix).invert()
        val editorViewProj = camera.viewProjMatrix

        val screenPoints = ndcCorners.map { ndc ->
            val world = Vector4f(ndc).mul(invViewProj)
            world.div(world.w)

            val editorNdc = Vector4f(world).mul(editorViewProj)
            editorNdc.div(editorNdc.w)

            val sx = (editorNdc.x * 0.5f + 0.5f) * width + x
            val sy = (1f - (editorNdc.y * 0.5f + 0.5f)) * height + y
            Vector2f(sx, sy)
        }

        val drawList = ImGui.getWindowDrawList()
        val color = ImGui.colorConvertFloat4ToU32(1f, 0.85f, 0f, 1f)
        val thickness = 2f

        for (i in screenPoints.indices) {
            val a = screenPoints[i]
            val b = screenPoints[(i + 1) % screenPoints.size]
            drawList.addLine(a.x, a.y, b.x, b.y, color, thickness)
        }
    }

    private fun handleInput() {
        if (!ImGui.isWindowHovered()) return
        if (ImGuizmo.isUsing()) return

        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && !ImGuizmo.isOver()) {
            val mouseX = ImGui.getMousePosX()
            val mouseY = ImGui.getMousePosY()
            val relX = mouseX - imageScreenX
            val relY = imageHeight - (mouseY - imageScreenY)
            val fbWidth = framebuffer.specification.width
            val fbHeight = framebuffer.specification.height
            val pixelX = (relX / imageWidth * fbWidth).toInt()
            val pixelY = (relY / imageHeight * fbHeight).toInt()
            if (pixelX >= 0 && pixelY >= 0 && pixelX < fbWidth && pixelY < fbHeight) {
                val entityId = framebuffer.readPixel(1, pixelX, pixelY)
                if (entityId == -1)
                    SelectionManager.clear()
                else
                    SelectionManager.selected = sceneContext.scene.findEntityById(entityId)
            }
        }

        if (ImGui.isMouseClicked(ImGuiMouseButton.Middle)) {
            isPanning = true
            lastMousePos.set(ImGui.getMousePosX(), ImGui.getMousePosY())
        }
        if (ImGui.isMouseReleased(ImGuiMouseButton.Middle))
            isPanning = false

        if (isPanning) {
            val mouseX = ImGui.getMousePosX()
            val mouseY = ImGui.getMousePosY()
            val dx = (mouseX - lastMousePos.x) * camera.size * 0.003f
            val dy = (mouseY - lastMousePos.y) * camera.size * 0.003f
            lastMousePos.set(mouseX, mouseY)

            cameraPos.x -= dx
            cameraPos.y += dy
            camera.position = camera.position.set(cameraPos.x, cameraPos.y, 0f)
        }

        val scroll = ImGui.getIO().mouseWheel
        if (scroll != 0f) {
            camera.size = (camera.size - scroll * camera.size * 0.1f).coerceIn(minZoom, maxZoom)
            camera.recalculateProjection()
        }

        if (ImGui.isKeyPressed(ImGuiKey.F)) {
            cameraPos.set(0f, 0f)
            camera.position = camera.position.set(0f, 0f, 0f)
            camera.size = 5f
            camera.recalculateProjection()
        }

        if (ImGui.isKeyPressed(ImGuiKey.T))
            gizmoOperation = GizmoOperation.Translate
        if (ImGui.isKeyPressed(ImGuiKey.R))
            gizmoOperation = GizmoOperation.Rotate
        if (ImGui.isKeyPressed(ImGuiKey.S))
            gizmoOperation = GizmoOperation.Scale
    }

    override fun dispose() {
        framebuffer.dispose()
    }
}
