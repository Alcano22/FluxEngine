package org.flux.core.imgui

import imgui.ImGui
import imgui.flag.ImGuiTreeNodeFlags
import imgui.type.ImBoolean
import imgui.type.ImString
import org.flux.core.asset.AnimationHandle
import org.flux.core.asset.AssetData
import org.flux.core.asset.AssetHandle
import org.flux.core.asset.Sprite
import org.flux.core.asset.SpriteSource
import org.flux.core.asset.TextureHandle
import org.flux.core.asset.resolve
import org.flux.core.logging.logger
import org.flux.core.renderer.Texture2D
import org.flux.core.scene.Component
import org.flux.core.scene.ExposeInInspector
import org.flux.core.scene.HideInInspector
import org.flux.core.serialization.AssetSerializer
import org.flux.core.util.Color
import org.joml.*
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

                if (type == AssetHandle::class) {
                    val handle = value as? AssetHandle<*>
                    val returnType = mutProperty.returnType

                    val typeArg = returnType.arguments.firstOrNull()?.type?.classifier
                    when (typeArg) {
                        Texture2D::class -> drawTextureField(name, handle as? TextureHandle) { newHandle ->
                            mutProperty.set(component, newHandle)
                        }
                        AssetData.Animation::class -> drawAnimationField(name, handle as? AnimationHandle) { newHandle ->
                            mutProperty.set(component, newHandle)
                        }
                        else -> drawGenericAssetField(name, handle) { newHandle ->
                            mutProperty.set(component, newHandle)
                        }
                    }
                    continue
                }

                if (type == SpriteSource::class) {
                    drawSpriteSourceField(name, value as? SpriteSource) { newSource ->
                        mutProperty.set(component, newSource)
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
        val texture = handle?.resolve()
        if (texture != null)
            ImGuiEx.imageFlipped(texture.rendererId, 48f, 48f)
        else
            ImGui.textDisabled("[None]")

        ImGui.sameLine()
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

    private fun drawAnimationField(
        name: String,
        handle: AnimationHandle?,
        onChanged: (AnimationHandle?) -> Unit
    ) {
        if (handle != null)
            ImGui.text(handle.path)
        else
            ImGui.textDisabled("[None]")

        ImGui.sameLine()
        ImGui.text(name)

        if (ImGui.beginDragDropTarget()) {
            val payload = ImGui.acceptDragDropPayload<String>("ASSET_ANIMATION")
            if (payload != null) {
                val relative = File(payload).relativeTo(File("").absoluteFile).path
                onChanged(AnimationHandle(relative))
            }
            ImGui.endDragDropTarget()
        }

        if (ImGui.beginPopupContextItem("##ctx_$name")) {
            if (ImGui.menuItem("Clear"))
                onChanged(null)
            ImGui.endPopup()
        }
    }

    private fun drawGenericAssetField(
        name: String,
        handle: AssetHandle<*>?,
        onChanged: (AssetHandle<*>?) -> Unit
    ) {
        if (handle != null)
            ImGui.text(handle.path)
        else
            ImGui.textDisabled("[None]")

        ImGui.sameLine()
        ImGui.text(name)

        if (ImGui.beginPopupContextItem("##ctx_$name")) {
            if (ImGui.menuItem("Clear"))
                onChanged(null)
            ImGui.endPopup()
        }
    }

    private fun drawSpriteSourceField(
        name: String,
        source: SpriteSource?,
        onChanged: (SpriteSource?) -> Unit
    ) {
        when (source) {
            is SpriteSource.FromTexture -> {
                val tex = runCatching { source.handle.resolve() }.getOrNull()
                if (tex != null)
                    ImGuiEx.imageFlipped(tex.rendererId, 48f, 48f)
                else
                    ImGui.textDisabled("[None]")
                ImGui.sameLine()
                ImGui.text("$name  [Texture]")
            }
            is SpriteSource.FromSprite -> {
                val sheet = runCatching { source.sprite.spritesheet.resolve() }.getOrNull()
                val tex = runCatching { sheet?.texture?.resolve() }.getOrNull()
                if (tex != null && sheet != null) {
                    val uvs = sheet.computeUVs(source.sprite.frameIndex)
                    ImGui.image(tex.rendererId.toLong(), 48f, 48f, uvs[0], uvs[3], uvs[2], uvs[1])
                } else
                    ImGui.textDisabled("[None]")
                ImGui.sameLine()
                ImGui.text("$name  [Sprite #${source.sprite.frameIndex}]")
            }
            null -> {
                ImGui.textDisabled("[None]")
                ImGui.sameLine()
                ImGui.text(name)
            }
        }

        if (ImGui.beginDragDropTarget()) {
            ImGui.acceptDragDropPayload<String>("ASSET_TEXTURE")?.let { payload ->
                val relative = File(payload).relativeTo(File("").absoluteFile).path
                onChanged(SpriteSource.FromTexture(TextureHandle(relative)))
            }
            ImGui.acceptDragDropPayload<String>("ASSET_SPRITE")?.let { payload ->
                runCatching {
                    val sprite = AssetSerializer.format.decodeFromString<Sprite>(payload)
                    onChanged(SpriteSource.FromSprite(sprite))
                }
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
