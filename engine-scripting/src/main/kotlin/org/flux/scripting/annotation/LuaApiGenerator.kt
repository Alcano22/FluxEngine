package org.flux.scripting.annotation

import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import java.io.File

object LuaApiGenerator {

    fun generate(outputPath: String) {
        val reflections = Reflections(
            ConfigurationBuilder()
                .forPackages("org.flux.scripting")
                .addClassLoaders(LuaApiGenerator::class.java.classLoader)
        )

        val classes = reflections.getTypesAnnotatedWith(LuaApiClass::class.java)
        val sb = StringBuilder()

        sb.appendLine("-- ============================================")
        sb.appendLine("-- flux_api.lua - AUTO-GENERATED, DO NOT EDIT")
        sb.appendLine("-- ============================================")
        sb.appendLine()

        for (cls in classes) {
            val classAnn = cls.getAnnotation(LuaApiClass::class.java) ?: continue
            val className = classAnn.name

            if (classAnn.description.isNotEmpty())
                sb.appendLine("--- ${classAnn.description}")

            sb.appendLine("---@class $className")

            for (field in cls.declaredFields) {
                val fieldAnn = field.getAnnotation(LuaApiField::class.java) ?: continue
                if (fieldAnn.description.isNotEmpty())
                    sb.appendLine("--- ${fieldAnn.description}")
                sb.appendLine("---@field ${field.name} ${fieldAnn.type}")
            }

            sb.appendLine("local $className = {}")
            sb.appendLine()

            for (field in cls.declaredFields) {
                val funcAnn = field.getAnnotation(LuaApiFunction::class.java) ?: continue
                val name = field.name

                sb.appendLine()
                if (funcAnn.description.isNotEmpty())
                    sb.appendLine("--- ${funcAnn.description}")
                for (param in funcAnn.params) {
                    val (pName, pType) = param.split(":").map { it.trim() }
                    sb.appendLine("---@param $pName $pType")
                }
                if (funcAnn.returnType != "nil")
                    sb.appendLine("---@return ${funcAnn.returnType}")

                val paramNames = funcAnn.params
                    .map { it.split(":")[0].trim() }
                    .joinToString(", ")
                sb.appendLine("function $className.$name($paramNames) end")
            }

            sb.appendLine()
        }

        sb.appendLine("-- ============================================")
        sb.appendLine("-- Globals - available in every script")
        sb.appendLine("-- ============================================")
        sb.appendLine()
        sb.appendLine("---@type LuaEntity")
        sb.appendLine("entity = {}")
        sb.appendLine()
        sb.appendLine("---@type LuaTransform")
        sb.appendLine("transform = {}")
        sb.appendLine()
        sb.appendLine("---@type InputAPI")
        sb.appendLine("Input = {}")
        sb.appendLine()
        sb.appendLine("---@type DebugAPI")
        sb.appendLine("Debug = {}")
        sb.appendLine()
        sb.appendLine("---@type Vec3")
        sb.appendLine("Vec3 = {}")

        sb.appendLine("-- ============================================")
        sb.appendLine("-- Script Lifecycle - define these in your script")
        sb.appendLine("-- ============================================")
        sb.appendLine()
        sb.appendLine("--- Called once when the runtime starts")
        sb.appendLine("function onStart() end")
        sb.appendLine()
        sb.appendLine("--- Called once when the runtime stops")
        sb.appendLine("function onStop() end")
        sb.appendLine()
        sb.appendLine("--- Called every frame while the runtime is playing")
        sb.appendLine("---@param dt number delta time in seconds")
        sb.appendLine("function onUpdate(dt) end")

        val file = File(outputPath)
        file.parentFile.mkdirs()
        file.writeText(sb.toString())

        println("[LuaApiGenerator] Generated: $outputPath")
    }
}
