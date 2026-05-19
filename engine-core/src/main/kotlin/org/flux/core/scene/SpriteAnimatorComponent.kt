package org.flux.core.scene

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.flux.core.asset.AnimationHandle
import org.flux.core.util.Timestep

@Serializable
@SingleComponent
class SpriteAnimatorComponent(
    var animationHandle: AnimationHandle? = null
) : Component() {

    @Transient
    var currentClip: AnimationClip? = null
        private set

    @Transient
    private var time = 0f

    @Transient
    private var frameIndex = 0

    val currentFrame get() = currentClip?.frames?.getOrNull(frameIndex)

    override fun onAttach() {
        animationHandle?.animation?.clips?.firstOrNull()?.let { play(it.name) }
    }

    override fun onStart() {
        animationHandle?.animation?.clips?.firstOrNull()?.let { play(it.name) }
    }

    override fun onStop() {
        time = 0f
        frameIndex = 0
        currentClip = null
    }

    override fun onUpdate(ts: Timestep) {
        val clip = currentClip ?: return

        time += ts.seconds
        val rawIndex = (time * clip.fps).toInt()

        frameIndex = if (rawIndex >= clip.frames.size) {
            if (clip.loop) {
                time %= clip.frames.size / clip.fps
                0
            } else
                clip.frames.size - 1
        } else
            rawIndex
    }

    fun play(name: String) {
        val clip = animationHandle?.animation?.clips?.find { it.name == name } ?: return
        if (clip == currentClip) return

        currentClip = clip
        time = 0f
        frameIndex = 0
    }
}
