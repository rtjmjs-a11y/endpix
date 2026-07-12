# Not2Pix (endpix) - AI 交接文档 (第四轮)

> 生成时间: 2026-07-12 23:50 CST
> 用途: 交接给其他 AI 继续处理
> 仓库: https://github.com/rtjmjs-a11y/endpix
> 设备: vivo V2314A, 无线调试 192.168.1.100:35851 (端口每次重启变化)
> ADB: C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe

---

## Git 状态

- 最后推送: `0a9bb2c` (main)
- 本地提交: `9b761d0` (state commit, 未推送但已包含在 0a9bb2c 中)
- 工作树: 干净

---

## 本轮 (第四轮) 完成

### 1. HSV 取色器 (ColorPickerDialog)

**文件: EditorScreen.kt (行 838-933)**

| 组件 | 说明 |
|------|------|
| `ColorPickerDialog` | 完整取色器弹窗，AlertDialog 封装 |
| `HueBar` | 垂直彩虹渐变条，白色横条指示器，支持拖拽 |
| `SvSquare` | 240dp 方块，横向饱和度+纵向明度，空心圆指示器 |
| `AlphaBar` | 水平透明条，棋盘格底+渐变色，白色竖线指示器 |
| 十六进制 | `BasicTextField` #RRGGBBAA，`titleMedium` 字体，长按复制到剪贴板 |
| 按钮 | 确定(绿 #4CAF50) / 取消(红 #F44336)，14dp 内边距，大间距 |

### 2. 调色板子选项 (PaletteBar)

**行 347-378**

- 点击色块 → 弹出 DropdownMenu：调色盘(ic_palette) / 取色器(ic_eyedropper)
- 取色器激活时底部显示取色图标
- 长按色块 → 直接选中颜色

### 3. 像素完美 (Pixel Perfect) 最终方案

**PixelCanvas.kt:**
- `drawPixelPerfectLine`: 1-connected Bresenham (midpoint 变体)
- `drawLineSize`: 仅在 `size<=1 && pixelPerfect` 时调用完美线条
- `cleanPixelJoints`: 扫描笔画边界框，清除 3/4 L 形和 4/4 方块，仅删 `strokePixels` 中像素
- `ppSet` / `beginStroke` / `strokePixels`: 追踪当前笔画像素

**EditorViewModel.kt:**
- `PpMode.NORMAL`: 无清理
- `PpMode.EXTREME`: 抬笔后调用 `cleanPixelJoints`
- 像素完美不影响画笔大小（滑块始终可调）
- 仅在 `brushSize=1` 时生效

### 4. 其他功能

| 功能 | 状态 |
|------|------|
| 选择工具 (SELECT) | 方形/套索选择，子选项 UI，确认/取消/复制 |
| 橡皮擦大小 | 1-10 滑块，角标始终显示 |
| 油漆桶移除像素 | 开关，启用时红点角标 |
| 性能方案 | DEFAULT/REGION/ASYNC/HA，设置中切换 |
| UI 圆角持久化 | SaveData 包含 uiCornerRadius，自动保存/恢复 |
| 应用名 | endpix |
| 键盘不推弹窗 | `adjustNothing` |

---

## 待完成

| 问题 | 优先级 | 说明 |
|------|--------|------|
| 取色板背景 30dp 独立缩放 | 高 | `AlertDialog` 的 `text` 块内用 `Box` 分层会破坏括号结构。需自定义 `Dialog` 组件实现 |
| 选择工具实际功能 | 高 | `copySelection` 只做 flatten，未实现实际像素复制 |
| 套索选择精确选区 | 中 | 当前用边界框，非精确多边形 |
| 逐层 undo/redo | 低 | 当前全局历史栈，切换图层/帧清空 |
| 取色器笔画拦截 | 中 | onStrokeDown/onStrokeMove 已有 eyedropperActive 拦截 |

---

## 关键文件位置

| 文件 | 行号 | 内容 |
|------|------|------|
| `EditorScreen.kt` | 838 | ColorPickerDialog |
| `EditorScreen.kt` | 962 | HueBar |
| `EditorScreen.kt` | 975 | SvSquare |
| `EditorScreen.kt` | 988 | AlphaBar |
| `EditorScreen.kt` | 347 | PaletteBar (popup) |
| `EditorScreen.kt` | 156 | ColorPickerDialog 调用 |
| `PixelCanvas.kt` | 177 | drawPixelPerfectLine |
| `PixelCanvas.kt` | 201 | cleanPixelJoints |
| `PixelCanvas.kt` | 264 | drawLineSize |
| `EditorViewModel.kt` | 163 | applyFlush |
| `EditorViewModel.kt` | 235 | onStrokeDown (eyedropper intercept) |
| `EditorViewModel.kt` | 617 | onSelectLongPress / confirmSelection / cancelSelection |

---

## 构建部署

```bash
cd D:/appnew
./gradlew assembleDebug --rerun-tasks
ADB="C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
TARGET="192.168.1.100:35851"
"$ADB" kill-server && "$ADB" start-server
"$ADB" connect $TARGET
"$ADB" -s "$TARGET" shell am force-stop com.example.endpix
"$ADB" -s "$TARGET" install -r "app/build/outputs/apk/debug/app-debug.apk"
"$ADB" -s "$TARGET" shell am start -n com.example.endpix/.MainActivity
```

## 无线调试

```bash
adb pair 192.168.1.100:PAIRING_PORT PAIRING_CODE
adb connect 192.168.1.100:CONNECTION_PORT
# 如端口超时，kill-server 后重试
```

---

## 设计准则 (AI 必须遵守)

- 紧凑间距: `Arrangement.spacedBy(4.dp)` 或更小，内边距 ≤8dp
- 半透明叠加: 弹窗 `Color(0xE62A2A2E)`，标签 `Color(0x4DFFFFFF)`
- 选中高亮: `MaterialTheme.colorScheme.primary`
- 所有 `RoundedCornerShape` 必须用 `LocalCornerRadius.current.dp`
- 文件修改先做小步验证，`AlertDialog` 的 `text` 块内避免嵌套 `Box` 分层
- 部署前 `adb kill-server && start-server` 避免无线连接卡死