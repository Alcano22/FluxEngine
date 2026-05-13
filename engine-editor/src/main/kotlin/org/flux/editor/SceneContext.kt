package org.flux.editor

import org.flux.core.scene.Scene

class SceneContext(scene: Scene) {

    var scene: Scene = scene
        private set

    private val listeners = mutableListOf<(Scene) -> Unit>()

    fun onSceneChange(listener: (Scene) -> Unit) {
        listeners.add(listener)
    }

    fun replace(newScene: Scene) {
        scene = newScene
        listeners.forEach { it(newScene) }
    }
}
