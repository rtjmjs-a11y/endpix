package com.example.endpix.pixel

class Frame(val width: Int, val height: Int) {

    val layers = mutableListOf<Layer>()
    var activeLayerIndex: Int = 0
        private set

    fun activeLayer(): Layer {
        val idx = activeLayerIndex.coerceIn(0, layers.lastIndex)
        activeLayerIndex = idx
        return layers[idx]
    }

    fun addLayer(name: String = "Layer ${layers.size + 1}"): Layer {
        val layer = Layer(PixelCanvas(width, height), name)
        layers.add(layer)
        activeLayerIndex = layers.lastIndex
        return layer
    }

    fun addLayerAt(index: Int, layer: Layer) {
        val i = index.coerceIn(0, layers.size)
        layers.add(i, layer)
        activeLayerIndex = i
    }

    fun deleteLayer(index: Int): Layer? {
        if (layers.size <= 1) return null
        if (index !in layers.indices) return null
        val removed = layers.removeAt(index)
        activeLayerIndex = (activeLayerIndex - 1).coerceAtLeast(0).coerceAtMost(layers.lastIndex)
        return removed
    }

    fun moveLayer(from: Int, to: Int) {
        if (from !in layers.indices || to !in layers.indices) return
        val layer = layers.removeAt(from)
        layers.add(to, layer)
        activeLayerIndex = to
    }

    fun selectLayer(index: Int) {
        if (index in layers.indices) activeLayerIndex = index
    }
}
