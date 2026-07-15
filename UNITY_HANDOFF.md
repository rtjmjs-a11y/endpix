# endpix → Unity 交接报告 (Kilo Code 对 Kilo Code)

> **给 VS Code 里的 Kilo Code (Unity):**
> 你好，我是 Android Studio 里的 Kilo Code。我们是同一个 AI。
> 下面是 endpix 像素编辑器在 **真机手机竖屏** 上的精确 UI 数据。
> **禁止使用电脑版/桌面布局**。这个 App 是纯竖屏手机应用，所有尺寸基于手机触摸屏。
> 如果你用桌面分辨率做，按钮会变得巨大、间距离谱、完全不可用。
> 
> **目标设备参数（必须按这个做）:**
> - 机型: vivo V2314A
> - 物理分辨率: 1080 × 2400 px
> - 屏幕密度: 400 DPI (densityDpi)
> - density 倍率: 2.75 (1 dp = 2.75 px)
> - Android: 15 (API 35)
> - 竖屏 Portrait Only
> 
> **Unity Canvas Scaler 设置:**
> - Reference Resolution: 1080 × 2400
> - Screen Match Mode: Match Width Or Height = 0.5
> - 这样 Unity 里 1 unit ≈ 1 px = 1/2.75 dp

---

## dp → px 换算表（density = 2.75）

| dp | px (×2.75) | 用途 |
|----|-----------|------|
| 1  | 2.75      | 最小间距/线宽 |
| 2  | 5.5       | 紧凑间距 |
| 3  | 8.25      | 小内边距 |
| 4  | 11        | 默认间距 |
| 6  | 16.5      | 弹窗间距 |
| 8  | 22        | 标准间距 |
| 12 | 33        | 大内边距 |
| 14 | 38.5      | 按钮内边距 |
| 16 | 44        | 图标大小 |
| 20 | 55        | 缩略图/小图标 |
| 22 | 60.5      | 按钮内图标 |
| 24 | 66        | 滑块高度 |
| 28 | 77        | 透明条高度 |
| 30 | 82.5      | 色块大小 |
| 36 | 99        | 色相条宽/透明条高 |
| 40 | 110       | 工具按钮大小 |
| 48 | 132       | 工具栏/调色板宽度 |
| 52 | 143       | 子选项偏移量 |
| 96 | 264       | 图层面板宽度 |
| 100| 275       | 滑块宽度 |
| 160| 440       | 子选项面板宽度 |
| 240| 660       | SV方块大小(旧) |
| 360| 990       | SV方块大小(新) |

---

## 字体大小对照

| Android style | 实际大小 (sp) | Unity 字号 | 用途 |
|---------------|-------------|-----------|------|
| labelSmall | 11sp | 11 | 状态栏/标签/角标 |
| bodySmall | 12sp | 12 | 正文小字/像素完美 |
| bodyMedium | 14sp | 14 | 正文 |
| titleSmall | 16sp | 16 | 按钮文字(确定/取消) |
| titleMedium | 18sp | 18 | 十六进制码输入框 |

---

## 全屏布局（竖屏手机）

```
┌──────────────────────────────────────────┐
│ 状态栏 (系统)          高度约 66px (24dp)  │ ← 系统状态栏
├──────────────────────────────────────────┤ y = 66px
│                                          │
│  ┌────────────┐                          │
│  │ StatusBar  │  wrap×wrap               │ ← 左上角浮层
│  │ #4D000000  │  padding(8,3)            │
│  └────────────┘                          │
│                                          │
│  ┌─DocStrip──────────────────────────┐   │ ← 横向滚动
│  │ [Doc1×] [Doc2×] [+]              │   │   高约 32dp
│  └───────────────────────────────────┘   │
│                                          │
│  ┌─FrameStrip───────────────────────┐   │ ← 横向滚动
│  │ [ thumb1 ] [ thumb2 ] [+] [⎘] [×]│   │   高约 48dp
│  └───────────────────────────────────┘   │
│                                          │
├────┬──────────────────────────┬─────────┤
│    │                          │         │
│ T  │                          │  调色   │
│ o  │     全屏 Canvas          │  板     │
│ o  │   (OpenGL/RenderTexture) │         │
│ l  │                          │  48dp   │
│    │                          │  宽     │
│ 48 │                          │         │
│ dp │                          │         │
│ 宽 │                          │         │
│    │                          │ ┌─────┐ │
│    │                          │ │图层 │ │ ← 右上角浮层
│    │                          │ │面板 │ │   96dp宽
│    │                          │ └─────┘ │
│    │                          │         │
├────┴──────────────────────────┴─────────┤
│ [☰] [🔧]        [♟] [+] [−]      [↩] [↪]│ ← 底部栏
│                                          │   高约 48dp
├──────────────────────────────────────────┤
│ 导航栏 (系统)          高度约 66px       │ ← 系统导航栏
└──────────────────────────────────────────┘
```

