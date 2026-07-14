# endpix UI 完整布局规格 (Unity AI 可执行)

> 参考分辨率: 1080×2340 (竖屏手机), 坐标原点 (0,0) = 左上角
> 所有尺寸: Unity px (= Android dp × 3 for 3x density)
> 颜色格式: #RRGGBB 或 #AARRGGBB

---

## 整体结构树

```
Canvas (Screen Space Overlay, 1080×2340)
├── OpenGL_Canvas (RawImage, fillMaxSize, z=0)
├── TopStrips (VerticalLayout, TopStart, z=1)
│   ├── StatusBar (HorizontalLayout, wrap)
│   ├── DocStrip (ScrollView→HorizontalLayout, 32h)
│   └── FrameStrip (ScrollView→HorizontalLayout, 48h)
├── ToolColumn (VerticalLayout, CenterLeft, 48w, z=2)
│   ├── FlexibleSpace (top)
│   ├── PencilButton (40×40, badge: brushSize)
│   ├── EraserButton (40×40, badge: eraserSize)
│   ├── BucketButton (40×40, badge: red dot)
│   ├── SelectButton (40×40, icon dynamic)
│   ├── ShapeButton (40×40, icon dynamic)
│   └── FlexibleSpace (bottom)
├── PaletteBar (VerticalLayout, CenterRight, 48w, z=2)
│   ├── ColorSlot × N (30×30 each)
│   └── EyedropperIndicator (20×20, conditional)
├── LayerPanel (VerticalLayout, TopEnd, 96w, z=2)
│   ├── Header (icon + 4 buttons)
│   ├── LayerList (scrollable)
│   └── OpacitySlider (conditional)
├── BottomStrip (HorizontalLayout, BottomCenter, z=2)
│   ├── MenuButton (40×40)
│   ├── ShortcutButton (40×40)
│   ├── FlexibleSpace
│   ├── PanButton (40×40)
│   ├── ZoomInButton (40×40)
│   ├── ZoomOutButton (40×40)
│   ├── FlexibleSpace
│   ├── UndoButton (40×40)
│   └── RedoButton (40×40)
├── SubSelectors (z=3, conditional)
│   ├── ShapeSelector (at SHAPE y-pos)
│   ├── BrushSelector (at PENCIL y-pos)
│   ├── EraserSelector (at ERASER y-pos)
│   ├── BucketSelector (at BUCKET y-pos)
│   └── SelectSelector (at SELECT y-pos)
└── Dialogs (z=4, conditional)
    ├── ColorPickerDialog (center)
    ├── NewCanvasDialog (center)
    ├── ResizeCanvasDialog (center)
    └── SettingsDialog (center)
```

---

## 1. 主画布 (EditorScreen)

```csharp
// 最底层: OpenGL 渲染 (Unity 中用 RawImage + RenderTexture)
RawImage canvas = new RawImage();
canvas.texture = renderTexture; // 从 PixelCanvas 上传
canvas.rectTransform = new RectTransform(0, 0, 1080, 2340);
canvas.anchorMin = (0, 0); canvas.anchorMax = (1, 1);
```

---

## 2. StatusBar (左上角信息栏)

```csharp
// 位置: TopStart, auto-size
// 背景: 30%透明黑 (#4D000000), 圆角 8dp
// 内边距: horizontal 8, vertical 3

VerticalLayout statusBar = new VerticalLayout();
statusBar.SetAnchor(AnchorPresets.TopLeft);
statusBar.backgroundColor = new Color(0,0,0,0.3f); // #4D000000
statusBar.cornerRadius = 8;
statusBar.padding = new RectOffset(8, 8, 3, 3);

// 第一行: 画布尺寸 + 帧数 + 缩放
HorizontalLayout row1 = new HorizontalLayout();
row1.AddText("32×32", fontSize=12, color=White);
row1.AddSpace(8);
row1.AddText("1帧", fontSize=12, color=White);
row1.AddSpace(8);
row1.AddText("100%", fontSize=12, color=#2196F3); // primary blue

// 第二行: 性能数据
HorizontalLayout row2 = new HorizontalLayout();
row2.AddText("FPS:60", fontSize=12, color=#88CC88);
row2.AddSpace(8);
row2.AddText("CPU:5%", fontSize=12, color=#88AAFF);
row2.AddSpace(8);
row2.AddText("GPU:12%", fontSize=12, color=#FFAA88);
```

