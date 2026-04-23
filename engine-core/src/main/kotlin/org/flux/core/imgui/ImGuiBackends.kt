package org.flux.core.imgui

import org.flux.core.util.Disposable

interface ImGuiPlatformBackend : Disposable {

    fun init()

    fun newFrame()

    fun updateViewports()
}

interface ImGuiRendererBackend : Disposable {

    fun init()

    fun newFrame()
    fun renderDrawData()
}
