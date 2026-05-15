package org.flux.scripting.api

import org.flux.core.input.Input
import org.flux.core.input.Key
import org.flux.core.input.MouseButton
import org.flux.core.logging.logger
import org.flux.scripting.annotation.LuaApiClass
import org.flux.scripting.annotation.LuaApiField
import org.flux.scripting.annotation.LuaApiFunction
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction

@LuaApiClass("InputAPI", description = "Keyboard and mouse input")
object LuaInput {

    @field:LuaApiFunction(
        params = ["key: string"],
        returnType = "boolean",
        description = "Returns true while the key is held down"
    )
    val getKey = Unit

    @field:LuaApiFunction(
        params = ["key: string"],
        returnType = "boolean",
        description = "Returns true only in the frame the key was pressed"
    )
    val getKeyDown = Unit

    @field:LuaApiFunction(
        params = ["key: string"],
        returnType = "boolean",
        description = "Returns true only in the frame the key was released"
    )
    val getKeyUp = Unit

    @field:LuaApiFunction(
        params = ["button: string"],
        returnType = "boolean",
        description = "Returns true while the mouse button is held down"
    )
    val getMouseButton = Unit

    @field:LuaApiFunction(
        params = ["button: string"],
        returnType = "boolean",
        description = "Returns true only in the frame the mouse button was pressed"
    )
    val getMouseButtonDown = Unit

    @field:LuaApiFunction(
        params = ["button: string"],
        returnType = "boolean",
        description = "Returns true only in the frame the mouse button was released"
    )
    val getMouseButtonUp = Unit

    @field:LuaApiField("number", description = "Current X position of the mouse")
    val mouseX = Unit

    @field:LuaApiField("number", description = "Current Y position of the mouse")
    val mouseY = Unit

    private val logger = logger()

    val LIB get() = LuaTable().also { t ->
        t.set("getKey", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue =
                valueOf(Input.getKey(arg.toKey()))
        })

        t.set("getKeyDown", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue =
                valueOf(Input.getKeyDown(arg.toKey()))
        })

        t.set("getKeyUp", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue =
                valueOf(Input.getKeyUp(arg.toKey()))
        })

        t.set("getMouseButton", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue =
                valueOf(Input.getMouseButton(arg.toMouseButton()))
        })

        t.set("getMouseButtonDown", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue =
                valueOf(Input.getMouseButtonDown(arg.toMouseButton()))
        })

        t.set("getMouseButtonUp", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue =
                valueOf(Input.getMouseButtonUp(arg.toMouseButton()))
        })

        val mt = LuaTable()
        mt.rawset(LuaValue.valueOf("__index"), object : TwoArgFunction() {
            override fun call(self: LuaValue, key: LuaValue): LuaValue =
                when (key.tojstring()) {
                    "mouseX" -> valueOf(Input.mouseX.toDouble())
                    "mouseY" -> valueOf(Input.mouseY.toDouble())
                    else     -> self.rawget(key)
                }
        })
        t.setmetatable(mt)
    }

    private fun LuaValue.toKey(): Key =
        runCatching { Key.valueOf(tojstring()) }
            .getOrElse { throw logger.throwing(LuaError("Unknown key: '${tojstring()}'")) }

    private fun LuaValue.toMouseButton(): MouseButton =
        runCatching { MouseButton.valueOf(tojstring()) }
            .getOrElse { throw logger.throwing(LuaError("Unknown mouse button: '${tojstring()}'")) }
}
