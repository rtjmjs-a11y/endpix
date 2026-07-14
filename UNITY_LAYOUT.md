# endpix UI 布局规格 (Unity 可读)

> 所有尺寸单位: Unity px (1:1 对应 Android dp)
> 参考分辨率: 1080×2340 (竖屏手机)
> 坐标原点: 左上角 (0,0)

---

## 整体布局

```
┌──────────────────────────────────────────────────┐ y=0
│  StatusBar (半透明黑底)        w=screen, h=auto    │
├──────────────────────────────────────────────────┤
│  DocStrip (文档标签)           w=screen, h=32     │
├──────────────────────────────────────────────────┤
│  FrameStrip (帧缩略图)         w=screen, h=48     │
├────┬────────────────────────────┬───────────────┤
│ 左 │  OpenGL 画布 (全屏)        │ 右            │
│ 工 │  fillMaxSize               │ 调色板        │
│ 具 │                            │              │
│ 栏 │                            │              │
│ 48 │                            │ 48           │
│ dp │                            │ dp           │
├────┴────────────────────────────┴───────────────┤
│  BottomStrip (底部工具栏)    w=screen, h=48      │
└──────────────────────────────────────────────────┘ y=screen.h
```

---

## 1. StatusBar (左上角信息栏)

```csharp
// Unity UGUI 实现
GameObject statusBar = CreatePanel("StatusBar");
statusBar.SetAnchor(AnchorPresets.TopLeft);
statusBar.SetSize(300, 50);
statusBar.SetPosition(8, 8);
statusBar.SetBackground(Color(0x4D000000)); // 30% 透明黑
statusBar.SetCornerRadius(8); // LocalCornerRadius

// 子元素: HorizontalLayout
Text sizeText   = AddText("32×32",      fontSize=12, color=White);
Text frameText  = AddText("1帧",        fontSize=12, color=White);
Text zoomText   = AddText("100%",       fontSize=12, color=#2196F3);
Text fpsText    = AddText("FPS:60",     fontSize=12, color=#88CC88);
Text cpuText    = AddText("CPU:5%",     fontSize=12, color=#88AAFF);
Text gpuText    = AddText("GPU:12%",    fontSize=12, color=#FFAA88);
```

---

## 2. 左侧工具栏 (ToolColumn)

```csharp
// 位置: 屏幕左侧居中, x=4, y=center
// 尺寸: 48×fill, 内边距 4dp, 间距 4dp
// 垂直布局, 上下各一个 FlexibleSpace 撑满

GameObject toolColumn = CreateVerticalLayout("ToolColumn");
toolColumn.SetAnchor(AnchorPresets.MiddleLeft);
toolColumn.SetSize(48, screenHeight - 200);
toolColumn.SetPosition(4, 100);
toolColumn.spacing = 4;

// 工具按钮 (从上到下)
ToolButton pencil = new ToolButton("铅笔", icon_pencil, 40, onClick: TogglePencil);
    pencil.badge = brushSize.ToString(); // 右下角角标, 仅 brushSize > 1 时显示
    pencil.badgeColor = #64B5F6;
    pencil.badgeSize = 12;
    pencil.badgeBg = #1A1A1E;

ToolButton eraser = new ToolButton("橡皮", icon_eraser, 40, onClick: ToggleEraser);
    eraser.badge = eraserSize.ToString(); // 仅 eraserSize > 1 时显示

ToolButton bucket = new ToolButton("油漆桶", icon_bucket, 40, onClick: SelectBucket);
    bucket.badge = (bucketRemovePixels ? "●" : ""); // 红点 #F44336

ToolButton select = new ToolButton("选择", icon_select, 40, onClick: ToggleSelect);
    select.icon = (selectMode == RECT) ? icon_select_rect : icon_select_lasso;

ToolButton shape = new ToolButton("图形", icon_shape, 40, onClick: ToggleShape);
    shape.icon = GetShapeIcon(); // 根据 shapeMode 和 shapeFill 切换
```

---

