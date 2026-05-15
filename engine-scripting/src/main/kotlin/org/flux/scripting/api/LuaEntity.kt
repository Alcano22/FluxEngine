package org.flux.scripting.api

import org.flux.core.scene.Entity
import org.flux.scripting.annotation.LuaApiClass
import org.flux.scripting.annotation.LuaApiField
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.TwoArgFunction

@LuaApiClass("LuaEntity", description = "Represents an entity in the scene")
class LuaEntity(private val entity: Entity) : LuaTable() {

    @field:LuaApiField("string", description = "Name of the entity")
    val name = Unit

    @field:LuaApiField("LuaTransform", description = "Transform of the entity")
    val transform = Unit

    companion object {
        private val INDEX = valueOf("__index")
    }

    init {
        val mt = LuaTable()
        mt.rawset(INDEX, object : TwoArgFunction() {
            override fun call(self: LuaValue, key: LuaValue): LuaValue =
                when (key.tojstring()) {
                    "name"      -> valueOf(entity.name)
                    "transform" -> LuaTransform(entity.transform)
                    else        -> NIL
                }
        })
        setmetatable(mt)
    }
}
