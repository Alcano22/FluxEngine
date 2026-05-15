package org.flux.scripting.api

import org.flux.core.logging.logger
import org.flux.scripting.annotation.*
import org.joml.Vector3f
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction

@LuaApiClass("Vec3", description = "3D Vector with x, y, z components")
class LuaVec3(val vec: Vector3f) : LuaTable() {

    @field:LuaApiField("number", description = "X component")
    val x = Unit

    @field:LuaApiField("number", description = "Y component")
    val y = Unit

    @field:LuaApiField("number", description = "Z component")
    val z = Unit

    @field:LuaApiFunction(
        params = ["x: number", "y: number", "z: number"],
        description = "Sets all components at once"
    )
    val set = Unit

    @field:LuaApiFunction(
        returnType = "number",
        description = "Returns length of the vector"
    )
    val length = Unit

    @field:LuaApiFunction(
        description = "Normalizes the vector in-place"
    )
    val normalize = Unit

    companion object {
        private val INDEX    = valueOf("__index")
        private val NEWINDEX = valueOf("__newindex")
        private val TOSTRING = valueOf("__tostring")

        val LIB get() = LuaTable().also { lib ->
            lib.set("new", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs =
                    LuaVec3(Vector3f(args.tofloat(1), args.tofloat(2), args.tofloat(3)))
            })
        }

        private val logger = logger()
    }

    init {
        val mt = LuaTable()
        mt.rawset(INDEX, object : TwoArgFunction() {
            override fun call(self: LuaValue, key: LuaValue): LuaValue {
                val v = (self as LuaVec3).vec
                return when (key.tojstring()) {
                    "x" -> valueOf(v.x.toDouble())
                    "y" -> valueOf(v.y.toDouble())
                    "z" -> valueOf(v.z.toDouble())
                    "set" -> object : VarArgFunction() {
                        override fun invoke(args: Varargs): Varargs {
                            v.set(args.tofloat(2), args.tofloat(3), args.tofloat(4))
                            return NONE
                        }
                    }
                    "length" -> object : VarArgFunction() {
                        override fun invoke(args: Varargs): Varargs =
                            valueOf(v.length().toDouble())
                    }
                    "normalize" -> object : VarArgFunction() {
                        override fun invoke(args: Varargs): Varargs {
                            v.normalize()
                            return NONE
                        }
                    }
                    else -> NIL
                }
            }
        })

        mt.rawset(NEWINDEX, object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val self = args.arg(1) as? LuaVec3
                    ?: throw logger.throwing(LuaError("Expected LuaVec3"))
                val v = self.vec
                val value = args.tofloat(3)
                when (args.arg(2).tojstring()) {
                    "x" -> v.x = value
                    "y" -> v.y = value
                    "z" -> v.z = value
                    else -> throw logger.throwing(LuaError("Vec3 has no field '${args.arg(2).tojstring()}'"))
                }
                return NONE
            }
        })

        mt.rawset(TOSTRING, object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val v = (args.arg(1) as LuaVec3).vec
                return valueOf("Vec3(${v.x}, ${v.y}, ${v.z})")
            }
        })

        setmetatable(mt)
    }
}
