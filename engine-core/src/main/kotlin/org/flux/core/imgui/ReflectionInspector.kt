package org.flux.core.imgui

import imgui.ImGui
import imgui.flag.ImGuiTreeNodeFlags
import imgui.type.ImBoolean
import imgui.type.ImString
import org.flux.core.asset.AssetLocation
import org.flux.core.asset.AssetManager
import org.flux.core.logging.logger
import org.flux.core.renderer.Texture2D
import org.flux.core.renderer.TextureHandle
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
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

object ReflectionInspector {

    private val logger = logger()

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
                val type = mutProperty.returnType.classifier as? KClass<*>
                val value = mutProperty.get(component)

                if (type == TextureHandle::class) {
                    drawTextureField(name, value as? TextureHandle) { newHandle ->
                        mutProperty.set(component, newHandle)
                    }
                    continue
                }

                if (value == null) continue

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

    private fun drawTextureField(
        name: String,
        handle: TextureHandle?,
        onChanged: (TextureHandle?) -> Unit
    ) {
        val texture = handle?.texture
        if (texture != null) {
            ImGuiEx.imageFlipped(texture.rendererId, 48f, 48f)
            ImGui.sameLine()
        } else {
            ImGui.textDisabled("[None]")
            ImGui.sameLine()
        }

        ImGui.text(name)

        if (ImGui.beginDragDropTarget()) {
            val payload = ImGui.acceptDragDropPayload<String>("ASSET_TEXTURE")
            if (payload != null) {
                val relative = File(payload).relativeTo(File("").absoluteFile).path
                onChanged(TextureHandle(relative))
            }
            ImGui.endDragDropTarget()
        }

        if (ImGui.beginPopupContextItem("##ctx_$name")) {
            if (ImGui.menuItem("Clear"))
                onChanged(null)
            ImGui.endPopup()
        }
    }
}
