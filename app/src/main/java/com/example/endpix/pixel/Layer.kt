package com.example.endpix.pixel

class Layer(
    val canvas: PixelCanvas,
    var name: String = "Layer",
    var opacity: Float = 1f,
    var visible: Boolean = true,
    var glTextureId: Int = 0
)