---

## 3. DocStrip (文档标签)

```csharp
// 位置: TopStart, 紧接 StatusBar
// 横向滚动, 高 32, 间距 4, 内边距 (4,2)

ScrollView docStrip = new ScrollView(horizontal: true);
docStrip.content = new HorizontalLayout(spacing: 4, padding: (4,2));

foreach (doc in documents) {
    Button tab = new Button(doc.name, fontSize=12);
    tab.height = 28;
    tab.bg = (selected) ? primaryBlue : #B32A2A2E;
    tab.textColor = (selected) ? White_OnPrimary : White;
    tab.cornerRadius = 8;
    tab.onClick = () => SelectDocument(index);
    tab.AddIcon(icon_close, 16, onClick: () => CloseDocument(index));
}

Button addBtn = new Button(icon_plus, size=30, bg=#B32A2A2E);
```

---

## 4. FrameStrip (帧标签)

```csharp
// 位置: TopStart, 紧接 DocStrip
// 横向滚动, 高 48, 间距 4, 内边距 (4,2)

ScrollView frameStrip = new ScrollView(horizontal: true);
frameStrip.content = new HorizontalLayout(spacing: 4, padding: (4,2));

foreach (frame in frames) {
    Button frameBtn = new Button();
    frameBtn.size = 40×40;
    frameBtn.bg = (selected) ? primaryBlue : #B32A2A2E;
    frameBtn.image = frameThumbnail; // 48×48 缩略图
    frameBtn.AddText(frameIndex.ToString(), fontSize=12, bg=#80000000);
}

Button addFrame = new Button(icon_plus, 30);
Button dupFrame = new Button(icon_duplicate, 30);
Button delFrame = new Button(icon_close, 30);
```

---

## 5. ToolColumn (左侧工具栏)

```csharp
// 位置: CenterLeft, x=4, 宽 48, 高 fill
// 垂直布局, 上下 FlexibleSpace, 间距 4, 内边距 4

VerticalLayout toolColumn = new VerticalLayout();
toolColumn.SetAnchor(AnchorPresets.MiddleLeft);
toolColumn.SetSize(48, parent.height - 200);
toolColumn.SetPosition(4, 100);
toolColumn.spacing = 4;
toolColumn.padding = 4;

// 铅笔按钮
Button pencil = new Button(icon_pencil, 40×40);
pencil.bg = (tool==PENCIL) ? primaryBlue : #B32A2A2E;
pencil.onClick = () => { if(tool!=PENCIL) SelectTool(PENCIL); ToggleBrushSelector(); };
pencil.badge = (brushSize).ToString(); // 右下角, fontSize=12, color=#64B5F6, bg=#1A1A1E
pencil.badge.show = brushSize > 1;

// 橡皮按钮
Button eraser = new Button(icon_eraser, 40×40);
eraser.bg = (tool==ERASER) ? primaryBlue : #B32A2A2E;
eraser.onClick = () => { if(tool!=ERASER) SelectTool(ERASER); ToggleEraserSelector(); };
eraser.badge = (eraserSize).ToString();
eraser.badge.show = eraserSize > 1;

// 油漆桶按钮
Button bucket = new Button(icon_bucket, 40×40);
bucket.bg = (tool==BUCKET) ? primaryBlue : #B32A2A2E;
bucket.onClick = () => SelectTool(BUCKET);
bucket.badge = "●"; // 红点 #F44336, 8×8
bucket.badge.show = bucketRemovePixels;

// 选择按钮
Button select = new Button(icon_select, 40×40);
select.bg = (tool==SELECT) ? primaryBlue : #B32A2A2E;
select.icon = (selectMode==RECT) ? icon_select_rect : icon_select_lasso;
select.onClick = () => { if(tool!=SELECT) SelectTool(SELECT); ToggleSelectSelector(); };

// 图形按钮
Button shape = new Button(icon_shape, 40×40);
shape.bg = (tool==SHAPE) ? primaryBlue : #B32A2A2E;
shape.icon = GetShapeIcon(); // 根据 shapeMode 和 shapeFill 动态切换
shape.onClick = () => { if(tool!=SHAPE) SelectTool(SHAPE); ToggleShapeSelector(); };
```

