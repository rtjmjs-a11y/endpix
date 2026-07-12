# Not2Pix (endpix) - AI 交接文档 (第三轮)

> 生成时间: 2026-07-12 19:25 CST
> 用途: 交接给其他 AI 继续处理
> 仓库: https://github.com/rtjmjs-a11y/endpix
> 设备: vivo V2314A, 无线调试端口 192.168.1.100:35851 (每次重启变化)
> ADB: C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe

---

## Git 状态

- 最后推送: `286b9b1` (docs: update CHANGES.md)
- 未提交变更 (working tree):
  - `EditorScreen.kt` - SelectSelector UI + 新图标引用 + ToolColumn 重构
  - `EditorViewModel.kt` - 选择工具逻辑 + 状态变量
  - `ic_select.xml`, `ic_select_rect.xml`, `ic_select_lasso.xml` - 新 SVG 图标

---

## 本轮 (第三轮) 完成的工作

### 1. 选择工具 (SELECT) 完整实现

**替换了 EYEDROPPER (取色器)**，取色功能已移除。

**Tool.kt 新增:**
- `Tool.SELECT` 替换 `Tool.EYEDROPPER`
- `enum SelectMode { RECT, LASSO }`
- `enum PpMode(val label) { NORMAL("正常"), EXTREME("极端") }`

**EditorViewModel.kt 新增状态 (行 72-78):**
```kotlin
var hasSelection by mutableStateOf(false)
var selX0/selY0/selX1/selY1 by mutableIntStateOf(0)  // 选区边界
private var selSnapshot: IntArray? = null  // 选区快照
private val selLassoPath = ArrayList<IntArray>()  // 套索路径
```

**选择操作方法 (行 617-643):**
| 方法 | 行号 | 功能 |
|------|------|------|
| `onSelectLongPress()` | 617 | 全选画布 |
| `confirmSelection(layer, doc)` | 624 | 确认选择，清空选区 |
| `cancelSelection(layer, doc)` | 631 | 取消选择，恢复快照 |
| `copySelection(layer, doc)` | 640 | 复制选择 (TODO: 实际复制逻辑) |

**笔画处理:**
- onStrokeDown (行 280): 如已有选区 -> confirmSelection；否则清空 lassoPath
- onStrokeMove (行 367): RECT 模式更新 selX0/Y0/X1/Y1；LASSO 模式追加路径点
- onStrokeUp (行 420): 松手后设定选区边界，如有效则拍照快照 + `hasSelection = true`

### 2. SVG 图标 (3个新文件)

| 文件 | 说明 |
|------|------|
| `ic_select_rect.xml` | 虚线框选择图标 (4角L形) |
| `ic_select_lasso.xml` | 套索选择图标 (自由曲线) |
| `ic_select.xml` | 通用选择图标 (备用) |

工具栏图标根据 `selectMode` 动态切换：RECT -> `ic_select_rect`，LASSO -> `ic_select_lasso`

### 3. EditorScreen.kt UI

**SelectSelector 组件:**
- 方形选择/套索选择 模式切换
- 有选区时显示分界线 + 3个操作按钮：
  - 确认 (绿色 `#4CAF50`)
  - 复制 (蓝色 `#2196F3`)
  - 取消 (红色 `#F44336`)

**ToolColumn 重构:**
- 每个工具独立 Box + `onGloballyPositioned` 回调
- 5 个定位回调: `onPencilPositioned`, `onEraserPositioned`, `onBucketPositioned`, `onSelectPositioned`, `onShapePositioned`
- 工具顺序: PENCIL -> ERASER -> BUCKET -> SELECT -> SHAPE

---

## 像素完美 (Pixel Perfect) - 当前完整状态

### 算法 (PixelCanvas.kt)

| 函数 | 行号 | 说明 |
|------|------|------|
| `drawPixelPerfectLine` | 177 | 1-connected Bresenham (midpoint 变体，每步只移一个方向) |
| `beginStroke()` | 203 | 清空 `strokePixels` 集合 |
| `ppSet(x, y, color)` | 207 | 设像素 + 记录到 `strokePixels` |
| `cleanPixelJoints(minX..maxY, color)` | 215 | 扫描边界框内 3/4 L 形和 4/4 方块，仅删除 `strokePixels` 中的角点，最多 3 轮 |

### ViewModel 集成 (EditorViewModel.kt)

| 位置 | 行号 | 操作 |
|------|------|------|
| onStrokeDown PENCIL | 239-247 | `beginStroke()` + `ppSet` 首像素 + 边界框初始化 |
| onStrokeMove PENCIL | 310-313 | 追踪边界框 (px,py 和 x,y) |
| onStrokeUp | 439-441 | `ppMode == EXTREME` 时调 `cleanPixelJoints` + `doc.flatten()` |

### 子选项 (BrushSelector)
- 像素完美启用时滑块锁定 1px (disabled)
- 启用时显示 正常/极端 子选项
- 正常 = 无清理 (纯 1-connected Bresenham)
- 极端 = 清理 L 形角点 (strokePixels 追踪，不影响已有像素)

### 关键约束
- `strokePixels` 只追踪当前笔画，`cleanPixelJoints` 只删当前笔画的角点
- 边界框限制扫描范围 (非全画布)
- `PpMode.NORMAL` 跳过清理

