package org.flux.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.flux.core.logging.logger
import org.flux.core.scene.Component
import org.flux.core.util.Timestep
import org.flux.scripting.api.LuaEntity
import org.flux.scripting.api.LuaTransform
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue

@Serializable
@SerialName("LuaScriptComponent")
class LuaScriptComponent(val scriptPath: String) : Component() {

    companion object {
        private val logger = logger()
    }

    @Transient private var globals: Globals? = null
    @Transient private var fnOnStart: LuaFunction? = null
    @Transient private var fnOnStop: LuaFunction? = null
    @Transient private var fnOnUpdate: LuaFunction? = null

    override fun onStart() {
        val sandbox = LuaRuntime.createSandbox()
        sandbox.set("entity", LuaEntity(entity))
        sandbox.set("transform", LuaTransform(entity.transform))

        val chunk = LuaRuntime.load(scriptPath, sandbox) ?: return
        safeCall { chunk.call() }

        fnOnStart = sandbox.get("onStart").takeIf { it.isfunction() } as? LuaFunction
        fnOnStop = sandbox.get("onStop").takeIf { it.isfunction() } as? LuaFunction
        fnOnUpdate = sandbox.get("onUpdate").takeIf { it.isfunction() } as? LuaFunction
        globals = sandbox

        fnOnStart?.let { safeCall { it.call() } }
    }

    override fun onStop() {
        fnOnStop?.let { safeCall { it.call() } }
        fnOnStart = null
        fnOnStop = null
        fnOnUpdate = null
        globals = null
    }

    override fun onUpdate(ts: Timestep) {
        fnOnUpdate?.let { safeCall { it.call(LuaValue.valueOf(ts.seconds.toDouble())) } }
    }

    private inline fun safeCall(block: () -> Unit) {
        try {
            block()
        } catch (e: LuaError) {
            logger.error { "Error in '$scriptPath': ${e.message}" }
        }
    }
}
