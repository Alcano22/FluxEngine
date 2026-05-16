package org.flux.editor.util

import org.flux.core.scene.Entity
import java.nio.file.Path

object SelectionManager {

    var selected: Any? = null

    val selectedEntity: Entity?
        get() = selected as? Entity

    val selectedPath: Path?
        get() = selected as? Path

    fun clear() { selected = null }
}