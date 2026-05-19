package org.flux.editor.panel

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiKey
import imgui.type.ImBoolean
import imgui.type.ImString
import org.flux.core.asset.AnimationAsset
import org.flux.core.asset.AssetManager
import org.flux.core.asset.TextureHandle
import org.flux.core.imgui.ImGuiEx
import org.flux.core.scene.AnimationClip
import org.flux.core.scene.AnimationFrame
import org.flux.core.serialization.AnimationSerializer
import org.flux.core.util.Timestep
import org.flux.editor.util.DnDPayload
import org.flux.editor.util.NotificationModal
import java.io.File

class AnimationEditorPanel : EditorPanel("Animation Editor") {

    private var currentPath: String? = null
    private var asset: AnimationAsset? = null
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
            AnimationSerializer.deserialize(json)
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
                ImGui.textDisabled("No animation open. Double-click a .anim file.")
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

    private fun drawToolbar(a: AnimationAsset) {
        if (ImGui.button("Save") || (ImGui.isKeyDown(ImGuiKey.LeftCtrl) && ImGui.isKeyPressed(ImGuiKey.S)))
            save(a)

        if (isDirty) {
            ImGui.sameLine()
            ImGui.textColored(ImGui.colorConvertFloat4ToU32(1f, 0.6f, 0.2f, 1f), "*unsaved changes")
        }
    }

    private fun drawClipList(a: AnimationAsset) {
        ImGui.text("Clips")
        ImGui.separator()

        val clips = a.clips.toMutableList()
        var deletedIndex = -1

        clips.forEachIndexed { i, clip ->
            val selected = selectedClipIndex == i
            val displayName = clip.name.ifEmpty { "##clip_$i" }
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
            val newClip = AnimationClip(name = "New Clip")
            val updated = clips + newClip
            asset = a.copy(clips = updated)
            selectedClipIndex = updated.size - 1
            selectedFrameIndex = -1
            isDirty = true
        }
    }

