# Not2Pix 变更日志

> AI 可读格式。供后续 AI 读取项目变更历史。
> 更新: 2026-07-12 18:05

---

## 2026-07-12 第二轮变更 (commit a53ace1)

### 文件: `pixel/Tool.kt`

**变更:**
- `Tool.EYEDROPPER` -> `Tool.SELECT` (取色器替换为选择工具)
- 新增 `enum SelectMode { RECT, LASSO }` - 方形选择/套索选择
- 新增 `enum PpMode(val label) { NORMAL("正常"), EXTREME("极端") }` - 像素完美模式

### 文件: `pixel/PixelCanvas.kt`

**变更:**
- `drawPixelPerfectLine` 改用 1-connected Bresenham (midpoint 变体，每步只移一个方向)
- 新增 `strokePixels: HashSet<Long>` 追踪当前笔画像素
- 新增 `beginStroke()` 清空追踪集合
- `ppSet()` 改为：设像素 + 记录到 strokePixels
- `cleanPixelJoints(minX, minY, maxX, maxY, color)` 改为：
  - 只扫描笔画边界框区域 (非全画布)
  - 处理 3/4 L 形 (移除角点)
  - 仅移除 `strokePixels` 中的像素 (不影响已有内容)
  - 限制最多 3 轮扫描
- 删除 `wouldCreate2x2` 和 `safeGet` (不再使用)

### 文件: `ui/EditorViewModel.kt`

**新增状态:**
- `ppMode: PpMode` (默认 EXTREME) - 像素完美子模式
- `eraserSelectorOpen: Boolean` - 橡皮擦子选项弹窗
- `eraserSize: Int` (1..10) - 橡皮擦粗细
- `bucketRemovePixels: Boolean` - 油漆桶移除像素模式
- `selectSelectorOpen: Boolean` - 选择工具子选项弹窗
- `selectMode: SelectMode` (默认 RECT) - 选择模式
- `sMinX/sMinY/sMaxX/sMaxY` - 笔画边界框追踪

**新增方法:**
- `onEraserToolTap()` - 橡皮擦按钮点击 (选中+展开子选项)
- `onBucketToolTap()` - 油漆桶按钮点击
- `onSelectToolTap()` - 选择工具按钮点击 (选中+展开子选项)
- `onSelectLongPress()` - 长按选择工具 (TODO: 全选画布)

**修改:**
- PENCIL onStrokeDown: `beginStroke()` + `ppSet` 记录首像素 + 边界框初始化
- PENCIL onStrokeMove: 追踪边界框 (px,py 和 x,y)
- PENCIL onStrokeUp: `cleanPixelJoints` 仅在 `ppMode == EXTREME` 时调用 + `doc.flatten()`
- ERASER onStrokeDown/Move: 使用 `eraserSize` 绘制方块橡皮
- BUCKET onStrokeDown: `bucketRemovePixels` 时填充 0 (透明)
- EYEDROPPER 处理器替换为 SELECT (空操作, TODO)

### 文件: `ui/EditorScreen.kt`

**新增组件:**
- `EraserSelector` - 橡皮擦大小滑块 (1-10)
- `BucketSelector` - "移除像素"开关
- `SelectSelector` - 方形选择/套索选择

**修改:**
- `BrushSelector` 新增 正常/极端 子选项 (仅 pixelPerfect 启用时显示)
- `BrushSelector` 滑块在 pixelPerfect 启用时锁定为 1 (disabled)
- `ToolColumn` 重构: 每个工具独立 Box + onGloballyPositioned 回调
  - onPencilPositioned / onEraserPositioned / onBucketPositioned / onSelectPositioned
- 工具栏按钮: PENCIL -> ERASER -> BUCKET -> SELECT -> SHAPE
- 橡皮擦角标显示 eraserSize (>1 时)
- 画笔角标在 pixelPerfect 时不显示
- 4 个新选择器弹窗定位 (各自 btnY)

