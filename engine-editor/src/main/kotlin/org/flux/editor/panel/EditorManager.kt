package org.flux.editor.panel

import org.flux.core.util.Disposable
import org.flux.core.util.Timestep

class EditorManager : Disposable {

    @PublishedApi
    internal val panels = mutableListOf<EditorPanel>()

    fun addPanel(panel: EditorPanel) {
        panels.add(panel)
    }

    inline fun <reified T : EditorPanel> getPanel(): T? =
        panels.filterIsInstance<T>().firstOrNull()

    fun onUpdate(ts: Timestep) {
        panels.forEach {
            if (it.isOpen)
                it.onUpdate(ts)
        }
    }

    fun onImGuiRender() {
        panels.forEach {
            if (it.isOpen)
                it.onImGuiRender()
        }
    }

    override fun dispose() = panels.forEach { it.dispose() }
}
