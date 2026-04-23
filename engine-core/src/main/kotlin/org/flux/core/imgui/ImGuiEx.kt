package org.flux.core.imgui

import imgui.ImGui
import imgui.flag.ImGuiColorEditFlags
import imgui.flag.ImGuiTreeNodeFlags
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImBoolean
import org.flux.core.util.toArray
import org.joml.*
import kotlin.reflect.KMutableProperty0

object ImGuiEx {

    inline fun window(
        name: String,
        flags: Int = ImGuiWindowFlags.None,
        content: ImGuiEx.() -> Unit
    ) {
        val isOpen = ImGui.begin(name, flags)
        try {
            if (isOpen)
                content()
        } finally {
            ImGui.end()
        }
    }

    inline fun treeNode(
        label: String,
        flags: Int = ImGuiTreeNodeFlags.None,
        content: ImGuiEx.() -> Unit
    ) {
        val isOpen = ImGui.treeNodeEx(label, flags)
        if (isOpen) {
            try {
                content()
            } finally {
                ImGui.treePop()
            }
        }
    }

    inline fun mainMenuBar(content: ImGuiEx.() -> Unit) {
        if (ImGui.beginMainMenuBar()) {
            try {
                content()
            } finally {
                ImGui.endMainMenuBar()
            }
        }
    }

    inline fun menu(
        label: String,
        enabled: Boolean = true,
        content: ImGuiEx.() -> Unit
    ) {
        if (ImGui.beginMenu(label, enabled)) {
            try {
                content()
            } finally {
                ImGui.endMenu()
            }
        }
    }

    inline fun checkbox(
        label: String,
        property: KMutableProperty0<Boolean>,
        onChanged: (Boolean) -> Unit = {}
    ): Boolean {
        val tmp = ImBoolean(property.get())
        val changed = ImGui.checkbox(label, tmp)
        if (changed) {
            val newValue = tmp.get()
            property.set(newValue)
            onChanged(newValue)
        }
        return changed
    }

    inline fun dragInt(
        label: String,
        value: KMutableProperty0<Int>,
        speed: Float = 1f,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        format: String = "%d",
        onChanged: (Int) -> Unit = {}
    ): Boolean {
        val tmp = intArrayOf(value.get())
        val changed = ImGui.dragInt(label, tmp, speed, min, max, format)
        if (changed) {
            val newValue = tmp[0]
            value.set(newValue)
            onChanged(newValue)
        }
        return changed
    }

    inline fun dragInt2(
        label: String,
        value: Vector2i,
        speed: Float = 1f,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        format: String = "%d",
        onChanged: (Vector2ic) -> Unit = {}
    ): Boolean {
        val tmp = value.toArray()
        val changed = ImGui.dragInt2(label, tmp, speed, min, max, format)
        if (changed) {
            value.set(tmp)
            onChanged(value)
        }
        return changed
    }

    inline fun dragInt3(
        label: String,
        value: Vector3i,
        speed: Float = 1f,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        format: String = "%d",
        onChanged: (Vector3ic) -> Unit = {}
    ): Boolean {
        val tmp = value.toArray()
        val changed = ImGui.dragInt3(label, tmp, speed, min, max, format)
        if (changed) {
            value.set(tmp)
            onChanged(value)
        }
        return changed
    }

    inline fun dragInt4(
        label: String,
        value: Vector4i,
        speed: Float = 1f,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        format: String = "%d",
        onChanged: (Vector4ic) -> Unit = {}
    ): Boolean {
        val tmp = value.toArray()
        val changed = ImGui.dragInt4(label, tmp, speed, min, max, format)
        if (changed) {
            value.set(tmp)
            onChanged(value)
        }
        return changed
    }

    inline fun dragFloat(
        label: String,
        value: KMutableProperty0<Float>,
        speed: Float = 1f,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        format: String = "%.3f",
        onChanged: (Float) -> Unit = {}
    ): Boolean {
        val tmp = floatArrayOf(value.get())
        val changed = ImGui.dragFloat(label, tmp, speed, min, max, format)
        if (changed) {
            val newValue = tmp[0]
            value.set(newValue)
            onChanged(newValue)
        }
        return changed
    }

