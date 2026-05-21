package org.flux.editor.panel

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiKey
import imgui.type.ImBoolean
import imgui.type.ImString
import org.flux.core.asset.*
import org.flux.core.imgui.ImGuiEx
import org.flux.core.scene.AnimationClip
import org.flux.core.serialization.AssetSerializer
import org.flux.core.util.Timestep
import org.flux.editor.util.DnDPayload
import org.flux.editor.util.NotificationModal
import java.io.File

class AnimationEditorPanel : EditorPanel("Animation Editor") {

    private var currentPath: String? = null
    private var asset: AssetData.Animation? = null
    private var isDirty = false

    private var selectedClipIndex = -1
    private var selectedFrameIndex = -1

    private var isPreviewing = false
    private var previewTime = 0f

    private val selectedClip get() = asset?.clips?.getOrNull(selectedClipIndex)

    fun open(path: String) {
        currentPath = path
        asset = runCatching {
            val json = File(path).readText()
            AssetSerializer.deserialize(json) as? AssetData.Animation
                ?: AssetData.Animation()
        }.getOrElse {
            NotificationModal.error("Failed to load animation: ${it.message}")
            null
        }

        selectedClipIndex = -1
        selectedFrameIndex = -1
        isDirty = false
        isPreviewing = false
        previewTime = 0f
    }

    override fun onUpdate(ts: Timestep) {
        if (!isPreviewing) return
        val clip = selectedClip ?: return
        if (clip.frames.isEmpty()) return

        previewTime += ts.seconds
        val totalDuration = clip.frames.size / clip.fps
        if (previewTime >= totalDuration) {
            if (clip.loop)
                previewTime %= totalDuration
            else {
                previewTime = totalDuration - 1f / clip.fps
                isPreviewing = false
            }
        }
    }

    override fun drawContent() {
        ImGuiEx.window(title) {
            val a = asset
            if (a == null) {
                ImGui.textDisabled("No animation open. Double-click an animation .asset file.")
                return@window
            }

            drawToolbar(a)
            ImGui.separator()

            val clipPaneWidth = 180f
            if (ImGui.beginChild("##clips", clipPaneWidth, 0f, true))
                drawClipList(a)
            ImGui.endChild()

            ImGui.sameLine()

            if (ImGui.beginChild("##frames", 0f, 0f, false))
                drawFrameEditor(a)
            ImGui.endChild()
        }
    }

    private fun drawToolbar(a: AssetData.Animation) {
        if (ImGui.button("Save") || (ImGui.isKeyDown(ImGuiKey.LeftCtrl) && ImGui.isKeyPressed(ImGuiKey.S)))
            save(a)

        if (isDirty) {
            ImGui.sameLine()
            ImGui.textColored(ImGui.colorConvertFloat4ToU32(1f, 0.6f, 0.2f, 1f), "*unsaved changes")
        }
    }

    private fun drawClipList(a: AssetData.Animation) {
        ImGui.text("Clips")
        ImGui.separator()

        val clips = a.clips.toMutableList()
        var deletedIndex = -1

        clips.forEachIndexed { i, clip ->
            val selected = selectedClipIndex == i
            val displayName = clip.name.ifEmpty { "(unnamed)##$i" }
            if (ImGui.selectable(displayName, selected)) {
                selectedClipIndex = i
                selectedFrameIndex = -1
                isPreviewing = false
                previewTime = 0f
            }

            if (ImGui.beginPopupContextItem("##clipctx_$i")) {
                if (ImGui.menuItem("Delete"))
                    deletedIndex = i
                ImGui.endPopup()
            }
        }

        if (deletedIndex >= 0) {
            clips.removeAt(deletedIndex)
            asset = a.copy(clips = clips)
            if (selectedClipIndex >= clips.size)
                selectedClipIndex = clips.size - 1
            isDirty = true
        }

        ImGui.separator()
        if (ImGui.button("+ Add Clip")) {
            val updated = clips + AnimationClip(name = "New Clip")
            asset = a.copy(clips = updated)
            selectedClipIndex = updated.size - 1
            selectedFrameIndex = -1
            isDirty = true
        }
    }

