# endpix — 最终交接文档

> 最后更新: 2026-07-13 22:49 CST
> 仓库: https://github.com/rtjmjs-a11y/endpix
> 引擎: Android Studio (Kotlin + Jetpack Compose + OpenGL ES 3.0)
> 设备: vivo V2314A (Android 15), vivo V2502A (Android 16)
> 下一步: 转 Unity 开发

---

## 项目结构 (Android Studio)

```
D:\appnew/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/endpix/
│   │   │   ├── pixel/          # 核心数据模型
│   │   │   │   ├── Tool.kt         # 工具/形状/选择/像素完美/性能模式枚举
│   │   │   │   ├── PixelCanvas.kt  # 像素缓冲 + 绘图算法 + 脏区域追踪
│   │   │   │   ├── Document.kt     # 文档/帧/图层 + flatten/flattenRegion
│   │   │   │   ├── Frame.kt        # 帧管理
│   │   │   │   ├── Layer.kt        # 图层管理
│   │   │   │   ├── History.kt      # 撤销/重做栈
│   │   │   │   └── SaveData.kt     # 序列化存储
│   │   │   ├── gl/             # OpenGL 渲染
│   │   │   │   ├── PixelRenderer.kt    # ES 3.0 渲染器 + HA 模式
│   │   │   │   ├── PixelGLSurfaceView.kt # GL 触控 + DebugLog
│   │   │   │   ├── Shaders.kt          # 顶点/片段着色器
│   │   │   │   └── ShaderUtil.kt       # 着色器工具
│   │   │   └── ui/             # Compose UI
│   │   │       ├── EditorViewModel.kt  # 所有状态 + 笔画处理 + applyFlush
│   │   │       └── EditorScreen.kt     # 完整 UI + HSV 取色器 + 所有子选项
│   │   ├── res/
│   │   │   ├── drawable/       # SVG 矢量图标 (30+ 个)
│   │   │   └── values/         # 主题/颜色/字符串
│   │   └── AndroidManifest.xml # adjustNothing
│   └── build.gradle.kts
├── gradle/                     # Gradle 配置
├── CHANGES.md                  # 变更日志 (AI 可读)
├── AI_HANDOFF.md               # 交接文档
├── FIXES.md                    # 修复记录
└── build.gradle.kts
```

---

## 核心架构

### 渲染管线
```
用户触摸 → PixelGLSurfaceView (GL线程)
         → EditorViewModel.onStrokeDown/Move/Up (UI线程)
         → glView.queueEvent { 绘图操作 } (GL线程)
         → PixelCanvas 像素写入
         → applyFlush (perfMode 决定合成方式)
         → requestRender
         → PixelRenderer.onDrawFrame
         → OpenGL ES 3.0 shader 渲染
```

### 图层合成方案 (applyFlush)
| 模式 | 方法 | 说明 |
|------|------|------|
| DEFAULT | `doc.flatten()` | 全量合成 |
| REGION | `flattenRegion` + `markDirtyAll` | 区域合成 + 全量上传 |
| HA | GPU 图层混合 | 每层独立 GL 纹理 |
| ASYNC | 同 DEFAULT | 未实现 |

### 工具系统
| 工具 | 子选项 | 触发 |
|------|--------|------|
| 铅笔 | 大小 1-10 + 像素完美(正常/极端) | 点击展开 |
| 橡皮擦 | 大小 1-10 | 点击展开 |
| 油漆桶 | 移除像素开关 | 点击展开 |
| 选择 | 方形/套索 + 确认/复制/取消 | 点击展开 |
| 图形 | 线/矩形/圆/叶/套索 + 空心填充 | 点击展开 |

### HSV 取色器
- `HueBar`: 垂直彩虹渐变 + 白色横条指示器
- `SvSquare`: 360dp 饱和度/明度方块 + 空心圆指示器 + 色环(长按)
- `AlphaBar`: 水平透明条 + 棋盘格底
- 色环: 中空双半圆(上=当前色/下=原色) + 十字 + 描边
- 十六进制: `#RRGGBBAA` 可编辑 + 长按复制

---

## 像素完美 (Pixel Perfect)

| 组件 | 说明 |
|------|------|
| `drawPixelPerfectLine` | 1-connected Bresenham (midpoint 变体) |
| `drawLineSize` | `size<=1 && pixelPerfect` → 完美线条; `>1` → 方块印章 |
| `cleanPixelJoints` | 扫描笔画边界框, 清除 3/4 L 形 + 4/4 方块 |
| `strokePixels` | 追踪当前笔画像素, 只删当前笔画的角点 |
| `PpMode.NORMAL` | 无清理 |
| `PpMode.EXTREME` | 抬笔后调用 `cleanPixelJoints` |

