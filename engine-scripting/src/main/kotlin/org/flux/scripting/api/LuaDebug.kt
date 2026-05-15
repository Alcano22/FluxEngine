package org.flux.scripting.api

import io.github.oshai.kotlinlogging.KotlinLogging
import org.flux.scripting.annotation.LuaApiClass
import org.flux.scripting.annotation.LuaApiFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction

@LuaApiClass("DebugAPI", description = "Logging from Lua scripts")
object LuaDebug {

    @field:LuaApiFunction(
        params = ["msg: string"],
        description = "Logs an info message"
    )
    val log = Unit

    @field:LuaApiFunction(
        params = ["msg: string"],
        description = "Logs a warning message"
    )
    val warn = Unit

    @field:LuaApiFunction(
        params = ["msg: string"],
        description = "Logs an error message"
    )
    val error = Unit

    private val logger = KotlinLogging.logger("LuaScript")

    val LIB get() = LuaTable().also { t ->
        t.set("log", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                logger.info { arg.tojstring() }
                return NONE
            }
        })

        t.set("warn", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                logger.warn { arg.tojstring() }
                return NONE
            }
        })

        t.set("error", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                logger.error { arg.tojstring() }
                return NONE
            }
        })
    }
}