### 关键约束 (更新)

| 项 | 值 | 文件位置 |
|----|-----|---------|
| 画笔默认粗细 | 1 | EditorViewModel.kt:62 |
| 像素完美默认 | false | EditorViewModel.kt:63 |
| 像素完美模式默认 | EXTREME | EditorViewModel.kt:64 |
| 橡皮擦默认粗细 | 1 | EditorViewModel.kt:66 |
| 油漆桶移除像素默认 | false | EditorViewModel.kt:67 |
| 选择模式默认 | RECT | EditorViewModel.kt:68 |
| 性能方案默认 | REGION | EditorViewModel.kt:65 |
| UI 圆角默认 | 8f | EditorViewModel.kt:69 |

### Bug 记录 (更新)

| 问题 | 状态 | 说明 |
|------|------|------|
| cleanPixelJoints 误删已有像素 | 已修复 | strokePixels 追踪，仅删当前笔画像素 |
| cleanPixelJoints 全画布扫描慢 | 已修复 | 限制笔画边界框 + 最多 3 轮 |
| 像素完美影响画笔大小 | 已修复 | 启用时滑块锁定 1px |
| 选择工具实际功能 | TODO | onSelectLongPress 全选 + 选择区域操作未实现 |

---

## 2026-07-12 第一轮变更

### 文件: `pixel/Tool.kt`

**新增枚举:**
- `ExpandDir`: 9 方向（TOP_LEFT/TOP/TOP_RIGHT/LEFT/CENTER/RIGHT/BOTTOM_LEFT/BOTTOM/BOTTOM_RIGHT）。提供 `offsetX()/offsetY()` 计算内容偏移，`iconRes()` 返回 SVG 图标资源 ID。
- `PerfMode`: 性能方案枚举（DEFAULT/REGION/ASYNC/HA），各有 `label` 和 `desc`。

### 文件: `pixel/PixelCanvas.kt`

**新增:**
- `markDirtyRegion(minX, minY, maxX, maxY)`: 显式设置脏区域边界，用于 `flattenRegion`。
- `drawLineSize(x0, y0, x1, y1, color, size, pixelPerfect)`: 带粗细的线条绘制。`size=1` 时退化为 `drawLine`。`size>1` 时每个点绘制 `size×size` 方块。`pixelPerfect=true` 时绘制单像素方块。

### 文件: `pixel/Document.kt`

**修改:**
- `flattenRegion`: `markDirty()` 改为 `markDirtyAll()` — 只优化 CPU 合成范围，GPU 上传全量纹理。

### 文件: `ui/EditorViewModel.kt`

**新增状态:**
- `brushSelectorOpen: Boolean` — 画笔子选项弹窗
- `brushSize: Int` (1..10) — 画笔粗细
- `pixelPerfect: Boolean` — 像素完美开关
- `perfMode: PerfMode` (默认 REGION) — 性能方案
- `uiCornerRadius: Float` (0f..20f, 默认 8f) — UI 全局圆角

**新增方法:**
- `onPencilToolTap()`: 铅笔按钮点击，切换选中+子选项
- `applyFlush(layer, doc)`: 根据 `perfMode` 选择合成方式
  - DEFAULT/ASYNC → `doc.flatten()`
  - REGION → `layer.getAndClearDirty()` + `doc.flattenRegion()`
  - HA → 启用 GPU 图层混合
- `resizeCanvas(newW, newH, dir)`: 接受 `ExpandDir` 参数控制内容偏移方向

**修改:**
- 所有笔画处理器 `doc.flatten()` → `applyFlush(layer.canvas, doc)`
- **SHAPE 工具 `onStrokeMove`/`onStrokeUp` 例外**: 使用 `doc.flatten()` 直接合成（规避 REGION 模式下实时预览问题）
- PENCIL `onStrokeDown`/`onStrokeMove` 使用 `drawLineSize` 支持粗细

