package org.flux.scripting

import org.flux.core.logging.logger
import org.flux.scripting.api.*
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File

object LuaRuntime {

    private val logger = logger()

    private val sourceCache = mutableMapOf<String, String>()

    fun createSandbox(): Globals {
        val globals = JsePlatform.standardGlobals()

        globals.set("Input", LuaInput.LIB)
        globals.set("Vec3", LuaVec3.LIB)
        globals.set("Debug", LuaDebug.LIB)

        globals.set("os", LuaValue.NIL)
        globals.set("io", LuaValue.NIL)
        globals.set("luajava", LuaValue.NIL)

        return globals
    }

    fun load(path: String, globals: Globals): LuaFunction? {
        val src = sourceCache.getOrPut(path) {
            val file = File(path)
            if (!file.exists()) {
                logger.error { "Script not found: $path" }
                return null
            }
            file.readText()
        }

        return try {
            val chunk = globals.load(src, "@$path")
            if (chunk.isnil()) {
                logger.error { "Failed to load: $path" }
                null
            } else
                chunk as LuaFunction
        } catch (e: LuaError) {
            logger.error { "Compile error in '$path': ${e.message}" }
            null
        }
    }

    fun invalidate(path: String) = sourceCache.remove(path)
    fun invalidateAll() = sourceCache.clear()
}
