package org.flux.core.imgui

import imgui.ImGui
import imgui.flag.ImGuiTreeNodeFlags
import imgui.type.ImBoolean
import imgui.type.ImString
import org.flux.core.scene.Component
import org.flux.core.scene.ExposeInInspector
import org.flux.core.scene.HideInInspector
import org.flux.core.util.Color
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4f
import org.joml.Vector4i
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

object ReflectionInspector {

    private val propertyCache = mutableMapOf<KClass<out Component>, List<KMutableProperty1<Any, Any?>>>()

    fun drawComponent(component: Component) {
        val kClass = component::class
        val componentName = kClass.simpleName ?: "Unknown Component"

        val flags = ImGuiTreeNodeFlags.DefaultOpen or ImGuiTreeNodeFlags.Framed
        ImGuiEx.treeNode(componentName, flags) {
            val properties = propertyCache.getOrPut(kClass) {
                kClass.memberProperties
                    .filterIsInstance<KMutableProperty1<Any, Any?>>()
                    .filter { prop ->
                        if (prop.hasAnnotation<HideInInspector>())
                            false
                        else if (prop.hasAnnotation<ExposeInInspector>()) {
                            prop.isAccessible = true
                            true
                        } else
                            prop.visibility == KVisibility.PUBLIC
                    }
            }

            for (mutProperty in properties) {
                val name = mutProperty.name
                val value = mutProperty.get(component) ?: continue
                when (value) {
                    is Boolean -> {
                        val tmp = ImBoolean(value)
                        if (ImGui.checkbox(name, tmp))
                            mutProperty.set(component, tmp.get())
                    }
                    is Int -> {
                        val tmp = intArrayOf(value)
                        if (ImGui.dragInt(name, tmp, 1f))
                            mutProperty.set(component, tmp[0])
                    }
                    is Float -> {
                        val tmp = floatArrayOf(value)
                        if (ImGui.dragFloat(name, tmp, 0.1f))
                            mutProperty.set(component, tmp[0])
                    }

                    is String -> {
                        val tmp = ImString(value, 256)
                        if (ImGui.inputText(name, tmp))
                            mutProperty.set(component, tmp.get())
                    }

                    is Vector2i -> ImGuiEx.dragInt2(name, value, 1f)
                    is Vector3i -> ImGuiEx.dragInt3(name, value, 1f)
                    is Vector4i -> ImGuiEx.dragInt4(name, value, 1f)

                    is Vector2f -> ImGuiEx.dragFloat2(name, value, 0.1f)
                    is Vector3f -> ImGuiEx.dragFloat3(name, value, 0.1f)
                    is Vector4f -> ImGuiEx.dragFloat4(name, value, 0.1f)

                    is Color -> ImGuiEx.colorEdit4(name, value)

                    is Enum<*> -> {
                        val enumConstants = value.javaClass.enumConstants
                        if (ImGui.beginCombo(name, value.name)) {
                            for (enumValue in enumConstants) {
                                val isSelected = (enumValue == value)
                                if (ImGui.selectable((enumValue as Enum<*>).name, isSelected))
                                    mutProperty.set(component, enumValue)
                                if (isSelected)
                                    ImGui.setItemDefaultFocus()
                            }
                            ImGui.endCombo()
                        }
                    }
                }
            }
        }
    }

}
