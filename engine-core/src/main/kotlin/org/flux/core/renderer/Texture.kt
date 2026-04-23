package org.flux.core.renderer

import org.flux.core.util.Disposable

interface Texture : Disposable {

    val width: Int
    val height: Int

    fun bind(slot: Int = 0)
}