    inline fun dragAngle(
        label: String,
        value: KMutableProperty0<Float>,
        speed: Float = 1f,
        min: Float = -360f,
        max: Float = 360f,
        format: String = "%.1f°",
        onChanged: (Float) -> Unit = {}
    ): Boolean {
        val deg = Math.toDegrees(value.get().toDouble()).toFloat()
        val tmp = floatArrayOf(deg)
        val changed = ImGui.dragFloat(label, tmp, speed, min, max, format)
        if (changed) {
            val rad = Math.toRadians(tmp[0])
            value.set(rad)
            onChanged(rad)
        }
        return changed
    }

    inline fun dragAngle3(
        label: String,
        value: Vector3f,
        speed: Float = 1f,
        min: Float = -360f,
        max: Float = 360f,
        format: String = "%.1f°",
        onChanged: (Vector3fc) -> Unit = {}
    ): Boolean {
        val tmp = floatArrayOf(
            Math.toDegrees(value.x.toDouble()).toFloat(),
            Math.toDegrees(value.y.toDouble()).toFloat(),
            Math.toDegrees(value.z.toDouble()).toFloat()
        )
        val changed = ImGui.dragFloat3(label, tmp, speed, min, max, format)
        if (changed) {
            value.set(
                Math.toRadians(tmp[0].toDouble()).toFloat(),
                Math.toRadians(tmp[1].toDouble()).toFloat(),
                Math.toRadians(tmp[2].toDouble()).toFloat()
            )
            onChanged(value)
        }
        return changed
    }

    inline fun dragFloat2(
        label: String,
        value: Vector2f,
        speed: Float = 1f,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        format: String = "%.3f",
        onChanged: (Vector2fc) -> Unit = {}
    ): Boolean {
        val tmp = value.toArray()
        val changed = ImGui.dragFloat2(label, tmp, speed, min, max, format)
        if (changed) {
            value.set(tmp)
            onChanged(value)
        }
        return changed
    }

    inline fun dragFloat3(
        label: String,
        value: Vector3f,
        speed: Float = 1f,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        format: String = "%.3f",
        onChanged: (Vector3fc) -> Unit = {}
    ): Boolean {
        val tmp = value.toArray()
        val changed = ImGui.dragFloat3(label, tmp, speed, min, max, format)
        if (changed) {
            value.set(tmp)
            onChanged(value)
        }
        return changed
    }

    inline fun dragFloat4(
        label: String,
        value: Vector4f,
        speed: Float = 1f,
        min: Float = -Float.MAX_VALUE,
        max: Float = Float.MAX_VALUE,
        format: String = "%.3f",
        onChanged: (Vector4fc) -> Unit = {}
    ): Boolean {
        val tmp = value.toArray()
        val changed = ImGui.dragFloat4(label, tmp, speed, min, max, format)
        if (changed) {
            value.set(tmp)
            onChanged(value)
        }
        return changed
    }

    inline fun colorEdit3(
        label: String,
        value: Vector3f,
        flags: Int = ImGuiColorEditFlags.None,
        onChanged: (Vector3fc) -> Unit = {}
    ): Boolean {
        val tmp = value.toArray()
        val changed = ImGui.colorEdit3(label, tmp, flags)
        if (changed) {
            value.set(tmp)
            onChanged(value)
        }
        return changed
    }

    inline fun colorEdit4(
        label: String,
        value: Vector4f,
        flags: Int = ImGuiColorEditFlags.None,
        onChanged: (Vector4fc) -> Unit = {}
    ): Boolean {
        val tmp = value.toArray()
        val changed = ImGui.colorEdit4(label, tmp, flags)
        if (changed) {
            value.set(tmp)
            onChanged(value)
        }
        return changed
    }
}
