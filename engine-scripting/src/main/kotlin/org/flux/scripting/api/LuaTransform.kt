package org.flux.scripting.api

import org.flux.core.scene.TransformComponent
import org.flux.scripting.annotation.LuaApiClass
import org.flux.scripting.annotation.LuaApiField
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

@LuaApiClass("LuaTransform", description = "Transform of an entity - position, rotation, scale")
class LuaTransform(private val transform: TransformComponent) : LuaTable() {

    @field:LuaApiField("Vec3", description = "Position in world space")
    val position = Unit

    @field:LuaApiField("Vec3", description = "Rotation in Euler angles (radians)")
    val rotation = Unit

    @field:LuaApiField("Vec3", description = "Scale")
    val scale = Unit

    companion object {
        private val INDEX    = valueOf("__index")
        private val NEWINDEX = valueOf("__newindex")
    }

    private val positionVec = LuaVec3(transform.position)
    private val rotationVec = LuaVec3(transform.rotation)
    private val scaleVec    = LuaVec3(transform.scale)

    init {
        val mt = LuaTable()
        mt.rawset(INDEX, object : TwoArgFunction() {
            override fun call(self: LuaValue, key: LuaValue): LuaValue =
                when (key.tojstring()) {
                    "position" -> positionVec
                    "rotation" -> rotationVec
                    "scale"    -> scaleVec
                    else       -> NIL
                }
        })

        mt.rawset(NEWINDEX, object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val key = args.arg(2).tojstring()
                val value = args.arg(3)
                if (value is LuaVec3) {
                    when (key) {
                        "position" -> transform.position.set(value.vec)
                        "rotation" -> transform.rotation.set(value.vec)
                        "scale"    -> transform.scale.set(value.vec)
                    }
                }
                return NONE
            }
        })

        setmetatable(mt)
    }
}
