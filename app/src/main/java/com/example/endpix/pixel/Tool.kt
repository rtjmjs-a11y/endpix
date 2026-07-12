package com.example.endpix.pixel

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
