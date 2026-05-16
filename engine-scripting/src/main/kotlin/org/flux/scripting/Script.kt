package org.flux.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.flux.core.scene.Component
import org.flux.core.util.Timestep

@Serializable(with = ScriptSerializer::class)
@SerialName("Script")
abstract class Script : Component() {

    open fun start() {}
    open fun stop() {}
    open fun update(dt: Float) {}

    final override fun onStart() = start()
    final override fun onStop() = stop()
    final override fun onUpdate(ts: Timestep) = update(ts.seconds)
}