    private fun drawFrameEditor(a: AssetData.Animation) {
        val clipIndex = selectedClipIndex
        if (clipIndex < 0 || clipIndex >= a.clips.size) {
            ImGui.textDisabled("Select a clip")
            return
        }

        val clip = a.clips[clipIndex]
        val frames = clip.frames.toMutableList()

        val nameBuffer = ImString(clip.name, 128)
        if (ImGui.inputText("Name##clip", nameBuffer))
            updateClip(a, clipIndex, clip.copy(name = nameBuffer.get()))

        val fpsBuffer = floatArrayOf(clip.fps)
        if (ImGui.dragFloat("FPS##clip", fpsBuffer, 0.5f, 1f, 120f))
            updateClip(a, clipIndex, clip.copy(fps = fpsBuffer[0]))

        val loopBuffer = ImBoolean(clip.loop)
        if (ImGui.checkbox("Loop##clip", loopBuffer))
            updateClip(a, clipIndex, clip.copy(loop = loopBuffer.get()))

        ImGui.sameLine()
        drawPreviewControls(clip)

        ImGui.separator()
        ImGui.text("Frames")

        var deletedIndex = -1

        frames.forEachIndexed { i, frame ->
            ImGui.pushID(i)
            val selected = selectedFrameIndex == i

            val label = when (frame) {
                is SpriteSource.FromTexture ->
                    "Frame $i  [${File(frame.handle.path).name}]"
                is SpriteSource.FromSprite ->
                    "Frame $i  [${File(frame.sprite.spritesheet.path).nameWithoutExtension} #${frame.sprite.frameIndex}]"
            }

            if (ImGui.selectable(label, selected))
                selectedFrameIndex = i

            if (ImGui.beginDragDropSource()) {
                ImGui.setDragDropPayload("ANIM_FRAME", i as Any)
                ImGui.text("Frame $i")
                ImGui.endDragDropSource()
            }

            if (ImGui.beginDragDropTarget()) {
                val payload = ImGui.acceptDragDropPayload<Int>("ANIM_FRAME")
                if (payload != null && payload != i) {
                    val moved = frames.removeAt(payload)
                    frames.add(i, moved)
                    updateClip(a, clipIndex, clip.copy(frames = frames))
                    selectedFrameIndex = i
                }
                ImGui.endDragDropTarget()
            }

            if (ImGui.beginPopupContextItem("##framectx_$i")) {
                if (ImGui.menuItem("Delete"))
                    deletedIndex = i
                ImGui.endPopup()
            }

            ImGui.popID()
        }

        if (deletedIndex >= 0) {
            frames.removeAt(deletedIndex)
            updateClip(a, clipIndex, clip.copy(frames = frames))
            if (selectedFrameIndex >= frames.size)
                selectedFrameIndex = frames.size - 1
        }

        ImGui.separator()

        val dropW = ImGui.getContentRegionAvailX()
        ImGui.pushStyleColor(ImGuiCol.Button,        0.0f, 0.0f, 0.0f, 0.0f)
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.2f, 0.2f, 0.2f, 0.3f)
        ImGui.pushStyleColor(ImGuiCol.ButtonActive,  0.2f, 0.2f, 0.2f, 0.5f)
        ImGui.button("Drop texture or sprite...", dropW, 32f)
        ImGui.popStyleColor(3)

        if (ImGui.beginDragDropTarget()) {
            ImGui.acceptDragDropPayload<String>(DnDPayload.TEXTURE)?.let { payload ->
                val relative = File(payload).relativeTo(File("").absoluteFile).path
                val updated = frames + SpriteSource.FromTexture(TextureHandle(relative))
                updateClip(a, clipIndex, clip.copy(frames = updated))
                selectedFrameIndex = updated.size - 1
            }
            ImGui.acceptDragDropPayload<String>(DnDPayload.SPRITE)?.let { payload ->
                runCatching {
                    val sprite = AssetSerializer.format.decodeFromString<Sprite>(payload)
                    val updated = frames + SpriteSource.FromSprite(sprite)
                    updateClip(a, clipIndex, clip.copy(frames = updated))
                    selectedFrameIndex = updated.size - 1
                }
            }
            ImGui.endDragDropTarget()
        }

        val selFrame = frames.getOrNull(selectedFrameIndex)
        if (selFrame != null) {
            ImGui.separator()
            ImGui.text("Frame $selectedFrameIndex")
            drawFrameDetail(a, clipIndex, clip, frames, selectedFrameIndex, selFrame)
        }

