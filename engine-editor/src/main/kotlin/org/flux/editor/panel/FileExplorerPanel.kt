package org.flux.editor.panel

import imgui.ImGui
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiMouseButton
import imgui.flag.ImGuiStyleVar
import imgui.flag.ImGuiTableFlags
import imgui.flag.ImGuiTreeNodeFlags
import org.flux.core.asset.AssetLocation
import org.flux.core.asset.AssetManager
import org.flux.core.imgui.ImGuiEx
import org.flux.core.renderer.TextureFilter
import org.flux.editor.util.DnDPayload
import org.flux.editor.util.SelectionManager
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.math.floor
import kotlin.math.max

class FileExplorerPanel(
    private val root: Path = Path("assets")
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
    }

    private fun drawCell(path: Path) {
        val isDir = path.isDirectory()
        val ext = path.extension.lowercase()
        val label = elide(path.name.ifEmpty { path.nameWithoutExtension }, LABEL_MAX_CHARS)
        val texId = IconCache.idFor(isDir, ext)
        val textH = ImGui.getTextLineHeightWithSpacing()
        val cellW = ImGui.getContentRegionAvailX()
        val cellH = THUMB_SIZE + textH + 6f
        val startPos = ImGui.getCursorPos()
        val selected = selectedEntry == path

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

        if (!isDir && ImGui.beginDragDropSource()) {
            val absPath = path.toAbsolutePath().toString()
            val payloadType = when (ext) {
                "png", "jpg", "jpeg" -> DnDPayload.TEXTURE
                "kt"                 -> DnDPayload.SCRIPT
                "flux"               -> DnDPayload.SCENE
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

        if (!isDir && ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left))
            openInExternalEditor(path)

        val iconX = startPos.x + max(0f, (cellW - THUMB_SIZE) * 0.5f)
        ImGui.setCursorPos(iconX, startPos.y + 2f)
        ImGuiEx.imageFlipped(texId, THUMB_SIZE, THUMB_SIZE)

        val textW = ImGui.calcTextSizeX(label)
        ImGui.setCursorPos(startPos.x + max(0f, (cellW - textW) * 0.5f), startPos.y + THUMB_SIZE + 4f)
        ImGui.textUnformatted(label)

        ImGui.setCursorPos(startPos.x, startPos.y + cellH)
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

    private object IconCache {

        private const val FOLDER  = "textures/ui/folder.png"
        private const val GENERIC = "textures/ui/file.png"

        private val extMap = mapOf(
            "png"    to "textures/ui/file_image.png",
            "jpg"    to "textures/ui/file_image.png",
            "jpeg"   to "textures/ui/file_image.png",
            "txt"    to "textures/ui/file_text.png",
            "md"     to "textures/ui/file_text.png",
            "json"   to "textures/ui/file_text.png",
            "yml"    to "textures/ui/file_text.png",
            "yaml"   to "textures/ui/file_text.png",
            "ttf"    to "textures/ui/file_font.png",
            "otf"    to "textures/ui/file_font.png",
            "shader" to "textures/ui/file_code.png",
            "glsl"   to "textures/ui/file_code.png",
            "kt"     to "textures/ui/file_code.png"
        )

        private val cache = mutableMapOf<String, Int>()

        fun idFor(isDir: Boolean, ext: String): Int {
            val path = if (isDir) FOLDER else extMap[ext] ?: GENERIC
            return cache.getOrPut(path) {
                runCatching {
                    AssetManager.getTexture(path, AssetLocation.INTERNAL)
                        .rendererId
                }.getOrDefault(0)
            }
        }
    }
}
