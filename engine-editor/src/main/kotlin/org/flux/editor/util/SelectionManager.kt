package org.flux.editor.util

import org.flux.core.scene.Entity

object SelectionManager {

    var selected: Any? = null

    val selectedEntity: Entity?
        get() = selected as? Entity

    fun clear() { selected = null }
}