---

## 逐组件精确数据

### A. 全屏 Box (根容器)

```
fillMaxSize: 1080 × (2400 - 状态栏 - 导航栏) px
背景色: 透明 (Canvas 在底层)
z-order: 0 = Canvas, 1 = TopStrips, 2 = 工具栏/调色板/图层/底部栏, 3 = 子选项, 4 = 对话框
```

---

### B. StatusBar (左上角信息栏)

```
锚点: TopStart (左上角)
位置: 紧贴顶部 (statusBarsPadding 之后)
背景: #4D000000 (30% 透明黑)
圆角: 8dp (= LocalCornerRadius)
内边距: horizontal=8dp(22px), vertical=3dp(8.25px)

布局: VerticalLayout (两行)

第一行 (HorizontalLayout):
  Text "32×32"     font=11sp  color=#FFFFFF   spacing=8dp
  Text "1帧"       font=11sp  color=#FFFFFF   spacing=8dp
  Text "100%"      font=11sp  color=#2196F3

第二行 (HorizontalLayout):
  Text "FPS:60"    font=11sp  color=#88CC88   spacing=8dp
  Text "CPU:5%"    font=11sp  color=#88AAFF   spacing=8dp
  Text "GPU:12%"   font=11sp  color=#FFAA88

总高度: 约 50px (两行 11sp 文字 + 6dp 内边距)
总宽度: wrap (约 200dp)
```

---

### C. DocStrip (文档标签栏)

```
锚点: TopStart (紧接 StatusBar 下方)
宽度: fillMaxWidth (1080px)
高度: wrap (约 32dp)
内边距: horizontal=4dp, vertical=2dp
间距: 4dp

每个文档标签:
  背景: (选中) ? #2196F3 : #B32A2A2E
  圆角: 8dp
  内边距: start=8dp, top=4dp, bottom=4dp, end=2dp
  内容: HorizontalLayout {
    Text(doc.name, font=11sp, color=选中?白:白)
    Icon(ic_close, 16dp, color=选中?白:白)
  }

末尾: Button(ic_plus, 30dp, bg=#B32A2A2E)
```

---

### D. FrameStrip (帧标签栏)

```
锚点: TopStart (紧接 DocStrip 下方)
宽度: fillMaxWidth
高度: wrap (约 48dp)
内边距: horizontal=4dp, vertical=2dp
间距: 4dp

每个帧:
  尺寸: 40×40dp
  背景: (选中) ? #2196F3 : #B32A2A2E
  圆角: 8dp
  内容: Image(缩略图, 40×40dp) + Text(序号, 11sp, bg=#80000000, padding=2dp)

末尾: Button(ic_plus, 30dp), Button(ic_duplicate, 30dp), Button(ic_close, 30dp)
```

---

### E. ToolColumn (左侧工具栏)

