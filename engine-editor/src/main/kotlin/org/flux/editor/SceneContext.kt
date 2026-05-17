package org.flux.editor

import org.flux.core.runtime.RuntimeState
import org.flux.core.scene.Scene
import org.flux.core.serialization.SceneSerializer
import org.flux.editor.util.SelectionManager

class SceneContext(scene: Scene) {

    var scene = scene
        private set

    var runtimeState = RuntimeState.STOPPED
        private set

    private var sceneSnapshot: String? = null

    private val sceneChangeListeners = mutableListOf<(Scene) -> Unit>()
    private val stateChangeListeners = mutableListOf<(RuntimeState) -> Unit>()

    val isPlaying get() = runtimeState == RuntimeState.PLAYING
    val isPaused  get() = runtimeState == RuntimeState.PAUSED
    val isStopped get() = runtimeState == RuntimeState.STOPPED

    fun onSceneChange(listener: (Scene) -> Unit) {
        sceneChangeListeners.add(listener)
    }

    fun replace(newScene: Scene) {
        scene = newScene
        SelectionManager.clear()
        sceneChangeListeners.forEach { it(newScene) }
    }

    fun onStateChange(listener: (RuntimeState) -> Unit) {
        stateChangeListeners.add(listener)
    }

    fun play() {
        if (runtimeState == RuntimeState.STOPPED) {
            sceneSnapshot = SceneSerializer.serialize(scene)
            scene.onStart()
        }
        setState(RuntimeState.PLAYING)
    }

    fun pause() {
        if (runtimeState == RuntimeState.PLAYING)
            setState(RuntimeState.PAUSED)
    }

    fun resume() {
        if (runtimeState == RuntimeState.PAUSED)
            setState(RuntimeState.PLAYING)
    }

    fun stop() {
        if (runtimeState == RuntimeState.STOPPED) return

        scene.onStop()
        val snapshot = sceneSnapshot
        if (snapshot != null) {
            replace(SceneSerializer.deserialize(snapshot))
            sceneSnapshot = null
        }
        setState(RuntimeState.STOPPED)
    }

    private fun setState(state: RuntimeState) {
        runtimeState = state
        stateChangeListeners.forEach { it(state) }
    }
}
