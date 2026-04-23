package org.flux.core.layer

class LayerStack : Iterable<Layer> {

    private val layers = mutableListOf<Layer>()
    private var layerInsertIndex = 0

    fun pushLayer(layer: Layer) {
        layers.add(layerInsertIndex, layer)
        layerInsertIndex++
        layer.onAttach()
    }

    fun pushOverlay(overlay: Layer) {
        layers.add(overlay)
        overlay.onAttach()
    }

    fun popLayer(layer: Layer) {
        val index = layers.indexOf(layer)
        if (index == -1 || index >= layerInsertIndex) return

        layers.removeAt(index)
        layerInsertIndex--
        layer.onDetach()
    }

    fun popOverlay(overlay: Layer) {
        val index = layers.indexOf(overlay)
        if (index == -1 || index < layerInsertIndex) return

        layers.removeAt(index)
        overlay.onDetach()
    }

    fun clear() {
        layers.forEach { it.onDetach() }
        layers.clear()
        layerInsertIndex = 0
    }

    override fun iterator() = layers.iterator()

    fun reversed() = layers.asReversed().toList()
}
