package com.example.endpix.pixel

import com.example.endpix.R

enum class Tool {
    PENCIL,
    ERASER,
    BUCKET,
    EYEDROPPER,
    SHAPE
}

enum class ShapeMode {
    LINE,
    RECTANGLE,
    CIRCLE,
    LEAF,
    LASSO
}

enum class ShortcutAction(val label: String) {
    NONE("无"),
    FIT("适配"),
    SAVE("保存"),
    ENABLE_GRID("开启与关闭网格"),
    OPEN_SETTINGS("打开设置")
}

enum class ExpandDir(val label: String) {
    TOP_LEFT("左上"),
    TOP("上"),
    TOP_RIGHT("右上"),
    LEFT("左"),
    RIGHT("右"),
    CENTER("中心"),
    BOTTOM_LEFT("左下"),
    BOTTOM("下"),
    BOTTOM_RIGHT("右下");

    fun offsetX(oldW: Int, newW: Int): Int = when (this) {
        TOP_LEFT, LEFT, BOTTOM_LEFT -> newW - oldW
        TOP, CENTER, BOTTOM -> (newW - oldW) / 2
        TOP_RIGHT, RIGHT, BOTTOM_RIGHT -> 0
    }

    fun offsetY(oldH: Int, newH: Int): Int = when (this) {
        TOP_LEFT, TOP, TOP_RIGHT -> newH - oldH
        LEFT, CENTER, RIGHT -> (newH - oldH) / 2
        BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT -> 0
    }

    fun iconRes(): Int = when (this) {
        TOP_LEFT -> R.drawable.ic_expand_tl
        TOP -> R.drawable.ic_expand_t
        TOP_RIGHT -> R.drawable.ic_expand_tr
        LEFT -> R.drawable.ic_expand_l
        CENTER -> R.drawable.ic_expand_c
        RIGHT -> R.drawable.ic_expand_r
        BOTTOM_LEFT -> R.drawable.ic_expand_bl
        BOTTOM -> R.drawable.ic_expand_b
        BOTTOM_RIGHT -> R.drawable.ic_expand_br
    }
}

enum class PerfMode(val label: String, val desc: String) {
    DEFAULT("默认方案", "flatten 全量合成"),
    REGION("区域合成", "flattenRegion 脏区域"),
    ASYNC("异步合成", "后台线程 flatten"),
    HA("GPU 合成", "HA 模式 GL 图层混合")
}
