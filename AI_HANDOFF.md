# Not2Pix (endpix) — 项目状态摘要 + AI 交接文档

> 生成时间: 2026-07-12 17:06 CST
> 用途: 交接给其他 AI 继续处理

---

## 项目概览

| 项 | 值 |
|----|-----|
| 项目名 | Not2Pix / endpix (package: com.example.endpix) |
| 类型 | Android 像素艺术编辑器 |
| 语言 | Kotlin |
| UI | Jetpack Compose (Material3) |
| 渲染 | OpenGL ES 3.0 (GLSurfaceView) |
| 构建 | Gradle 9.4.1, AGP 9.2.1, Kotlin 2.2.10, JDK 21 |
| compileSdk | 37.1 (需手动安装 android-37.1 platform) |
| 仓库 | https://github.com/rtjmjs-a11y/endpix |
| 设备 | vivo V2314A (Android 15) |
| 连接 | 无线调试: 192.168.1.100:41597 (端口每次变化) |
| ADB | C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe |

---

## Git 状态

- 最后提交: `27bd0ce` — "feat: expand direction, brush selector, perf modes, pixel perfect..."
- 未提交文件 (working tree dirty):
  - `PixelCanvas.kt` — drawPixelPerfectLine 改为 Godot `err=dx-dy` 变体 + cleanPixelJoints
  - `PixelGLSurfaceView.kt` — DebugLog 工具 + StrokeListener 修复
  - `EditorViewModel.kt` — cleanPixelJoints 调用 + DebugLog 清除

---

## 核心架构

```
用户触摸 → PixelGLSurfaceView.onTouchEvent
         → EditorViewModel.onStrokeDown/Move/Up (UI线程)
         → glView.queueEvent { 画笔操作 } (GL线程)
         → PixelCanvas 像素写入
         → applyFlush (perfMode 决定合成方式)
         → requestRender
         → PixelRenderer.onDrawFrame
         → OpenGL ES 3.0 shader 渲染
```

### 图层合成方案

| 模式 | 方法 | 说明 |
|------|------|------|
| DEFAULT | `doc.flatten()` | 全量合成到 displayCanvas |
| REGION (默认) | `flattenRegion` + `markDirtyAll` | 区域合成 + 全量纹理上传 |
| ASYNC | 同 DEFAULT | 未实现异步 |
| HA | GPU 图层混合 | 每层独立 GL 纹理, BLEND |

`applyFlush()` 在 EditorViewModel.kt:146 控制方案切换。

---

## 像素完美 (Pixel Perfect) — 当前状态

### 已实现

1. **绘制算法**: Godot `err = dx - dy` Bresenham 变体 (PixelCanvas.kt:177)
   ```kotlin
   err = dx - dy
   while:
       draw(x,y)
       e2 = 2*err
       if e2 > -dy → err -= dy; x += sx
       if e2 < dx  → err += dx; y += sy
   ```

2. **抬笔后处理**: `cleanPixelJoints(color)` (PixelCanvas.kt:201)
   - 全画布扫描 2×2 方块 (4像素全着色)
   - 移除右上角像素
   - 循环至无方块
   - 8-连通性不破坏

3. **调用位置**: EditorViewModel.kt:361
   ```kotlin
   if (t == Tool.PENCIL && pixelPerfect) layer.canvas.cleanPixelJoints(c)
   ```

### 待解决问题

- **拐角仍有直角像素**: 段间连接处的 3/4 L 形未被完全清除
- 用户要求: 保持线条连贯的同时消除 2×2 堆积
- 参考: Krita GSoC 2024 "Pixel Perfect Lines" (需要手动擦除残留直角像素)
- 参考: Aseprite "Pixel Perfect" 模式 (绘前检测)
- 参考: Godot Bresenham 变体 (err=dx-dy, 当前已采用)

### 关键算法文件

| 文件 | 行 | 内容 |
|------|-----|------|
| PixelCanvas.kt | 177-192 | drawPixelPerfectLine (Godot 变体) |
| PixelCanvas.kt | 201-216 | cleanPixelJoints |
| PixelCanvas.kt | 218-227 | ppSet + wouldCreate2x2 (未使用, 保留) |
| EditorViewModel.kt | 228-244 | onStrokeDown PENCIL |
| EditorViewModel.kt | 285-287 | onStrokeMove PENCIL |
| EditorViewModel.kt | 361 | onStrokeUp PENCIL 清洁调用 |

---

## DebugLog 工具

位于 `PixelGLSurfaceView.kt`, 写入 `/data/data/com.example.endpix/files/endpix_dbg.txt`

```kotlin
DebugLog.log("PP ($px,$py)→($x,$y) size=$sz")
```

读取命令:
```bash
adb shell "run-as com.example.endpix cat files/endpix_dbg.txt"
```

---

## 构建和部署命令

```bash
# 构建
cd D:/appnew
./gradlew assembleDebug --rerun-tasks
# 注意: Gradle 增量编译可能误判 UP-TO-DATE, 需要 --rerun-tasks

# 安装
adb -s 192.168.1.100:PORT install -r app/build/outputs/apk/debug/app-debug.apk

# 启动
adb -s 192.168.1.100:PORT shell am force-stop com.example.endpix
adb -s 192.168.1.100:PORT shell am start -n com.example.endpix/.MainActivity

# 读取调试日志
adb -s 192.168.1.100:PORT shell "run-as com.example.endpix cat files/endpix_dbg.txt"

# 清除调试日志
adb -s 192.168.1.100:PORT shell "run-as com.example.endpix rm -f files/endpix_dbg.txt"

# 清除旧存档 (防 OOM)
adb -s 192.168.1.100:PORT shell rm -f /data/data/com.example.endpix/files/not2pix_save.dat
```

---

## 无线调试连接

```bash
# 方式1: 配对后连接
adb pair 192.168.1.100:PAIRING_PORT PAIRING_CODE
adb connect 192.168.1.100:CONNECTION_PORT

# 方式2: 直接连接 (已配对过)
adb connect 192.168.1.100:PORT
```

---

## 关键修复记录 (FIXES.md)

详见 `D:/appnew/FIXES.md`:
- 修复 #1: 画笔不显示 → `flattenRegion` 改 `flatten()` + `requestRender` 线程修复
- 修复 #2: HA 模式图层纹理不可见

## 变更记录 (CHANGES.md)

详见 `D:/appnew/CHANGES.md`:
- 扩展方向、BrushSelector、PerfMode、UI 圆角、快捷鍵等

---

## 其他参考

- Krita GSoC: https://krita.org/zh-cn/posts/2024/gsoc-2024/
- CSDN Aseprite: https://blog.csdn.net/gitblog_00117/article/details/150626445
- wandsmire/not2pix: https://github.com/wandsmire/not2pix (libGDX/Java 像素编辑器, 功能相似)
- gritsenko/Pix2d: https://github.com/gritsenko/Pix2d (C# 像素编辑器)