    private fun drawFrameEditor(a: AnimationAsset) {
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
        ImGui.text("Spritesheet")
        val handle = clip.textureHandle
        if (handle != null) {
            val tex = runCatching { handle.texture }.getOrNull()
            if (tex != null) {
                ImGuiEx.imageFlipped(tex.rendererId, 48f, 48f)
                ImGui.sameLine()
            }
            ImGui.text(File(handle.path).name)
        } else
            ImGui.textDisabled("[None]  (needed for Sheet Frames)")

        if (ImGui.beginDragDropTarget()) {
            val payload = ImGui.acceptDragDropPayload<String>(DnDPayload.TEXTURE)
            if (payload != null) {
                val relative = File(payload).relativeTo(File("").absoluteFile).path
                updateClip(a, clipIndex, clip.copy(textureHandle = TextureHandle(relative)))
            }
            ImGui.endDragDropTarget()
        }

        if (clip.textureHandle != null) {
            ImGui.sameLine()
            if (ImGui.smallButton("Clear##sheet"))
                updateClip(a, clipIndex, clip.copy(textureHandle = null))
        }

        ImGui.separator()
        ImGui.text("Frames")

        var deletedIndex = -1

        frames.forEachIndexed { i, frame ->
            val selected = selectedFrameIndex == i
            ImGui.pushID(i)

            val label = when (frame) {
                is AnimationFrame.TextureFrame -> "Frame $i  [${File(frame.handle.path).name}]"
                is AnimationFrame.SheetFrame   -> "Frame $i  [Sheet: ${frame.u0}..${frame.u1}]"
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

        val availW = ImGui.getContentRegionAvailX()
        val buttonW = (availW - ImGui.getStyle().itemSpacingX) * 0.5f

        ImGui.pushStyleColor(ImGuiCol.Button,        0f, 0f, 0f, 0f)
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.2f, 0.2f, 0.2f, 0.3f)
        ImGui.pushStyleColor(ImGuiCol.ButtonActive,  0.2f, 0.2f, 0.2f, 0.5f)
        ImGui.button("Drop texture here...", buttonW, 32f)
        ImGui.popStyleColor(3)

        if (ImGui.beginDragDropTarget()) {
            val payload = ImGui.acceptDragDropPayload<String>(DnDPayload.TEXTURE)
            if (payload != null) {
                val relative = File(payload).relativeTo(File("").absoluteFile).path
                val newFrame = AnimationFrame.TextureFrame(TextureHandle(relative))
                val updated = frames + newFrame
                updateClip(a, clipIndex, clip.copy(frames = updated))
                selectedFrameIndex = updated.size - 1
            }
            ImGui.endDragDropTarget()
        }

        ImGui.sameLine()
        if (ImGui.button("+ Sheet Frame", buttonW, 32f)) {
            val newFrame = AnimationFrame.SheetFrame(0f, 0f, 1f, 1f)
            val updated = frames + newFrame
            updateClip(a, clipIndex, clip.copy(frames = updated))
            selectedFrameIndex = updated.size - 1
            isDirty = true
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
        a: AnimationAsset,
        clipIndex: Int,
        clip: AnimationClip,
        frames: MutableList<AnimationFrame>,
        frameIndex: Int,
        frame: AnimationFrame
    ) {
        when (frame) {
            is AnimationFrame.TextureFrame -> {
                ImGui.text("Texture: ${File(frame.handle.path).name}")
                if (ImGui.beginDragDropTarget()) {
                    val payload = ImGui.acceptDragDropPayload<String>(DnDPayload.TEXTURE)
                    if (payload != null) {
                        val relative = File(payload).relativeTo(File("").absoluteFile).path
                        frames[frameIndex] = AnimationFrame.TextureFrame(TextureHandle(relative))
                        updateClip(a, clipIndex, clip.copy(frames = frames))
                    }
                    ImGui.endDragDropTarget()
                }

                val tex = runCatching { frame.handle.texture }.getOrNull()
                if (tex != null)
                    ImGuiEx.imageFlipped(tex.rendererId, 64f, 64f)
            }
            is AnimationFrame.SheetFrame -> {
                val u0 = floatArrayOf(frame.u0)
                val v0 = floatArrayOf(frame.v0)
                val u1 = floatArrayOf(frame.u1)
                val v1 = floatArrayOf(frame.v1)

                ImGui.setNextItemWidth(50f)
                var changed = ImGui.dragFloat("U0", u0, 0.01f, 0f, 1f)
                ImGui.sameLine()
                ImGui.setNextItemWidth(50f)
                changed = changed || ImGui.dragFloat("##U1", u1, 0.01f, 0f, 1f)
                ImGui.sameLine()
                ImGui.text("U1")

                ImGui.setNextItemWidth(50f)
                changed = changed || ImGui.dragFloat("V0", v0, 0.01f, 0f, 1f)
                ImGui.sameLine()
                ImGui.setNextItemWidth(50f)
                changed = changed || ImGui.dragFloat("##V1", v1, 0.01f, 0f, 1f)
                ImGui.sameLine()
                ImGui.text("V1")

                if (changed) {
                    frames[frameIndex] = AnimationFrame.SheetFrame(u0[0], v0[0], u1[0], v1[0])
                    updateClip(a, clipIndex, clip.copy(frames = frames))
                }

                val sheetTex = runCatching { clip.textureHandle?.texture }.getOrNull()
                if (sheetTex != null) {
                    ImGui.separator()
                    ImGui.image(
                        sheetTex.rendererId.toLong(), 128f, 128f,
                        frame.u0, frame.v1, frame.u1, frame.v0
                    )
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
            is AnimationFrame.TextureFrame -> {
                val tex = runCatching { frame.handle.texture }.getOrNull() ?: return
                ImGuiEx.imageFlipped(tex.rendererId, 128f, 128f)
            }
            is AnimationFrame.SheetFrame -> {
                val tex = runCatching { clip.textureHandle?.texture }.getOrNull() ?: run {
                    ImGui.textDisabled("No spritesheet set")
                    return
                }
                ImGui.image(
                    tex.rendererId.toLong(), 128f, 128f,
                    frame.u0, frame.v1, frame.u1, frame.v0
                )
            }
        }
    }

    private fun updateClip(a: AnimationAsset, index: Int, newClip: AnimationClip) {
        val clips = a.clips.toMutableList()
        clips[index] = newClip
        asset = a.copy(clips = clips)
        isDirty = true
    }

    private fun save(a: AnimationAsset) {
        val path = currentPath ?: return
        runCatching {
            File(path).writeText(AnimationSerializer.serialize(a))
            AssetManager.invalidateAnimation(path)
            isDirty = false
        }.onFailure {
            NotificationModal.error("Failed to save: ${it.message}")
        }
    }
}