---

## 所有工具子选项汇总

| 工具 | 子选项 | 位置 |
|------|--------|------|
| 铅笔 (PENCIL) | 大小滑块 1-10 + 像素完美(1px) + 正常/极端 | BrushSelector |
| 橡皮擦 (ERASER) | 大小滑块 1-10 | EraserSelector |
| 油漆桶 (BUCKET) | 移除像素开关 | BucketSelector |
| 选择 (SELECT) | 方形/套索 + 确认/复制/取消 | SelectSelector |
| 图形 (SHAPE) | 直线/矩形/圆/叶/套索 + 空心填充 | ShapeSelector |

---

## 性能方案 (PerfMode)

| 模式 | 方法 | 默认 |
|------|------|------|
| DEFAULT | `doc.flatten()` 全量合成 | |
| **REGION** | `flattenRegion` + `markDirtyAll` 区域合成 | ✓ |
| ASYNC | 同 DEFAULT (未实现) | |
| HA | GPU 图层混合 `ensureLayerTexture` | |

`applyFlush()` 在 EditorViewModel.kt:163 控制切换。
切换时自动同步 `renderer.hardwareAcceleration` + `invalidateTexture()`。

---

## 已知问题 / TODO

| 问题 | 状态 | 说明 |
|------|------|------|
| 选择工具复制功能 | TODO | `copySelection` 只做了 flatten，未实际复制像素 |
| 套索选择实际选区 | 部分 | 记录路径点但选区用边界框，非精确多边形 |
| 选择区域可视化 | TODO | 无虚线框/高亮显示选区范围 |
| 选择区域移动 | TODO | 未实现拖拽移动选区内容 |
| 像素完美拐角 | 部分解决 | EXTREME 模式清理 L 形，NORMAL 模式不清理 |
| HA 模式 | 可用 | `markDirtyAll` 每帧强制上传，性能未优化 |
| ASYNC 模式 | 未实现 | 同 DEFAULT |

---

## 构建和部署

```bash
# 构建
cd D:/appnew
./gradlew assembleDebug --rerun-tasks
# 注意: Gradle 增量编译可能误判 UP-TO-DATE，需要 --rerun-tasks

# 安装 + 启动 (端口每次重启变化)
ADB="C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
TARGET="192.168.1.100:35851"
"$ADB" -s "$TARGET" install -r "app/build/outputs/apk/debug/app-debug.apk"
"$ADB" -s "$TARGET" shell am force-stop com.example.endpix
"$ADB" -s "$TARGET" shell am start -n com.example.endpix/.MainActivity

# 读取调试日志
"$ADB" -s "$TARGET" shell "run-as com.example.endpix cat files/endpix_dbg.txt"

# 清除调试日志
"$ADB" -s "$TARGET" shell "run-as com.example.endpix rm -f files/endpix_dbg.txt"
```

---

## 无线调试连接

```bash
# 配对 (端口和配对码每次变化)
adb pair 192.168.1.100:PAIRING_PORT PAIRING_CODE

# 直接连接 (已配对过)
adb connect 192.168.1.100:CONNECTION_PORT

# mDNS 自动发现 (已配对设备)
# 设备名: adb-10AD8R0Q7C000EM-xP9wCd._adb-tls-connect._tcp
```

---

## DebugLog 工具

位于 `PixelGLSurfaceView.kt`，写入 `/data/data/com.example.endpix/files/endpix_dbg.txt`

```kotlin
DebugLog.log("message")  // 自动每2秒刷新到文件
DebugLog.dir = context.filesDir  // 在 PixelGLSurfaceView init 中设置
```

---

## 关键文件位置

| 文件 | 说明 |
|------|------|
| `pixel/Tool.kt` | Tool/ShapeMode/SelectMode/PpMode/ShortcutAction/ExpandDir/PerfMode 枚举 |
| `pixel/PixelCanvas.kt` | 像素缓冲 + drawLine/drawPixelPerfectLine/cleanPixelJoints/floodFill + ppSet/beginStroke |
| `pixel/Document.kt` | Document/Frame/Layer + flatten/flattenRegion/compositeLayers |
| `gl/PixelRenderer.kt` | OpenGL ES 3.0 渲染 + HA 模式 + ensureLayerTexture |
| `gl/PixelGLSurfaceView.kt` | GLSurfaceView + 触控 + DebugLog + StrokeListener |
| `ui/EditorViewModel.kt` | 所有状态 + 笔画处理 + applyFlush + 选择工具 |
| `ui/EditorScreen.kt` | Compose UI + 所有选择器组件 + LocalCornerRadius |
| `res/drawable/ic_*.xml` | SVG 矢量图标 (含 3 个新选择图标) |
| `FIXES.md` | 修复记录 |
| `CHANGES.md` | 变更日志 (两轮) |
| `AI_HANDOFF.md` | 本文档 |

---

## 参考链接

- Krita GSoC Pixel Perfect: https://krita.org/zh-cn/posts/2024/gsoc-2024/
- Aseprite Pixel Perfect: https://blog.csdn.net/gitblog_00117/article/details/150626445
- wandsmire/not2pix (libGDX): https://github.com/wandsmire/not2pix
- gritsenko/Pix2d (C#): https://github.com/gritsenko/Pix2d