---

## 6. PaletteBar (右侧调色板)

```csharp
// 位置: CenterRight, x=parent.w-52, 宽 48, 高 fill
// 垂直布局, 垂直居中, 内边距 4

VerticalLayout paletteBar = new VerticalLayout();
paletteBar.SetAnchor(AnchorPresets.MiddleRight);
paletteBar.SetSize(48, parent.height - 200);
paletteBar.SetPosition(parent.width - 52, 100);
paletteBar.spacing = 2;

foreach (int idx, Color c in palette) {
    GameObject slot = new GameObject("ColorSlot_" + idx);
    slot.AddComponent<Image>().color = c;
    slot.rectTransform.sizeDelta = new Vector2(30, 30);
    slot.cornerRadius = LocalCornerRadius;
    // 边框: 选中 2dp primaryBlue, 未选中 1dp #3A3A3E
    slot.border = (c == selectedColor) ? (2, primaryBlue) : (1, #3A3A3E);
    slot.onClick = () => {
        if (eyedropperActive) { SelectColor(c); eyedropperActive = false; }
        else ShowPopup(idx, ["调色盘", "取色器"]); // DropdownMenu
    };
    slot.onLongPress = () => SelectColor(c);
}

// 取色器指示器
if (eyedropperActive) {
    AddIcon(icon_eyedropper, size=20, color=#64B5F6);
}
```

---

## 7. LayerPanel (图层面板, 右上角)

```csharp
// 位置: TopEnd, y=topStripsH, 宽 96, 内边距 4
// 背景: #2A2A2E (95%不透明), 圆角, 内边距 6

VerticalLayout layerPanel = new VerticalLayout();
layerPanel.SetAnchor(AnchorPresets.TopRight);
layerPanel.SetSize(96, auto);
layerPanel.SetPosition(0, topStripsHeight);
layerPanel.padding = new RectOffset(4, 4, 4, 4);
layerPanel.backgroundColor = new Color(0.18f, 0.18f, 0.22f, 0.95f); // #E62A2A2E
layerPanel.cornerRadius = LocalCornerRadius;

// 头部
HorizontalLayout header = new HorizontalLayout();
header.AddIcon(icon_layers, 16, White);
header.AddFlexibleSpace();
header.AddButton(icon_plus, 24, onClick: AddLayer);
header.AddButton(icon_arrow_up, 24, onClick: MoveLayerUp);
header.AddButton(icon_arrow_down, 24, onClick: MoveLayerDown);
header.AddButton(icon_close, 24, onClick: DeleteLayer);

// 图层列表 (从下到上)
foreach (layer in layers.Reversed()) {
    HorizontalLayout layerRow = new HorizontalLayout();
    layerRow.bg = (selected) ? primaryBlue_Container : #B32A2A2E;
    layerRow.border = (selected) ? (1, primaryBlue) : null;
    layerRow.cornerRadius = LocalCornerRadius;
    layerRow.padding = 3;
    layerRow.AddImage(layer.thumbnail, 20×20); // 或 20×20 灰色方块
    layerRow.AddText(layer.name, fontSize=12, White, maxLines=1);
    layerRow.AddFlexibleSpace();
    layerRow.AddIcon(layer.visible ? icon_eye : icon_eye_off, 14, onClick: ToggleVisible);
    layerRow.spacing = 2;
    layerRow.onClick = () => SelectLayer(index);
}

// 不透明度滑块 (仅 >1 层)
if (layers.Count > 1) {
    AddText("不透明 " + (active.opacity*100) + "%", fontSize=12, White);
    AddSlider(active.opacity, 0..1, onChange: SetOpacity);
}
```