---

## UI 圆角系统

- `LocalCornerRadius` CompositionLocal 全局提供
- 所有 `RoundedCornerShape` 使用 `LocalCornerRadius.current.dp`
- 设置滑块 0-20dp, 持久化到 `SaveData`

---

## 关键修复

| 问题 | 修复 |
|------|------|
| 画笔不显示 | `flattenRegion` → `flatten()` + `requestRender` 线程修复 |
| HA 模式图层不可见 | `markDirtyAll` 强制上传 |
| 像素完美断线 | 1-connected Bresenham + `strokePixels` 追踪 |
| 2×2 方块残留 | `cleanPixelJoints` 多轮扫描 |
| 键盘推弹窗 | `adjustNothing` |
| 圆角不生效 | `LocalCornerRadius` + sed 全量替换 |

---

## Unity 转换注意事项

### 引擎结构对应

| Android Studio | Unity |
|----------------|-------|
| `PixelCanvas` (IntArray) | `Texture2D` + `Color[]` |
| `Document/Frame/Layer` | `ScriptableObject` + `RenderTexture` |
| `PixelRenderer` (OpenGL) | Unity 渲染管线 |
| `EditorViewModel` (状态) | `MonoBehaviour` + `ScriptableObject` |
| `EditorScreen` (Compose UI) | Unity UI Toolkit / UGUI |
| `PixelGLSurfaceView` (触控) | `Input.GetTouch` / `InputSystem` |
| `History` (撤销/重做) | 命令模式 + `Stack<ICommand>` |
| `SaveData` (序列化) | `JsonUtility` / `PlayerPrefs` |

### 渲染方案

Android 版用 OpenGL ES 3.0 + 自定义 shader。Unity 可用:
- `Graphics.Blit` 合成图层到 `RenderTexture`
- `Material.SetTexture` 上传像素数据
- `CommandBuffer` 实现图层混合

### 工具系统

Android 版工具是 `when(tool)` 分支。Unity 建议:
- 策略模式: `ITool` 接口, `PencilTool/EraserTool/...` 实现
- 每个工具独立 `MonoBehaviour` 或 `ScriptableObject`

### 像素完美

Bresenham 算法语言无关, 直接移植 Kotlin → C#:
```csharp
// drawPixelPerfectLine 直接翻译
void DrawPixelPerfectLine(Texture2D tex, int x0, int y0, int x1, int y1, Color color) {
    int dx = Mathf.Abs(x1 - x0);
    int dy = Mathf.Abs(y1 - y0);
    // ... Bresenham 逻辑
}
```

### HSV 取色器

Android 版用 Compose `Canvas` + `DrawScope`。Unity 可用:
- `GUI.DrawTexture` 绘制渐变
- `Event.current.mousePosition` 处理触控
- 或 Unity UI Toolkit 的 `VisualElement` + 自定义 `Mesh`

---

## 构建部署 (Android Studio)

```bash
cd D:/appnew
./gradlew assembleDebug --rerun-tasks
ADB="C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
TARGET="192.168.1.100:PORT"
"$ADB" kill-server && "$ADB" start-server
"$ADB" connect $TARGET
"$ADB" -s "$TARGET" install -r "app/build/outputs/apk/debug/app-debug.apk"
"$ADB" -s "$TARGET" shell am start -n com.example.endpix/.MainActivity
```

---

## 无线调试

```bash
adb pair 192.168.1.100:PAIRING_PORT PAIRING_CODE
adb connect 192.168.1.100:CONNECTION_PORT
# 端口每次重启变化, 如超时 kill-server 重试
```

---

## AI 可读文件

| 文件 | 内容 |
|------|------|
| `AI_HANDOFF.md` | 本文档 |
| `CHANGES.md` | 三轮变更记录 + 设计准则 |
| `FIXES.md` | Bug 修复记录 |
| `endpix.apk` | 最终安装包 |

---

## 设计准则

- 紧凑间距: `Arrangement.spacedBy(4.dp)`, 内边距 ≤8dp
- 半透明叠加: 弹窗 `Color(0xE62A2A2E)`, 标签 `Color(0x4DFFFFFF)`
- 选中高亮: `MaterialTheme.colorScheme.primary`
- 所有 `RoundedCornerShape` 必须用 `LocalCornerRadius.current.dp`
- 文件修改先做小步验证
- 部署前 `adb kill-server && start-server` 避免卡死