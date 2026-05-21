package org.flux.editor.panel

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiInputTextFlags
import imgui.flag.ImGuiKey
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiPopupFlags
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiTableFlags
import imgui.flag.ImGuiTreeNodeFlags
import imgui.type.ImString
import org.flux.core.asset.AssetData
import org.flux.core.asset.AssetLocation
import org.flux.core.asset.AssetManager
import org.flux.core.asset.TextureHandle
import org.flux.core.imgui.ImGuiEx
import org.flux.core.renderer.TextureFilter
import org.flux.core.serialization.AssetSerializer
import org.flux.editor.util.DnDPayload
import org.flux.editor.util.NotificationModal
import org.flux.editor.util.SelectionManager
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo
import kotlin.math.floor
import kotlin.math.max

class FileExplorerPanel(
    private val root: Path = Path("Assets")
) : EditorPanel("File Explorer") {

    companion object {
        private const val LEFT_PANE_WIDTH = 200f
        private const val THUMB_SIZE      = 64f
        private const val CELL_PADDING    = 12f
        private const val CELL_WIDTH      = THUMB_SIZE + CELL_PADDING
        private const val LABEL_MAX_CHARS = 20
    }

    private var currentDir: Path = root
    private var selectedEntry: Path? = null

    private var renamingEntry: Path? = null
    private var renameBuffer = ImString(256)

    var onAnimationOpen: ((String) -> Unit)? = null
    var onSpritesheetOpen: ((String) -> Unit)? = null

    override fun drawContent() {
        ImGuiEx.window(title) {
            if (ImGui.beginChild("##left", LEFT_PANE_WIDTH, 0f, true))
                renderTree()
            ImGui.endChild()

            ImGui.sameLine()

            if (ImGui.beginChild("##right", 0f, 0f, false))
                renderRightPane()
            ImGui.endChild()
        }
    }

    private fun renderTree() {
        val flags = ImGuiTreeNodeFlags.OpenOnArrow or
                    ImGuiTreeNodeFlags.DefaultOpen or
                    ImGuiTreeNodeFlags.SpanFullWidth or
                    if (currentDir == root) ImGuiTreeNodeFlags.Selected else 0

        val open = ImGui.treeNodeEx("assets", flags)
        if (ImGui.isItemClicked())
            selectDir(root)
        if (open) {
            drawDirChildren(root)
            ImGui.treePop()
        }
    }

    private fun drawDirChildren(dir: Path) {
        listChildren(dir, onlyDirs = true).forEach { child ->
            ImGui.pushID(child.toString())
            val hasChildren = hasSubDirs(child)
            val flags = ImGuiTreeNodeFlags.OpenOnArrow or
                        ImGuiTreeNodeFlags.SpanFullWidth or
                        (if (currentDir == child) ImGuiTreeNodeFlags.Selected else 0) or
                        (if (!hasChildren) ImGuiTreeNodeFlags.Leaf or ImGuiTreeNodeFlags.NoTreePushOnOpen else 0)

            val opened = ImGui.treeNodeEx(child.name, flags)
            if (ImGui.isItemClicked())
                selectDir(child)
            if (opened && hasChildren) {
                drawDirChildren(child)
                ImGui.treePop()
            }
            ImGui.popID()
        }
    }

    private fun renderRightPane() {
        renderBreadcrumbs()
        ImGui.separator()

        val footerH = ImGui.getTextLineHeightWithSpacing() +
                      ImGui.getStyle().framePaddingY * 2f +
                      ImGui.getStyle().itemSpacingY * 2f

        if (ImGui.beginChild("##grid_area", 0f, -footerH, false))
            renderGrid()
        ImGui.endChild()

        ImGui.separator()
        renderFooter()
    }

    private fun renderBreadcrumbs() {
        if (ImGui.smallButton("..")) {
            val parent = currentDir.parent
            if (parent != null && isUnderRoot(parent))
                selectDir(parent)
        }

        ImGui.sameLine()

        if (ImGui.smallButton("assets"))
            selectDir(root)

        val rel = runCatching { root.relativize(currentDir) }.getOrNull()
        if (rel != null && rel.toString().isNotEmpty()) {
            var pathSoFar = root
            rel.forEach { seg ->
                ImGui.sameLine()
                ImGui.textDisabled("/")
                ImGui.sameLine()
                pathSoFar = pathSoFar.resolve(seg)
                val captured = pathSoFar
                if (ImGui.smallButton(seg.toString()))
                    selectDir(captured)
            }
        }
    }

    private fun renderGrid() {
        val entries = listChildren(currentDir, onlyDirs = false)
            .filter { it.extension != "meta" }
            .sortedWith(compareBy<Path> { !it.isDirectory() }.thenBy { it.name.lowercase() })

        val availX = ImGui.getContentRegionAvailX()
        val perRow = max(1, floor((availX + CELL_PADDING) / CELL_WIDTH).toInt())
        val flags = ImGuiTableFlags.SizingStretchSame or ImGuiTableFlags.NoPadOuterX

        if (ImGui.beginTable("##grid", perRow, flags)) {
            entries.forEachIndexed { i, entry ->
                ImGui.tableNextColumn()
                ImGui.pushID(i)
                drawCell(entry)
                ImGui.popID()
            }
            ImGui.endTable()
        }

        if (ImGui.beginPopupContextWindow(
            "##grid_ctx",
            ImGuiPopupFlags.MouseButtonRight or ImGuiPopupFlags.NoOpenOverItems
        )) {
            if (ImGui.beginMenu("New")) {
                if (ImGui.menuItem("Folder"))
                    createFolder(currentDir)
                if (ImGui.menuItem("Animation"))
                    createAnimationFile(currentDir)

                val sel = selectedEntry
                if (sel != null && !sel.isDirectory()) {
                    val ext = sel.extension.lowercase()
                    if (ext == "png" || ext == "jpg" || ext == "jpeg") {
                        ImGui.separator()
                        if (ImGui.menuItem("Spritesheet (${sel.nameWithoutExtension})"))
                            createSpritesheetFile(currentDir, sel)
                    }
                }
                ImGui.endMenu()
            }
            ImGui.endPopup()
        }
    }

    private fun drawCell(path: Path) {
        val isDir = path.isDirectory()
        val ext = path.extension.lowercase()
        val label = elide(path.name.ifEmpty { path.nameWithoutExtension }, LABEL_MAX_CHARS)
        val texId = IconCache.idFor(isDir, ext, path.toAbsolutePath().toString())
        val textH = ImGui.getTextLineHeightWithSpacing()
        val cellW = ImGui.getContentRegionAvailX()
        val cellH = THUMB_SIZE + textH + 6f
        val startPos = ImGui.getCursorPos()
        val selected = selectedEntry == path
        val isRenaming = renamingEntry == path

        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 4f, 4f)
        if (!selected) {
            ImGui.pushStyleColor(ImGuiCol.Header,        0f, 0f, 0f, 0f)
            ImGui.pushStyleColor(ImGuiCol.HeaderHovered, 0f, 0f, 0f, 0f)
            ImGui.pushStyleColor(ImGuiCol.HeaderActive,  0f, 0f, 0f, 0f)
        }

        if (ImGui.selectable("##$label", selected, 0, cellW, cellH)) {
            selectedEntry = path
            SelectionManager.selected = path
        }

        if (!selected)
            ImGui.popStyleColor(3)
        ImGui.popStyleVar()

        if (ImGui.beginPopupContextWindow("##cell_ctx_${path.name}")) {
            if (ImGui.beginMenu("New")) {
                if (ImGui.menuItem("Folder"))
                    createFolder(currentDir)
                if (ImGui.menuItem("Animation"))
                    createAnimationFile(currentDir)

                val sel = selectedEntry
                if (sel != null && !sel.isDirectory()) {
                    val ext = sel.extension.lowercase()
                    if (ext == "png" || ext == "jpg" || ext == "jpeg") {
                        ImGui.separator()
                        if (ImGui.menuItem("Spritesheet (${sel.nameWithoutExtension})"))
                            createSpritesheetFile(currentDir, sel)
                    }
                }
                ImGui.endMenu()
            }
            ImGui.endPopup()
        }

        if (selected && !isRenaming && ImGui.isKeyPressed(ImGuiKey.F2)) {
            renamingEntry = path
            renameBuffer = ImString(path.nameWithoutExtension, 256)
        }

        if (selected && !isRenaming && ImGui.isKeyPressed(ImGuiKey.Delete))
            deleteEntry(path)

        if (!isDir && ImGui.beginDragDropSource()) {
            val absPath = path.toAbsolutePath().toString()
            val payloadType = when (ext) {
                "png", "jpg", "jpeg" -> DnDPayload.TEXTURE
                "kt"                 -> DnDPayload.SCRIPT
                "flux"               -> DnDPayload.SCENE
                "asset"              -> when (AssetManager.getAssetType(path.toAbsolutePath().toString())) {
                    "ANIMATION"   -> DnDPayload.ANIMATION
                    "SPRITESHEET" -> DnDPayload.SPRITESHEET
                    else          -> null
                }
                else                 -> null
            }

            if (payloadType != null) {
                ImGui.setDragDropPayload(payloadType, absPath)
                ImGuiEx.imageFlipped(texId, 32f, 32f)
                ImGui.sameLine()
                ImGui.text(label)
            }

            ImGui.endDragDropSource()
        }

        if (isDir && ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left))
            selectDir(path)

        if (!isDir && ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) {
            when (ext) {
                "asset" -> when (AssetManager.getAssetType(path.toAbsolutePath().toString())) {
                    "ANIMATION"   -> onAnimationOpen?.invoke(path.toAbsolutePath().toString())
                    "SPRITESHEET" -> onSpritesheetOpen?.invoke(path.toAbsolutePath().toString())
                    else          -> openInExternalEditor(path)
                }
                else -> openInExternalEditor(path)
            }
        }

        val iconX = startPos.x + max(0f, (cellW - THUMB_SIZE) * 0.5f)
        ImGui.setCursorPos(iconX, startPos.y + 2f)
        ImGuiEx.imageFlipped(texId, THUMB_SIZE, THUMB_SIZE)

        if (isRenaming) {
            ImGui.setCursorPos(startPos.x, startPos.y + THUMB_SIZE + 4f)
            ImGui.setNextItemWidth(cellW)
            ImGui.setKeyboardFocusHere()
            if (ImGui.inputText("##rename_${path.name}", renameBuffer, ImGuiInputTextFlags.EnterReturnsTrue))
                commitRename(path)
            if (ImGui.isKeyPressed(ImGuiKey.Escape))
                renamingEntry = null
            if (!ImGui.isItemActive() && !ImGui.isItemFocused())
                renamingEntry = null
        } else {
            val textW = ImGui.calcTextSizeX(label)
            ImGui.setCursorPos(startPos.x + max(0f, (cellW - textW) * 0.5f), startPos.y + THUMB_SIZE + 4f)
            ImGui.textUnformatted(label)
        }

        ImGui.setCursorPos(startPos.x, startPos.y + cellH)
    }

    private fun commitRename(path: Path) {
        val newName = renameBuffer.get().trim()
        renamingEntry = null

        if (newName.isEmpty() || newName == path.nameWithoutExtension) return

        val newPath = if (path.isDirectory())
            path.parent.resolve(newName)
        else
            path.parent.resolve("$newName.${path.extension}")

        runCatching {
            Files.move(path, newPath)
            if (selectedEntry == path) {
                selectedEntry = newPath
                SelectionManager.selected = newPath
            }
        }.onFailure {
            NotificationModal.error("Failed to rename: ${it.message}")
        }
    }

    private fun deleteEntry(path: Path) {
        runCatching {
            if (path.isDirectory())
                path.toFile().deleteRecursively()
            else
                Files.delete(path)

            if (selectedEntry == path) {
                selectedEntry = null
                SelectionManager.selected = null
            }
        }.onFailure {
            NotificationModal.error("Failed to delete: ${it.message}")
        }
    }

    private fun renderFooter() {
        val sel = selectedEntry
        if (sel == null) {
            ImGui.textDisabled("No selection")
            return
        }

        val rel = runCatching { root.relativize(sel).toString() }.getOrElse { sel.toString() }

        if (sel.isDirectory()) {
            val count = runCatching { Files.list(sel).use { it.count() } }.getOrDefault(0L)
            ImGui.textUnformatted("$rel  ($count items)")
        } else {
            val size = runCatching { Files.size(sel) }.getOrDefault(-1L)
            ImGui.textUnformatted("$rel  ${humanSize(size)}")
        }

        ImGui.sameLine()
        if (ImGui.smallButton("Copy"))
            ImGui.setClipboardText(rel)
        ImGui.sameLine()
        if (ImGui.smallButton("Reveal"))
            revealInExplorer(sel)
    }

    private fun selectDir(path: Path) {
        val p = path.normalize()
        if (!Files.isDirectory(p) || !isUnderRoot(p)) return

        currentDir = p
        selectedEntry = null
    }

    private fun isUnderRoot(path: Path) =
        path.toAbsolutePath().normalize().startsWith(root.toAbsolutePath().normalize())

    private fun hasSubDirs(dir: Path): Boolean {
        if (!Files.isDirectory(dir))
            return false

        Files.newDirectoryStream(dir).use { ds ->
            for (p in ds) {
                if (p.isDirectory() && !p.name.startsWith('.'))
                    return true
            }
        }
        return false
    }

    private fun listChildren(dir: Path, onlyDirs: Boolean): List<Path> {
        if (!Files.exists(dir) || !Files.isDirectory(dir))
            return emptyList()

        return Files.list(dir).use { stream ->
            stream.filter { p ->
                val name = p.name
                if (name.isEmpty() || name.startsWith('.'))
                    return@filter false
                if (onlyDirs) Files.isDirectory(p) else true
            }.toList()
        }
    }

    private fun openInExternalEditor(path: Path) {
        if (!Desktop.isDesktopSupported()) return

        runCatching { Desktop.getDesktop().open(path.toFile()) }
    }

    private fun revealInExplorer(path: Path) {
        if (!Desktop.isDesktopSupported()) return

        val file = if (path.isDirectory()) path.toFile() else path.toFile().parentFile
        runCatching { Desktop.getDesktop().open(file) }
    }

    private fun elide(text: String, max: Int): String {
        if (text.length <= max)
            return text

        return text.take(max - 3) + "..."
    }

    private fun humanSize(bytes: Long): String {
        if (bytes < 0)
            return "?"

        val units = arrayOf("B", "KB", "MB", "GB")
        var b = bytes.toDouble()
        var i = 0
        while (b >= 1024.0 && i < units.size - 1) {
            b /= 1024.0
            i++
        }
        return "%.1f %s".format(b, units[i])
    }

    private fun createFolder(dir: Path) {
        var name = "New Folder"
        var folder = dir.resolve(name).toFile()
        var counter = 1
        while (folder.exists()) {
            name = "New Folder $counter"
            folder = dir.resolve(name).toFile()
            counter++
        }

        runCatching {
            folder.mkdirs()
            selectedEntry = folder.toPath()
        }.onFailure {
            NotificationModal.error("Failed to create folder: ${it.message}")
        }
    }

    private fun createAnimationFile(dir: Path) {
        var name = "New Animation"
        var file = dir.resolve("$name.asset").toFile()
        var counter = 1
        while (file.exists()) {
            name = "New Animation $counter"
            file = dir.resolve("$name.asset").toFile()
            counter++
        }

        runCatching {
            file.writeText(AssetSerializer.serialize(AssetData.Animation()))
            selectedEntry = file.toPath()
        }.onFailure {
            NotificationModal.error("Failed to create animation: ${it.message}")
        }
    }

    private fun createSpritesheetFile(dir: Path, texturePath: Path) {
        val relative = texturePath.toAbsolutePath()
            .relativeTo(Path("").toAbsolutePath())
            .toString()

        var name = texturePath.nameWithoutExtension
        var file = dir.resolve("$name.asset").toFile()
        var counter = 1
        while (file.exists()) {
            name = "${texturePath.nameWithoutExtension} $counter"
            file = dir.resolve("$name.asset").toFile()
            counter++
        }

        runCatching {
            file.writeText(AssetSerializer.serialize(
                AssetData.Spritesheet(
                    texture    = TextureHandle(relative),
                    cellWidth  = 32,
                    cellHeight = 32
                )
            ))
            selectedEntry = file.toPath()
        }.onFailure {
            NotificationModal.error("Failed to create spritesheet: ${it.message}")
        }
    }

    private object IconCache {

        private const val FOLDER  = "folder.png"
        private const val GENERIC = "file.png"

        private val extMap = mapOf(
            "png"    to "file_image.png",
            "jpg"    to "file_image.png",
            "jpeg"   to "file_image.png",
            "txt"    to "file_text.png",
            "md"     to "file_text.png",
            "json"   to "file_text.png",
            "yml"    to "file_text.png",
            "yaml"   to "file_text.png",
            "ttf"    to "file_font.png",
            "otf"    to "file_font.png",
            "shader" to "file_code.png",
            "glsl"   to "file_code.png",
            "kt"     to "file_code.png"
        )

        private val cache = mutableMapOf<String, Int>()

        fun idFor(isDir: Boolean, ext: String, absPath: String): Int {
            val iconPath = when {
                isDir -> FOLDER
                ext == "asset" -> when (AssetManager.getAssetType(absPath)) {
                    "ANIMATION" -> "file_animation.png"
                    else        -> GENERIC
                }
                else -> extMap[ext] ?: GENERIC
            }
            return cache.getOrPut(iconPath) {
                runCatching {
                    AssetManager.getTexture("textures/ui/$iconPath", AssetLocation.INTERNAL)
                        .rendererId
                }.getOrDefault(0)
            }
        }
    }
}
