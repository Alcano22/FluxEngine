package org.flux.opengl

import org.flux.core.EngineBuilder
import org.flux.opengl.imgui.GLImGuiBackend
import org.flux.opengl.renderer.GLFactory
import org.flux.opengl.renderer.GLRendererAPI

fun EngineBuilder.useOpenGL(): EngineBuilder {
    this.rendererApi = GLRendererAPI()
    this.graphicsFactory = GLFactory()
    this.imguiRendererBackend = GLImGuiBackend()
    return this
}