---

## 8. BottomStrip (底部工具栏)

```csharp
// 位置: BottomCenter, 宽 fill, 高 48
// 内边距 (6,4), 横向布局

HorizontalLayout bottomStrip = new HorizontalLayout();
bottomStrip.SetAnchor(AnchorPresets.BottomCenter);
bottomStrip.SetSize(parent.width, 48);
bottomStrip.padding = new RectOffset(6, 6, 4, 4);
bottomStrip.spacing = 4;

// 菜单按钮 (左侧)
Button menu = new Button(icon_menu, 40×40);
menu.onClick = () => ShowDropdownMenu(["新建", "调整画布大小", "导出 PNG", "设置"]);

// 快捷键按钮
Button shortcut = new Button(shortcutIcon, 40×40);
shortcut.bg = #2A2A2E;
shortcut.icon = GetShortcutIcon(shortcutTapAction);
shortcut.onClick = () => ExecuteShortcut(shortcutTapAction);
shortcut.onLongPress = () => ExecuteShortcut(shortcutLongPressAction);

// 中间分隔
AddFlexibleSpace();
Button pan = new Button(icon_move, 40×40, selected: panMode);
Button zoomIn = new Button(icon_plus, 40×40);
Button zoomOut = new Button(icon_minus, 40×40);

// 右侧分隔
AddFlexibleSpace();
Button undo = new Button(icon_undo, 40×40, enabled: canUndo);
Button redo = new Button(icon_redo, 40×40, enabled: canRedo);
```

---

## 9. 子选择器 (Sub-Selector 弹窗)

```csharp
// 所有子选择器: 出现在对应工具按钮右侧
// 位置: TopStart, x=52, y=按钮Y位置
// 背景: #2A2A2E (95%), 圆角 6dp, 内边距 6dp, 间距 4dp

// ===== ShapeSelector =====
Panel shapeSelector = new Panel(width=auto, bg=#E62A2A2E, cornerRadius=6, padding=2, spacing=1);
foreach (mode in [LINE, RECTANGLE, CIRCLE, LEAF, LASSO]) {
    Row option = new Row(height=30, cornerRadius=6, padding=(6,6));
    option.bg = (selected) ? primaryBlue : Transparent;
    option.AddIcon(GetShapeIcon(mode), 16, (selected) ? onPrimary : White);
    option.AddText(GetShapeName(mode), fontSize=12, (selected) ? onPrimary : White);
    option.onClick = () => SelectShapeMode(mode);
}
AddSeparator(width=70, height=1, color=#66FFFFFF); // 40% 白线
Row fillToggle = new Row(height=30, cornerRadius=6, padding=(6,6));
fillToggle.bg = (shapeFill) ? primaryBlue : Transparent;
fillToggle.AddIcon(icon_bucket, 16);
fillToggle.AddText("空心填充", fontSize=12);
fillToggle.onClick = () => shapeFill = !shapeFill;

// ===== BrushSelector =====
Panel brushSelector = new Panel(width=160, bg=#E62A2A2E, padding=6, spacing=4);
brushSelector.AddText("画笔大小: " + brushSize, fontSize=12, White);
brushSelector.AddSlider(brushSize, 1..10, width=160);
brushSelector.AddToggle("像素完美(1px)", pixelPerfect, color=primary);
if (pixelPerfect) {
    brushSelector.AddHGroup(spacing=4) {
        brushSelector.AddToggle("正常", ppMode==NORMAL);
        brushSelector.AddToggle("极端", ppMode==EXTREME);
    };
}

// ===== EraserSelector =====
Panel eraserSelector = new Panel(width=160, bg=#E62A2A2E, padding=6, spacing=4);
eraserSelector.AddText("橡皮擦大小: " + eraserSize, fontSize=12, White);
eraserSelector.AddSlider(eraserSize, 1..10, width=160);

// ===== BucketSelector =====
Panel bucketSelector = new Panel(width=160, bg=#E62A2A2E, padding=6, spacing=4);
bucketSelector.AddToggle("移除像素", bucketRemovePixels);

// ===== SelectSelector =====
Panel selectSelector = new Panel(width=160, bg=#E62A2A2E, padding=6, spacing=4);
selectSelector.AddToggle("方形选择", selectMode==RECT);
selectSelector.AddToggle("套索选择", selectMode==LASSO);
if (hasSelection) {
    selectSelector.AddSeparator(width=70, height=1, color=#66FFFFFF);
    selectSelector.AddHGroup(["确认", "复制", "取消"],
        colors: [#4CAF50, #2196F3, #F44336],
        textColor: Black,
        fontSize: 12);
}
```

