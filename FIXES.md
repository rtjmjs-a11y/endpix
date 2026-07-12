# Not2Pix — 绘图修复方案

> 本文档由 AI 辅助生成，供其他 AI 读取并快速应用于代码修复。
> 最后更新：2026-07-12

---

## 修复 #1：画笔不显示（flattenRegion 脏区域追踪失败）

### 现象
- 图形工具和油漆桶正常，铅笔/橡皮不显示
- 图层缩略图正确（图层数据 OK），画布显示无效

### 根因
`flattenRegion()` 调用 `displayCanvas.markDirty(sx, sy)` 和 `displayCanvas.markDirty(ex, ey)`，只标记两个角点。但 `uploadDirtyRegion()` 读取 `dirtyMinX/dirtyMaxX/dirtyMinY/dirtyMaxY` 边界框时，可能与 `glTexSubImage2D` 的 row_stride 不对齐，导致纹理上传的像素区域错误。

### 解决方案
将所有笔画和操作的 `flattenRegion` 替换为 `flatten()`。

`flatten()` 的工作方式：
1. 清空整个 `displayCanvas.pixels`
2. 全量合成所有可见图层到 `displayCanvas`
3. 调用 `rebuildTexBuffer()` 重建完整 `texBuffer`
4. 调用 `markDirtyAll()` 标记整个画布脏
5. 渲染器 `uploadDirtyRegion()` 上传全量纹理

### 修改位置

**文件：`app/src/main/java/com/example/endpix/ui/EditorViewModel.kt`**

所有 `layer.canvas.getAndClearDirty()?.let { doc.flattenRegion(...) }` 替换为 `doc.flatten()`。

涉及函数：
- `onStrokeDown()` — PENCIL, ERASER, BUCKET 分支
- `onStrokeMove()` — PENCIL, ERASER, SHAPE 分支
- `onStrokeUp()` — SHAPE 分支

### 关键约束

以下配置缺一不可：

| 配置项 | 文件 | 值 | 说明 |
|--------|------|-----|------|
| renderMode | PixelGLSurfaceView.kt:36 | `RENDERMODE_WHEN_DIRTY` | 按需渲染 |
| hardwareAcceleration | EditorViewModel.kt:58 | `false` | 禁用 HA 模式 |
| hardwareAcceleration | PixelRenderer.kt:49 | `false` | 渲染器也不启用 HA |
| requestRender 位置 | EditorViewModel.kt | `queueEvent { }` 内部 | 必须从 GL 线程调用 |

### 性能说明

`flatten()` 对 32×32 画布仅 1024 像素，256×256 为 65536 像素——每帧完全可接受。  
后续如需大画布（1024+），可重新启用 `flattenRegion` 但需修复脏区域标记逻辑。

---

## 修复 #2：HA 模式图层纹理不可见

### 根因
HA 模式下笔画处理器调用 `getAndClearDirty()` 清除了图层脏标记，但渲染器 `ensureLayerTexture()` 依赖 `hasDirtyRegion` 来上传纹理。脏标记在渲染前被清除，导致图层纹理永不更新。

### 解决方案（未启用）
HA 模式下笔画处理器跳过 `getAndClearDirty()` + `flattenRegion()` 调用，让渲染器直接在 `onDrawFrame` 中读取图层脏标记并上传。

修改位置同上文件，每个画笔操作加 `if (!hardwareAcceleration)` 守卫。

---

## 部署命令

```bash
ADB="C:/Users/Administrator/AppData/Local/Android/Sdk/platform-tools/adb.exe"
TARGET="192.168.1.100:44049"  # 无线调试端口每次变化

# 构建
cd D:/appnew
./gradlew assembleDebug --rerun-tasks
# 注意：可能需 --rerun-tasks 强制重编（Gradle 增量编译可能误判 UP-TO-DATE）

# 安装 + 清除旧存档 + 启动
"$ADB" -s "$TARGET" install -r "app/build/outputs/apk/debug/app-debug.apk"
"$ADB" -s "$TARGET" shell rm -f /data/data/com.example.endpix/files/not2pix_save.dat
"$ADB" -s "$TARGET" shell am force-stop com.example.endpix
"$ADB" -s "$TARGET" shell am start -n com.example.endpix/.MainActivity

# 连接新设备（配对 + 连接）
"$ADB" pair <IP:配对端口> <配对码>
"$ADB" connect <IP:连接端口>
```

## 环境

| 项 | 值 |
|----|-----|
| Package | com.example.endpix |
| compileSdk | 37.1 (需手动安装 android-37.1 platform) |
| AGP | 9.2.1 |
| Kotlin | 2.2.10 |
| JDK | 21 (Android Studio JBR) |
| 测试设备 | vivo V2314A (Android 15), vivo V2502A (Android 16) |
| 渲染 | OpenGL ES 3.0 |
| 默认画布 | 32×32 (DEFAULT_W/DEFAULT_H in EditorViewModel) |