## 3. 右侧调色板 (PaletteBar)

```csharp
// 位置: 屏幕右侧居中, x=screen.w-52, y=center
// 尺寸: 48×fill, 内边距 4dp, 垂直居中

GameObject paletteBar = CreateVerticalLayout("PaletteBar");
paletteBar.SetAnchor(AnchorPresets.MiddleRight);
paletteBar.SetSize(48, screenHeight - 200);
paletteBar.SetPosition(screenWidth - 52, 100);

// 色块
foreach (Color color in palette) {
    ColorSlot slot = new ColorSlot(30, 30); // 30×30dp
    slot.color = color;
    slot.border = (color == selectedColor) ? (2dp, primaryBlue) : (1dp, #3A3A3E);
    slot.cornerRadius = LocalCornerRadius;
    slot.onClick = () => ShowPopup(调色盘, 取色器); // DropdownMenu
    slot.onLongPress = () => SelectColor(color);
}

// 取色中指示器
if (eyedropperActive) {
    Icon eyedropperIcon = AddIcon(icon_eyedropper, size=20, color=#64B5F6);
}
```

---

## 4. 底部工具栏 (BottomStrip)

```csharp
// 位置: 屏幕底部, y=screen.h-48
// 尺寸: screen.w×48, 内边距 6×4dp

// 布局: [菜单|快捷键] ←flex→ [平移|+|−] ←flex→ [撤销|重做]

GameObject bottomStrip = CreateHorizontalLayout("BottomStrip");
bottomStrip.SetAnchor(AnchorPresets.BottomCenter);
bottomStrip.SetSize(screenWidth, 48);
bottomStrip.spacing = 4;
bottomStrip.padding = (6, 4);

// 左侧组
HGroup leftGroup = new HGroup(spacing=4);
    Button menu = new Button(icon_menu, 40, onClick: ShowMenu);
        // 菜单: 新建, 调整画布大小, 导出PNG, 设置
    Button shortcut = new Button(icon_shortcut, 40);
        shortcut.onClick = () => Execute(shortcutTapAction);
        shortcut.onLongPress = () => Execute(shortcutLongPressAction);
        shortcut.icon = GetShortcutIcon(shortcutTapAction);

// 中间组
FlexibleSpace();
Button pan = new Button(icon_move, 40, selected: panMode);
Button zoomIn = new Button(icon_plus, 40);
Button zoomOut = new Button(icon_minus, 40);
FlexibleSpace();

// 右侧组
HGroup rightGroup = new HGroup(spacing=4);
    Button undo = new Button(icon_undo, 40, enabled: canUndo);
    Button redo = new Button(icon_redo, 40, enabled: canRedo);
```

---

## 5. 子选项弹窗 (Sub-Selectors)

```csharp
// 所有子选项: 弹窗出现在对应工具按钮右侧
// 背景: #2A2A2E (95%不透明), 圆角 6dp, 内边距 6dp, 间距 4dp

// 画笔子选项
Panel BrushSelector() {
    width = 160; spacing = 4;
    Text("画笔大小: " + brushSize, fontSize=12, color=White);
    Slider(brushSize, 1..10, width=160);
    Toggle("像素完美(1px)", pixelPerfect, color=primary);
    if (pixelPerfect) {
        HGroup Toggle("正常", ppMode==NORMAL) Toggle("极端", ppMode==EXTREME);
    }
}

// 橡皮擦子选项
Panel EraserSelector() {
    width = 160; spacing = 4;
    Text("橡皮擦大小: " + eraserSize, fontSize=12, color=White);
    Slider(eraserSize, 1..10, width=160);
}

// 油漆桶子选项
Panel BucketSelector() {
    width = 160;
    Toggle("移除像素", bucketRemovePixels);
}

// 图形子选项
Panel ShapeSelector() {
    HGroup { icon_line("直线"), icon_rect("矩形"), icon_circle("圆形"),
             icon_leaf("柳叶"), icon_lasso("套索填充") };
    Separator(width=70, height=1, color=#66FFFFFF); // 40% 白线
    Toggle("空心填充", shapeFill, icon=icon_bucket);
}

// 选择工具子选项
Panel SelectSelector() {
    Toggle("方形选择", selectMode==RECT);
    Toggle("套索选择", selectMode==LASSO);
    if (hasSelection) {
        Separator(width=70, height=1, color=#66FFFFFF);
        HGroup { Button("确认", #4CAF50), Button("复制", #2196F3), Button("取消", #F44336) };
    }
}
```