---

## 10. HSV 取色器 (ColorPickerDialog)

```csharp
// 自定义 Dialog, 居中, 背景半透明遮罩
// 外框: 20dp padding + #1E1E22 + 圆角
// 内容: 12dp padding, 间距 2dp

Dialog colorPicker = new Dialog();
colorPicker.SetAnchor(AnchorPresets.MiddleCenter);
colorPicker.overlay = new Color(0,0,0,0.5f); // 半透明遮罩
colorPicker.usePlatformDefaultWidth = false;

// 背景层
Box background = new Box();
background.padding = 20;
background.backgroundColor = #1E1E22;
background.cornerRadius = LocalCornerRadius;

// 内容层
VerticalLayout content = new VerticalLayout(spacing=2, padding=12);
content.size = 360 + 36 + 6; // 402dp 宽

// 第一行: 色相条 + SV方块
HorizontalLayout row1 = new HorizontalLayout();
row1.AddHueBar(width=36, height=360); // 垂直彩虹渐变, 白色横条指示器
row1.AddSpace(6);
row1.AddSvSquare(360, 360); // 饱和度/明度方块, 空心圆指示器

// 第二行: 对比色 + 透明条
HorizontalLayout row2 = new HorizontalLayout();
row2.AddColorComparison(36, 36); // 左上原色 + 右下新色, 两个小方块
row2.AddSpace(6);
row2.AddAlphaBar(flex=1, height=36); // 水平透明条, 棋盘格底, 白色竖线指示器

// 第三行: 十六进制码
HexInput hexInput = new HexInput(width=70, height=36);
hexInput.font = titleMedium; // 18px
hexInput.format = "#RRGGBBAA"; // 8位含透明度
hexInput.onChange = ParseHex;
hexInput.onLongPress = CopyToClipboard;

// 第四行: 确定/取消
HorizontalLayout buttons = new HorizontalLayout(spacing=24);
buttons.AddButton("确定", bg=#4CAF50, padding=14, font=titleSmall); // 16px
buttons.AddButton("取消", bg=#F44336, padding=14, font=titleSmall);

// 色环 (长按SvSquare时显示)
// outerRadius=192px, innerRadius=128px
// 上半=当前色, 下半=原色, 中心透明+十字
// 仅显示在 ring band (128-192) 内
```

---

## 11. 设置对话框 (SettingsDialog)

