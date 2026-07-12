package com.example.endpix.pixel

import java.io.Serializable

data class SaveData(
    val docs: List<DocData>,
    val activeDoc: Int,
    val tool: Tool,
    val color: Int,
    val shapeMode: ShapeMode,
    val shapeFill: Boolean,
    val uiCornerRadius: Float = 8f
) : Serializable

data class DocData(
    val name: String,
    val w: Int, val h: Int,
    val frames: List<FrameData>,
    val activeFrame: Int
) : Serializable

data class FrameData(
    val layers: List<LayerData>,
    val activeLayer: Int
) : Serializable

data class LayerData(
    val name: String,
    val opacity: Float,
    val visible: Boolean,
    val pixels: IntArray
) : Serializable