### 文件: `ui/EditorScreen.kt`

**主要新增:**
1. **`LocalCornerRadius` CompositionLocal**: 全局圆角值，默认 8f。
2. **`CompositionLocalProvider`**: 包裹 EditorScreen 所有内容，所有 `RoundedCornerShape(X.dp)` 改为 `RoundedCornerShape(LocalCornerRadius.current.dp)`。
3. **`ShortcutPicker`**: 设置中快捷键选择弹窗组件（行显示 + DropdownMenu）。
4. **`BrushSelector`**: 画笔子选项弹窗（大小滑块 1-10 + 像素完美按钮）。
5. **方向选择器**: `ResizeCanvasDialog` 内 3×3 SVG 图标网格下拉。

**UI 修改:**
- **ToolColumn**: 铅笔按钮增加 `onPencilToolTap()` + `brushSize` 角标 + `onGloballyPositioned` 回调 `pencilBtnY`。
- **BottomStrip 快捷键按钮**: 移除 DropdownMenu 配置弹窗。点按=`shortcutTapAction`，长按=`shortcutLongPressAction`。
- **ShapeSelector**: "填充" → "空心填充"，分界线 70dp 宽度 + 40% 不透明度。
- **SettingsDialog**:
  - 分类: "常用设置" / "网格与背景" / "特殊选项"
  - 常用设置: "快捷键" 标签栏(30%白色) + ShortcutPicker 单击/长按 + "UI图图形" 标签栏 + UI圆角滑块 0-20 + 自动保存
  - 特殊选项: PerfMode 性能方案选择

### 图标文件 (新增/修改)

| 文件 | 说明 |
|------|------|
| `ic_expand_t.xml` | 基础向上箭头，被其他 7 个图标通过 `group rotation` 复用 |
| `ic_expand_b.xml` | rotation=180 |
| `ic_expand_l.xml` | rotation=270 |
| `ic_expand_r.xml` | rotation=90 |
| `ic_expand_tl.xml` | rotation=315 |
| `ic_expand_tr.xml` | rotation=45 |
| `ic_expand_bl.xml` | rotation=225 |
| `ic_expand_br.xml` | rotation=135 |
| `ic_expand_c.xml` | 四角星形 |

### 关键约束

| 项 | 值 | 文件位置 |
|----|-----|---------|
| 画笔默认粗细 | 1 | EditorViewModel.kt |
| 像素完美默认 | false | EditorViewModel.kt |
| 性能方案默认 | REGION | EditorViewModel.kt |
| UI 圆角默认 | 8f | EditorViewModel.kt |
| 扩展方向默认 | BOTTOM_RIGHT | EditorViewModel.kt |
| 快捷键-单击默认 | FIT | EditorViewModel.kt |
| 快捷键-长按默认 | NONE | EditorViewModel.kt |

### Bug 记录

| 问题 | 状态 | 说明 |
|------|------|------|
| HA 模式图层纹理不可见 | 未修复 | `ensureLayerTexture` 与 `getAndClearDirty` 脏标记冲突 |
| `flattenRegion` 非全量时不可见 | 已规避 | 改用 `markDirtyAll` 全量上传 |
| 图形工具 REGION 模式预览消失 | 已规避 | SHAPE 笔画直接用 `flatten()` |
| sed 批量替换 `RoundedCornerShape` 部分失效 | 已修复 | 改用 `sed -i` 命令强制替换 |

### 部署

```bash
ADB="C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
TARGET="192.168.1.100:44049"  # 无线调试端口每次变化
cd D:/appnew && ./gradlew assembleDebug --rerun-tasks
"$ADB" -s "$TARGET" install -r "app/build/outputs/apk/debug/app-debug.apk"
"$ADB" -s "$TARGET" shell am force-stop com.example.endpix
"$ADB" -s "$TARGET" shell am start -n com.example.endpix/.MainActivity
```