```
锚点: CenterStart (垂直居中, 左贴边)
位置: x=0, y=center
宽度: 48dp (132px)
高度: fillMaxHeight
内边距: 4dp (11px)
间距: 4dp (11px)
布局: VerticalLayout

内容 (从上到下):
  FlexibleSpace (weight=1) ← 上方撑满
  PencilButton     40×40dp   bg=(选中)#2196F3:(未选中)#B32A2A2E
  EraserButton     40×40dp
  BucketButton     40×40dp
  SelectButton     40×40dp
  ShapeButton      40×40dp
  FlexibleSpace (weight=1) ← 下方撑满

每个按钮:
  尺寸: 40dp × 40dp (110×110px)
  圆角: 8dp
  图标: size × 0.55 = 22dp (60.5px)
  图标颜色: (选中) #FFFFFF : (未选中) #FFFFFF
  背景: (选中) #2196F3 : (未选中) #B32A2A2E : (禁用) #22FFFFFF
  图标禁用色: #FFFFFF66

角标 (badge, 右下角):
  位置: BottomEnd, offset(x=2dp, y=-2dp)
  字体: 11sp, color=#64B5F6
  背景: #DD1A1A1E, 圆角 8dp, padding(horizontal=3dp)
  显示条件: brushSize > 1 或 eraserSize > 1

油漆桶红点:
  位置: BottomEnd, offset(x=2dp, y=-2dp)
  尺寸: 8×8dp
  颜色: #F44336
  圆角: 8dp
  显示条件: bucketRemovePixels == true
```

---

### F. PaletteBar (右侧调色板)

```
锚点: CenterEnd (垂直居中, 右贴边)
位置: x=parent.right-48dp, y=center
宽度: 48dp (132px)
高度: fillMaxHeight
内边距: 4dp
垂直排列: Center

每个色块:
  尺寸: 30×30dp (82.5×82.5px)
  垂直间距: 2dp (5.5px) ← padding(vertical=2dp)
  圆角: 8dp
  边框: (选中) 2dp #2196F3 : (未选中) 1dp #3A3A3E
  背景: 用户颜色

色块交互:
  onClick: 
    if (eyedropperActive) { selectColor; eyedropperActive=false }
    else { showPopup(调色盘/取色器) }
  onLongClick: selectColor

Popup (DropdownMenu):
  选项1: Icon(ic_palette,18dp) + Text("调色盘", 12sp, 白)
  选项2: Icon(ic_eyedropper,18dp) + Text("取色器", 12sp, 白)

取色中指示器:
  显示条件: eyedropperActive
  Icon(ic_eyedropper, 20dp, color=#64B5F6)
  间距: 4dp
```

---

### G. LayerPanel (右上角图层面板)

```
锚点: TopEnd (右贴边, y=topStripsHeight)
宽度: 96dp (264px)
内边距: 4dp → 圆角/背景 → 6dp (双层 padding)
背景: #E62A2A2E (95%不透明深灰)
圆角: 8dp

头部 (HorizontalLayout, SpaceBetween):
  Icon(ic_layers, 16dp, 白)
  Row {
    Button(ic_plus, 24dp)       spacing=2dp
    Button(ic_arrow_up, 24dp)   spacing=2dp
    Button(ic_arrow_down, 24dp) spacing=2dp
    Button(ic_close, 24dp)
  }

图层列表 (从下到上 reversed):
  每个图层行:
    fillMaxWidth, 圆角 8dp
    背景: (选中) #primaryContainer : #B32A2A2E
    边框: (选中) 1dp #2196F3 : 无
    内边距: 3dp
    内容: HorizontalLayout {
      Image(缩略图, 20×20dp) 或 Box(20×20dp, #3A3A3E)
      Text(layer.name, 11sp, 白, maxLines=1)
      FlexibleSpace
      Icon(ic_eye/ic_eye_off, 14dp, 白)
    }
    间距: 2dp

不透明度滑块 (仅 layers.Count > 1):
  Text("不透明 XX%", 11sp, 白)
  Slider(0..1, 默认全宽)
```

---

### H. BottomStrip (底部工具栏)

```
锚点: BottomCenter (底部居中, navigationBarsPadding)
宽度: fillMaxWidth
高度: wrap (约 48dp)
内边距: horizontal=6dp, vertical=4dp
垂直对齐: Bottom

布局: HorizontalLayout {
  左侧组:
    VerticalLayout(spacing=4dp) {
      Button(ic_menu, 40dp) → 弹出菜单[新建/调整画布/导出PNG/设置]
      Button(shortcut, 40dp, bg=#2A2A2E) → onClick=快捷动作, onLongPress=长按动作
    }
  
  FlexibleSpace (weight=1)
  
  中间组:
    Button(ic_move, 40dp, selected=panMode)
    Button(ic_plus, 40dp)    ← 放大
    Button(ic_minus, 40dp)   ← 缩小
  
  FlexibleSpace (weight=1)
  
  右侧组:
    Row(spacing=4dp) {
      Button(ic_undo, 40dp, enabled=canUndo)
      Button(ic_redo, 40dp, enabled=canRedo)
    }
}
```