---

## 6. HSV 取色器 (ColorPickerDialog)

```csharp
// 自定义 Dialog, 居中显示
// 外背景: 20dp padding + #1E1E22 + 圆角
// 内内容: 12dp padding

Dialog ColorPicker() {
    width = 436; // 内容区: 36+6+360+8+36 = 446dp, 外框 20dp
    padding = 20; // 背景缩进
    cornerRadius = LocalCornerRadius;

    // 第一行: 色相条 + SV方块
    HGroup {
        HueBar(width=36, height=360); // 垂直彩虹渐变
        Spacer(6);
        SvSquare(360, 360); // 饱和度/明度方块
    }

    // 第二行: 对比色 + 透明条
    HGroup {
        ColorComparison(width=36, height=36); // 左上原色 + 右下新色
        Spacer(6);
        AlphaBar(flex=1, height=36); // 水平透明条, 棋盘格底
    }

    // 第三行: 十六进制码
    HGroup {
        HexInput(width=70, height=36); // #RRGGBBAA, BasicTextField, 长按复制
        font = titleMedium;
    }

    // 第四行: 确定/取消
    HGroup(spacing=24) {
        Button("确定", bg=#4CAF50, padding=14, font=titleSmall);
        Button("取消", bg=#F44336, padding=14, font=titleSmall);
    }
}

// 色环 (长按SvSquare时显示)
ColorRing {
    show = isDragging;
    outerRadius = 192px; // 4倍放大
    innerRadius = 128px;
    topHalf = currentColor;    // Color.hsv(hue, sat, val)
    bottomHalf = previousColor; // Color.hsv(hue, initSat, initVal)
    center = dragPosition;
    crosshair = (12px, white 60%);
    outline = (white 3px outer, white 50% 2px inner);
    // 仅绘制在 ring band (128-192) 内, 中心透明
}
```

---

## 7. 设置对话框 (SettingsDialog)

```csharp
// Tab: 常用设置 | 网格与背景 | 特殊选项
// 每个Tab: 垂直列表, 间距 8dp

Tab 常用设置 {
    Label("快捷键", bg=#4DFFFFFF, padding=(10,5));
    Picker("单击动作", shortcutTapAction, icon);
    Picker("长按动作", shortcutLongPressAction, icon);
    Label("UI图图形", bg=#4DFFFFFF);
    Slider("UI圆角", 0..20, uiCornerRadius);
    Toggle("自动保存", autoSaveOnBackground);
}

Tab 网格与背景 {
    Toggle("显示网格", gridVisible);
}

Tab 特殊选项 {
    List("性能方案", perfMode, options: [
        "默认方案 (flatten)",
        "区域合成 (flattenRegion)",
        "异步合成 (后台)",
        "GPU合成 (HA)"
    ]);
}
```

---

## 设计常量

| 常量 | 值 |
|------|-----|
| 主背景色 | #1C1C1E |
| 弹窗背景 | #2A2A2E (95%不透明) |
| 标签背景 | #4DFFFFFF (30%透明白) |
| 选中高亮 | Material Primary (蓝色) |
| 文本颜色 | White |
| 辅助文本 | Gray |
| 按钮大小 | 40dp (主), 30dp (色块), 24dp (小) |
| 间距 | 4dp (默认), 6dp (弹窗), 2dp (紧凑) |
| 圆角 | LocalCornerRadius (默认 8dp) |
| 字体 | labelSmall(12), bodySmall(14), titleSmall(16), titleMedium(18) |