        drawPreviewThumbnail(clip)
    }

    private fun drawFrameDetail(
        a: AssetData.Animation,
        clipIndex: Int,
        clip: AnimationClip,
        frames: MutableList<SpriteSource>,
        frameIndex: Int,
        frame: SpriteSource
    ) {
        when (frame) {
            is SpriteSource.FromTexture -> {
                ImGui.text("Texture: ${File(frame.handle.path).name}")
                if (ImGui.beginDragDropTarget()) {
                    val payload = ImGui.acceptDragDropPayload<String>(DnDPayload.TEXTURE)
                    if (payload != null) {
                        val relative = File(payload).relativeTo(File("").absoluteFile).path
                        frames[frameIndex] = SpriteSource.FromTexture(TextureHandle(relative))
                        updateClip(a, clipIndex, clip.copy(frames = frames))
                    }
                    ImGui.endDragDropTarget()
                }

                val tex = runCatching { frame.handle.resolve() }.getOrNull()
                if (tex != null)
                    ImGuiEx.imageFlipped(tex.rendererId, 64f, 64f)
            }

            is SpriteSource.FromSprite -> {
                val sheet = runCatching { frame.sprite.spritesheet.resolve() }.getOrNull()
                ImGui.text("Spritesheet: ${File(frame.sprite.spritesheet.path).nameWithoutExtension}")
                ImGui.text("Frame Index: ${frame.sprite.frameIndex}")

                if (sheet != null) {
                    val tex = runCatching { sheet.texture.resolve() }.getOrNull()
                    if (tex != null) {
                        val cols = ((tex.width - sheet.offsetX) / (sheet.cellWidth + sheet.paddingX).coerceAtLeast(1)).coerceAtLeast(1)
                        val rows = ((tex.height - sheet.offsetY) / (sheet.cellHeight + sheet.paddingY).coerceAtLeast(1)).coerceAtLeast(1)
                        val totalFrames = (0 until rows).sumOf { row ->
                            (0 until cols).count { col ->
                                val x1 = sheet.offsetX + col * (sheet.cellWidth + sheet.paddingX) + sheet.cellWidth
                                val y1 = sheet.offsetY + row * (sheet.cellHeight + sheet.paddingY) + sheet.cellHeight
                                x1 <= tex.width && y1 <= tex.height
                            }
                        }

                        val indexBuffer = intArrayOf(frame.sprite.frameIndex)
                        if (ImGui.dragInt("Frame Index", indexBuffer, 1f, 0, totalFrames - 1)) {
                            frames[frameIndex] = SpriteSource.FromSprite(
                                frame.sprite.copy(frameIndex = indexBuffer[0])
                            )
                            updateClip(a, clipIndex, clip.copy(frames = frames))
                        }

                        val uvs = sheet.computeUVs(frame.sprite.frameIndex)
                        val displayH = 128f
                        val aspect = tex.width.toFloat() / tex.height.toFloat()
                        val displayW = displayH * aspect
                        ImGui.separator()
                        ImGui.image(
                            tex.rendererId.toLong(), displayW, displayH,
                            uvs[0], uvs[3], uvs[2], uvs[1]
                        )
                    }
                }
            }
        }
    }

    private fun drawPreviewControls(clip: AnimationClip) {
        if (clip.frames.isEmpty()) return

        if (isPreviewing) {
            if (ImGui.button("Stop##preview")) {
                isPreviewing = false
                previewTime = 0f
            }
        } else {
            if (ImGui.button("Preview##preview")) {
                isPreviewing = true
                previewTime = 0f
            }
        }
    }

    private fun drawPreviewThumbnail(clip: AnimationClip) {
        if (!isPreviewing || clip.frames.isEmpty()) return

        val frameIndex = (previewTime * clip.fps).toInt().coerceIn(0, clip.frames.size - 1)
        val frame = clip.frames[frameIndex]

        ImGui.separator()
        ImGui.text("Preview")

        when (frame) {
            is SpriteSource.FromTexture -> {
                val tex = runCatching { frame.handle.resolve() }.getOrNull() ?: return
                ImGuiEx.imageFlipped(tex.rendererId, 128f, 128f)
            }

            is SpriteSource.FromSprite -> {
                val sheet = runCatching { frame.sprite.spritesheet.resolve() }.getOrNull() ?: return
                val tex = runCatching { sheet.texture.resolve() }.getOrNull() ?: return
                val uvs = sheet.computeUVs(frame.sprite.frameIndex)
                val displayH = 128f
                val aspect = tex.width.toFloat() / tex.height.toFloat()
                val displayW = displayH * aspect
                ImGui.image(
                    tex.rendererId.toLong(), displayW, displayH,
                    uvs[0], uvs[3], uvs[2], uvs[1]
                )
            }
        }
    }

    private fun updateClip(a: AssetData.Animation, index: Int, newClip: AnimationClip) {
        val clips = a.clips.toMutableList()
        clips[index] = newClip
        asset = a.copy(clips = clips)
        isDirty = true
    }

    private fun save(a: AssetData.Animation) {
        val path = currentPath ?: return
        runCatching {
            File(path).writeText(AssetSerializer.serialize(a))
            AssetManager.invalidateAsset(path)
            isDirty = false
        }.onFailure {
            NotificationModal.error("Failed to save: ${it.message}")
        }
    }
}