---

### I. 子选项弹窗 (所有通用)

```
位置: TopStart, offset(x=52dp, y=对应按钮Y位置)
背景: #E62A2A2E (95%不透明)
圆角: 8dp
内边距: 6dp (部分 2dp)
间距: 4dp (部分 1dp)

--- ShapeSelector ---
  内边距: 2dp, 间距: 1dp
  每个选项: Row(height=30dp, padding=6dp, 圆角8dp)
    Icon(16dp) + Text(11sp)
    选中bg: #2196F3
  分隔线: width=70dp, height=1dp, bg=#66FFFFFF
  空心填充: Row(height=30dp), Icon(ic_bucket,16dp) + Text

--- BrushSelector ---
  宽度: 160dp
  Text("画笔大小: X", 11sp, 白)
  Row(height=24dp, width=160dp) { Text("1") Slider(1-10) Text("10") }
  Toggle("像素完美(1px)", width=160dp, padding=6dp, font=12sp)
  if(像素完美): Row(width=160dp, spacing=4dp) { "正常" "极端" } 各 weight=1

--- EraserSelector ---
  宽度: 160dp
  Text("橡皮擦大小: X", 11sp, 白)
  Row(height=24dp, width=160dp) { Text("1") Slider(1-10) Text("10") }

--- BucketSelector ---
  宽度: 160dp
  Toggle("移除像素", width=160dp, font=12sp)

--- SelectSelector ---
  宽度: 160dp
  Toggle("方形选择", width=160dp)
  Toggle("套索选择", width=160dp)
  if(hasSelection):
    分隔线(70dp, 1dp, #66FFFFFF)
    Row(width=160dp, spacing=4dp) {
      "确认"(#4CAF50)  "复制"(#2196F3)  "取消"(#F44336)
    } 各 weight=1, font=11sp, Black字, padding=4dp, 居中
```

---

### J. HSV 取色器 (ColorPickerDialog)

```
容器: Dialog (居中, 半透明遮罩)
外框: padding=20dp, bg=#1E1E22, 圆角=8dp
内容: padding=12dp, spacing=2dp

内容宽度: 360(HueBar36+6+SvSquare360) = 402dp → 约 1106px

第一行: Row {
  HueBar: width=36dp, height=360dp, 垂直彩虹渐变
    指示器: 白色横条, width=80%, height=3dp, y=(1-hue)*height
  Spacer: 6dp
  SvSquare: 360×360dp
    背景层1: 水平渐变 白→hueColor
    背景层2: 垂直渐变 透明→黑
    指示器: 空心圆 r=8px 外圈 + r=5px 内圈当前色
    指示器颜色: 亮度>0.5→黑色轮廓, ≤0.5→白色轮廓
    长按拖拽时: 显示色环 (见下)
}

色环 (仅长按拖拽时显示):
  外半径: 192px
  内半径: 128px
  上半: 当前色 Color.hsv(hue, sat, val)
  下半: 拖拽前色 Color.hsv(hue, initSat, initVal)
  中心: 透明 + 十字(12px臂长, 白60%)
  外环描边: 白3px, 内环描边: 白50% 2px
  跟随手指, 可超出SV方块

第二行: Row {
  对比色: 36×36dp
    左上半: 原色(initialColor)
    右下半: 新色(newColor)
  Spacer: 6dp
  AlphaBar: weight=1, height=36dp
    底层: 4×4px棋盘格(白/灰交替)
    上层: 水平渐变 透明→当前色
    指示器: 白色竖线, width=3dp, height=80%
}

第三行: Row {
  HexInput: width=70dp(min), height=auto
    格式: "#RRGGBBAA"
    字体: 18sp (titleMedium)
    边框: 1dp #66FFFFFF, 圆角 8dp
    背景: #18FFFFFF
    内边距: horizontal=6dp, vertical=4dp
    长按: 复制到剪贴板
}

第四行: Row {
  padding: horizontal=8dp, offset y=4dp
  Button("确定"): weight=1, bg=#4CAF50, padding=14dp, font=16sp
  Spacer: 24dp
  Button("取消"): weight=1, bg=#F44336, padding=14dp, font=16sp
}
```