```csharp
// 位置: 居中
// 顶部: Tab 切换 (常用设置 | 网格与背景 | 特殊选项)
// 底部: 关闭按钮

Dialog settingsDialog = new Dialog();
settingsDialog.width = 300;

// Tab 切换
HorizontalLayout tabs = new HorizontalLayout(spacing=6);
foreach (name in ["常用设置", "网格与背景", "特殊选项"]) {
    Button tab = new Button(name, fontSize=12);
    tab.bg = (selected) ? primaryBlue : #2A2A2E;
    tab.textColor = (selected) ? onPrimary : White;
    tab.cornerRadius = LocalCornerRadius;
    tab.padding = (10, 6);
}

// ===== Tab 0: 常用设置 =====
VerticalLayout tab0 = new VerticalLayout(spacing=8);

// 快捷键标签
Panel shortcutLabel = new Panel(bg=#4DFFFFFF, padding=(10,5), cornerRadius=6);
shortcutLabel.AddText("快捷键", fontSize=14, White);

// 单击动作选择器
ShortcutPicker tapPicker = new ShortcutPicker("单击动作", shortcutTapAction);
tapPicker.icon = GetShortcutIcon(action);
tapPicker.onSelect = (action) => shortcutTapAction = action;

// 长按动作选择器
ShortcutPicker longPicker = new ShortcutPicker("长按动作", shortcutLongPressAction);

// UI图图形标签
Panel uiLabel = new Panel(bg=#4DFFFFFF, padding=(10,5), cornerRadius=6);
uiLabel.AddText("UI图图形", fontSize=14, White);

// UI圆角滑块
Slider cornerSlider = new Slider("UI圆角: " + uiCornerRadius, 0..20);
cornerSlider.value = uiCornerRadius;
cornerSlider.onChange = (v) => uiCornerRadius = v;

// 自动保存开关
Toggle autoSave = new Toggle("自动保存", autoSaveOnBackground);
autoSave.desc = "进后台时自动保存画布";

// ===== Tab 1: 网格与背景 =====
Toggle gridToggle = new Toggle("显示网格", gridVisible);

// ===== Tab 2: 特殊选项 =====
VerticalLayout tab2 = new VerticalLayout(spacing=8);
tab2.AddText("性能方案", fontSize=14, White);
foreach (mode in [DEFAULT, REGION, ASYNC, HA]) {
    Panel modePanel = new Panel(bg=(selected)?primaryBlue:Transparent, cornerRadius=6, padding=(8,6));
    modePanel.AddText(mode.label, fontSize=14);
    modePanel.AddText(mode.desc, fontSize=12, color=(selected)?onPrimary:Gray);
    modePanel.onClick = () => perfMode = mode;
}
```

---

## 12. 设计常量

| 常量 | 值 | 说明 |
|------|-----|------|
| ScreenSize | 1080×2340 | 参考分辨率 |
| PrimaryColor | #2196F3 | 选中高亮蓝 |
| PrimaryOnColor | White | 高亮上的文字色 |
| CellBg | #B32A2A2E | 未选中按钮背景 |
| PopupBg | #E62A2A2E | 弹窗背景 (95%不透明) |
| LabelBg | #4DFFFFFF | 标签栏背景 (30%透明白) |
| InfoBg | #4D000000 | 信息栏背景 (30%透明黑) |
| CornerRadius | 8dp | 默认圆角 (可调) |
| ButtonSize | 40dp | 主按钮大小 |
| ColorSlotSize | 30dp | 色块大小 |
| SmallButtonSize | 24dp | 小按钮 (图层操作) |
| IconSize | 22dp | 按钮内图标 (55% of 40) |
| Spacing | 4dp | 默认间距 |
| DialogSpacing | 6dp | 弹窗内间距 |
| CompactSpacing | 2dp | 紧凑间距 |
| SVSquareSize | 360dp | HSV 方块大小 |
| HueBarWidth | 36dp | 色相条宽度 |
| AlphaBarHeight | 36dp | 透明条高度 |
| FontLabelSmall | 12px | 标签/状态文字 |
| FontBodySmall | 14px | 正文小字 |
| FontTitleSmall | 16px | 按钮文字 |
| FontTitleMedium | 18px | 标题文字 |