---

### K. 设置对话框 (SettingsDialog)

```
容器: AlertDialog (居中)
宽度: ~300dp

Tab 栏: HorizontalLayout(spacing=6dp)
  每个 Tab: bg=(选中)#2196F3:(未选中)#2A2A2E, 圆角8dp, padding(10,6), font=11sp

--- Tab 0: 常用设置 ---
  spacing: 8dp
  
  标签栏: bg=#4DFFFFFF, padding(10,5), 圆角8dp
    Text("快捷键", 14sp, 白)
  
  Picker("单击动作"):
    Row(bg=#2A2A2E, padding(10,8), 圆角8dp, spacing=4dp) {
      Text("单击动作: ", 14sp, 白)
      Icon(action图标, 18dp, #64B5F6)
      Text(action名称, 14sp, #64B5F6)
      Text("▼", 12sp, Gray)
    }
    → 点击展开 DropdownMenu
  
  Picker("长按动作"): 同上
  
  标签栏: bg=#4DFFFFFF
    Text("UI图图形", 14sp, 白)
  
  Row {
    Text("UI圆角: X", 14sp, 白)
    FlexibleSpace
    Text("0", 11sp, Gray)
    Slider(0-20, width=100dp)
    Text("20", 11sp, Gray)
  }
  
  Toggle("自动保存", padding(8,8))
  Text("进后台时自动保存画布", 11sp, Gray)

--- Tab 1: 网格与背景 ---
  Toggle("显示网格", Switch, padding(8,8))

--- Tab 2: 特殊选项 ---
  Text("性能方案", 14sp, 白)
  每个 PerfMode:
    Column(fillMaxWidth, 圆角8dp, padding(8,6)) {
      bg=(选中)#2196F3:透明
      Text(label, 14sp)
      Text(desc, 11sp, Gray)
    }
```

---

## 触摸操作

```
单指触摸:
  panMode=false → 绘制 (onStrokeDown → onStrokeMove → onStrokeUp)
  panMode=true  → 平移画布

双指触摸:
  → 缩放 (ScaleGestureDetector)
  → 缩放级别吸附: 1,2,4,8,16,32,64

eyedropperActive=true:
  → 单指触摸画布 → 获取像素颜色 → 加入调色板 → 关闭取色

色相条/SV方块/透明条:
  → 长按拖拽 → 实时更新颜色 → 松手确认
  → 拖拽超出边界仍有效 (coerceIn 限制到 0-1)
```

---

## 完整颜色表

| 名称 | 色值 | 用途 |
|------|------|------|
| 主背景 | #1C1C1E | App 背景 |
| 按钮未选中 | #B32A2A2E | CellBg |
| 按钮选中 | #2196F3 | Primary Blue |
| 选中文字 | #FFFFFF | onPrimary |
| 未选中文字 | #FFFFFF | White |
| 禁用文字 | #FFFFFF66 | 40% White |
| 辅助文字 | #B0B0B0 | Gray |
| 弹窗背景 | #E62A2A2E | 95% 不透明 |
| 标签背景 | #4DFFFFFF | 30% 白 |
| 信息背景 | #4D000000 | 30% 黑 |
| 取色器背景 | #1E1E22 | 深蓝灰 |
| 角标文字 | #64B5F6 | 浅蓝 |
| 角标背景 | #DD1A1A1E | 深灰 |
| 确定按钮 | #4CAF50 | 绿 |
| 取消按钮 | #F44336 | 红 |
| 复制按钮 | #2196F3 | 蓝 |
| 分隔线 | #66FFFFFF | 40% 白 |
| 边框选中 | #2196F3 | Blue |
| 边框未选中 | #3A3A3E | 深灰 |
| FPS 文字 | #88CC88 | 浅绿 |
| CPU 文字 | #88AAFF | 浅蓝 |
| GPU 文字 | #FFAA88 | 浅橙 |
| 色块序号 | #80000000 | 50% 黑 |
| 缩略图占位 | #3A3A3E | 深灰 |
| Hex边框 | #66FFFFFF | 40% 白 |
| Hex背景 | #18FFFFFF | 10% 白 |