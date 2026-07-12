package com.example.endpix.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.endpix.R
import com.example.endpix.gl.PixelGLSurfaceView
import com.example.endpix.pixel.ShapeMode
import com.example.endpix.pixel.ShortcutAction
import com.example.endpix.pixel.Tool

private val CellBg = Color(0xB32A2A2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: EditorViewModel) {
    var showNewDialog by remember { mutableStateOf(false) }
    var showResizeDialog by remember { mutableStateOf(false) }
    @Suppress("UNUSED_EXPRESSION") vm.structureVersion

    var shapeBtnY by remember { mutableStateOf(0f) }
    var boxY by remember { mutableStateOf(0f) }
    var topStripsH by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> vm.glView?.onResume()
                Lifecycle.Event.ON_PAUSE -> { vm.saveState(); vm.glView?.onPause() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { boxY = it.positionInRoot().y }
    ) {
        AndroidView(
            factory = { ctx ->
                PixelGLSurfaceView(ctx, vm.renderer).also {
                    vm.glView = it
                    it.onResume()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .statusBarsPadding()
                .onGloballyPositioned { topStripsH = it.positionInRoot().y + it.size.height - boxY }
        ) {
            StatusBar(vm)
            DocStrip(vm)
            FrameStrip(vm)
        }

        ToolColumn(vm, Modifier.align(Alignment.CenterStart)) { shapeBtnY = it }

        PaletteBar(vm, Modifier.align(Alignment.CenterEnd))

        val topOffset = with(density) { topStripsH.toDp() }
        LayerPanel(vm, Modifier.align(Alignment.TopEnd).offset(y = topOffset))

        BottomStrip(vm, Modifier.align(Alignment.BottomCenter).navigationBarsPadding(), onNew = { showNewDialog = true }, onResize = { showResizeDialog = true })

        if (vm.tool == Tool.SHAPE && vm.shapeSelectorOpen) {
            val offsetDp = with(density) { (shapeBtnY - boxY).coerceAtLeast(0f).toDp() }
            ShapeSelector(vm, Modifier.align(Alignment.TopStart).offset(x = 52.dp, y = offsetDp))
        }
    }

    if (showNewDialog) {
        NewCanvasDialog(
            onDismiss = { showNewDialog = false },
            onConfirm = { w, h ->
                vm.addDocument(w, h)
                showNewDialog = false
            }
        )
    }

    if (showResizeDialog) {
        ResizeCanvasDialog(vm,
            onDismiss = { showResizeDialog = false },
            onConfirm = { w, h -> vm.resizeCanvas(w, h); showResizeDialog = false }
        )
    }

    if (vm.showSettings) {
        SettingsDialog(vm)
    }

    vm.exportMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.consumeExportMessage() },
            confirmButton = {
                TextButton(onClick = { vm.consumeExportMessage() }) { Text("OK") }
            },
            title = { Text("导出") },
            text = { Text(msg) }
        )
    }
}

@Composable
private fun StatusBar(vm: EditorViewModel) {
    val doc = vm.activeDoc
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${doc.width}×${doc.height}", style = MaterialTheme.typography.labelSmall, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("${doc.frames.size}帧", style = MaterialTheme.typography.labelSmall, color = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("${vm.zoomPercent}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("FPS:${vm.fpsDisplay}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF88CC88))
            Spacer(Modifier.width(8.dp))
            Text("CPU:${vm.cpuPercent}%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF88AAFF))
            Spacer(Modifier.width(8.dp))
            Text("GPU:${if (vm.gpuPercent >= 0) "${vm.gpuPercent}%" else "no get"}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFAA88))
        }
    }
}

@Composable
private fun DocStrip(vm: EditorViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        vm.documents.forEachIndexed { i, d ->
            val selected = i == vm.activeDocIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else CellBg)
                    .clickable { vm.selectDocument(i) }
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(d.name, color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(4.dp))
                    Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "关闭", tint = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White, modifier = Modifier.size(16.dp).clickable { vm.closeDocument(i) })
                }
            }
        }
        IconSquareButton(iconRes = R.drawable.ic_plus, size = 30) { vm.addDocument(32, 32) }
    }
}

@Composable
private fun FrameStrip(vm: EditorViewModel) {
    val doc = vm.activeDoc
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        doc.frames.forEachIndexed { i, f ->
            val selected = i == doc.activeFrameIndex
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else CellBg)
                    .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFF3A3A3E), RoundedCornerShape(6.dp))
                    .clickable { vm.selectFrame(i) },
                contentAlignment = Alignment.Center
            ) {
                if (i < vm.frameThumbs.size) {
                    Image(bitmap = vm.frameThumbs[i], contentDescription = "帧 ${i + 1}", contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                }
                Text("${i + 1}", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.background(Color(0x80000000), RectangleShape).padding(horizontal = 2.dp))
            }
        }
        Spacer(Modifier.width(4.dp))
        IconSquareButton(iconRes = R.drawable.ic_plus, size = 30) { vm.addFrame() }
        IconSquareButton(iconRes = R.drawable.ic_duplicate, size = 30) { vm.duplicateFrame() }
        IconSquareButton(iconRes = R.drawable.ic_close, size = 30) { vm.deleteFrame(doc.activeFrameIndex) }
    }
}

@Composable
private fun ToolColumn(vm: EditorViewModel, modifier: Modifier = Modifier, onShapePositioned: (Float) -> Unit) {
    val shapeIcon = when (vm.shapeMode) {
        ShapeMode.LINE -> R.drawable.ic_line
        ShapeMode.RECTANGLE -> if (vm.shapeFill) R.drawable.ic_rect_filled else R.drawable.ic_rect
        ShapeMode.CIRCLE -> if (vm.shapeFill) R.drawable.ic_circle_filled else R.drawable.ic_circle
        ShapeMode.LEAF -> if (vm.shapeFill) R.drawable.ic_leaf_filled else R.drawable.ic_leaf
        ShapeMode.LASSO -> R.drawable.ic_lasso
    }
    val tools = listOf(
        Tool.PENCIL to R.drawable.ic_pencil,
        Tool.ERASER to R.drawable.ic_eraser,
        Tool.BUCKET to R.drawable.ic_bucket,
        Tool.EYEDROPPER to R.drawable.ic_eyedropper
    )
    Column(
        modifier = modifier.width(48.dp).fillMaxHeight().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        tools.forEach { (tool, icon) ->
            IconSquareButton(iconRes = icon, selected = vm.tool == tool, size = 40) { vm.selectTool(tool) }
        }
        IconSquareButton(
            iconRes = shapeIcon,
            selected = vm.tool == Tool.SHAPE,
            size = 40,
            modifier = Modifier.onGloballyPositioned { onShapePositioned(it.positionInRoot().y) }
        ) { vm.onShapeToolTap() }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PaletteBar(vm: EditorViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(48.dp).fillMaxHeight().padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        vm.palette.forEach { c ->
            Box(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(c))
                    .border(width = if (vm.color == c) 2.dp else 1.dp, color = if (vm.color == c) MaterialTheme.colorScheme.primary else Color(0xFF3A3A3E), shape = RoundedCornerShape(6.dp))
                    .clickable { vm.selectColor(c) }
            )
        }
    }
}

@Composable
private fun LayerPanel(vm: EditorViewModel, modifier: Modifier = Modifier) {
    val frame = vm.activeFrame()
    val active = vm.activeLayer()
    Column(
        modifier = modifier
            .width(96.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xE62A2A2E))
            .padding(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(R.drawable.ic_layers), contentDescription = "图层", tint = Color.White, modifier = Modifier.size(16.dp))
            Row {
                IconSquareButton(iconRes = R.drawable.ic_plus, size = 24) { vm.addLayer() }
                Spacer(Modifier.width(2.dp))
                IconSquareButton(iconRes = R.drawable.ic_arrow_up, size = 24) { vm.moveLayer(frame.activeLayerIndex, 1) }
                Spacer(Modifier.width(2.dp))
                IconSquareButton(iconRes = R.drawable.ic_arrow_down, size = 24) { vm.moveLayer(frame.activeLayerIndex, -1) }
                Spacer(Modifier.width(2.dp))
                IconSquareButton(iconRes = R.drawable.ic_close, size = 24) { vm.deleteLayer(frame.activeLayerIndex) }
            }
        }
        Spacer(Modifier.size(4.dp))
        frame.layers.indices.reversed().forEach { idx ->
            val layer = frame.layers[idx]
            val selected = idx == frame.activeLayerIndex
            val thumb = vm.layerThumbs.getOrNull(idx)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else CellBg)
                    .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                    .clickable { vm.selectLayer(idx) }
                    .padding(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (thumb != null) {
                        Image(bitmap = thumb, contentDescription = layer.name, contentScale = ContentScale.FillBounds, modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)))
                    } else {
                        Box(modifier = Modifier.size(20.dp).background(Color(0xFF3A3A3E), RoundedCornerShape(4.dp)))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(layer.name, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    Icon(painter = painterResource(if (layer.visible) R.drawable.ic_eye else R.drawable.ic_eye_off), contentDescription = if (layer.visible) "可见" else "隐藏", tint = Color.White, modifier = Modifier.size(14.dp).clickable { vm.toggleLayerVisible(idx) })
                }
            }
            Spacer(Modifier.size(2.dp))
        }
        if (frame.layers.size > 1) {
            Text("不透明 ${(active.opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
            Slider(value = active.opacity, onValueChange = { vm.setLayerOpacity(frame.activeLayerIndex, it) }, valueRange = 0f..1f)
        }
    }
}

@Composable
private fun BottomStrip(vm: EditorViewModel, modifier: Modifier = Modifier, onNew: () -> Unit, onResize: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var shortcutOpen by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box {
                IconSquareButton(iconRes = R.drawable.ic_menu, size = 40) { menuOpen = true }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("新建") }, onClick = { menuOpen = false; onNew() })
                    DropdownMenuItem(text = { Text("调整画布大小") }, onClick = { menuOpen = false; onResize() })
                    DropdownMenuItem(text = { Text("导出 PNG") }, onClick = { menuOpen = false; vm.exportPng() })
                    DropdownMenuItem(text = { Text("设置") }, onClick = { menuOpen = false; vm.showSettings = true })
                }
            }
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A2A2E))
                        .combinedClickable(
                            onClick = { shortcutOpen = true },
                            onLongClick = { vm.executeShortcut(vm.shortcutTapAction) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(vm.shortcutActionIcon(vm.shortcutTapAction)),
                        contentDescription = vm.shortcutTapAction.label,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                DropdownMenu(expanded = shortcutOpen, onDismissRequest = { shortcutOpen = false }) {
                    ShortcutAction.entries.forEach { action ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painter = painterResource(vm.shortcutActionIcon(action)), contentDescription = action.label, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(action.label)
                                }
                            },
                            onClick = {
                                shortcutOpen = false
                                vm.shortcutTapAction = action
                                vm.executeShortcut(action)
                            }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        IconSquareButton(iconRes = R.drawable.ic_move, selected = vm.panMode, size = 40) { vm.togglePan() }
        IconSquareButton(iconRes = R.drawable.ic_plus, size = 40) { vm.zoomIn() }
        IconSquareButton(iconRes = R.drawable.ic_minus, size = 40) { vm.zoomOut() }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconSquareButton(iconRes = R.drawable.ic_undo, size = 40, enabled = vm.canUndo) { vm.undo() }
            IconSquareButton(iconRes = R.drawable.ic_redo, size = 40, enabled = vm.canRedo) { vm.redo() }
        }
    }
}

@Composable
private fun ShapeSelector(vm: EditorViewModel, modifier: Modifier = Modifier) {
    val options = listOf(
        ShapeMode.LINE to "直线" to R.drawable.ic_line,
        ShapeMode.RECTANGLE to "矩形" to (if (vm.shapeFill) R.drawable.ic_rect_filled else R.drawable.ic_rect),
        ShapeMode.CIRCLE to "圆形" to (if (vm.shapeFill) R.drawable.ic_circle_filled else R.drawable.ic_circle),
        ShapeMode.LEAF to "柳叶" to (if (vm.shapeFill) R.drawable.ic_leaf_filled else R.drawable.ic_leaf),
        ShapeMode.LASSO to "套索填充" to R.drawable.ic_lasso
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xE62A2A2E))
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        options.forEach { (pair, icon) ->
            val (mode, name) = pair
            val selected = vm.shapeMode == mode
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { vm.selectShapeMode(mode) }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painter = painterResource(icon), contentDescription = name, tint = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(name, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.White, maxLines = 1)
            }
        }
        Spacer(
            Modifier
                .padding(horizontal = 4.dp)
                .height(1.dp)
                .background(Color(0x33FFFFFF))
        )
        Row(
            modifier = Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (vm.shapeFill) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { vm.shapeFill = !vm.shapeFill }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painterResource(R.drawable.ic_bucket), contentDescription = "填充", tint = if (vm.shapeFill) MaterialTheme.colorScheme.onPrimary else Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("填充", style = MaterialTheme.typography.labelSmall, color = if (vm.shapeFill) MaterialTheme.colorScheme.onPrimary else Color.White, maxLines = 1)
        }
    }
}

@Composable
private fun IconSquareButton(
    iconRes: Int,
    selected: Boolean = false,
    size: Int = 44,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(when { !enabled -> Color(0x22FFFFFF); selected -> MaterialTheme.colorScheme.primary; else -> CellBg })
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(painter = painterResource(iconRes), contentDescription = null, tint = when { !enabled -> Color.White.copy(alpha = 0.4f); selected -> MaterialTheme.colorScheme.onPrimary; else -> Color.White }, modifier = Modifier.size((size * 0.55f).dp))
    }
}

@Composable
private fun NewCanvasDialog(onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val presets = listOf(16 to 16, 32 to 32, 64 to 64, 128 to 128, 256 to 256, 512 to 512, 1024 to 1024, 2048 to 2048)
    var selected by remember { mutableStateOf(32 to 32) }
    var customW by remember { mutableStateOf("") }
    var customH by remember { mutableStateOf("") }
    var useCustom by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (useCustom) {
                    val w = customW.toIntOrNull()?.coerceIn(1, 4096) ?: 32
                    val h = customH.toIntOrNull()?.coerceIn(1, 4096) ?: 32
                    onConfirm(w, h)
                } else {
                    onConfirm(selected.first, selected.second)
                }
            }) { Text("创建") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("新建文档") },
        text = {
            Column {
                presets.forEach { (w, h) ->
                    Row(modifier = Modifier.fillMaxWidth().clickable { selected = w to h; useCustom = false }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(if (!useCustom && selected == w to h) MaterialTheme.colorScheme.primary else Color(0xFF3A3A3E)))
                        Spacer(Modifier.size(12.dp))
                        Text("$w × $h")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().clickable { useCustom = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(if (useCustom) MaterialTheme.colorScheme.primary else Color(0xFF3A3A3E)))
                    Spacer(Modifier.size(12.dp))
                    Text("自定义", color = Color.White)
                    Spacer(Modifier.size(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = customW, onValueChange = { customW = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(1f), placeholder = { Text("宽") },
                        singleLine = true, textStyle = MaterialTheme.typography.bodySmall
                    )
                    Text(" × ", color = Color.White)
                    androidx.compose.material3.OutlinedTextField(
                        value = customH, onValueChange = { customH = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(1f), placeholder = { Text("高") },
                        singleLine = true, textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}

@Composable
private fun ResizeCanvasDialog(vm: EditorViewModel, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val doc = vm.activeDoc
    var newW by remember { mutableStateOf(doc.width.toString()) }
    var newH by remember { mutableStateOf(doc.height.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val w = newW.toIntOrNull()?.coerceIn(1, 4096) ?: doc.width
                val h = newH.toIntOrNull()?.coerceIn(1, 4096) ?: doc.height
                onConfirm(w, h)
            }) { Text("调整") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("调整画布大小") },
        text = {
            Column {
                Text("当前: ${doc.width} × ${doc.height}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.size(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.OutlinedTextField(
                        value = newW, onValueChange = { newW = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(1f), label = { Text("宽度") },
                        singleLine = true, textStyle = MaterialTheme.typography.bodySmall
                    )
                    Text(" × ", color = Color.White)
                    androidx.compose.material3.OutlinedTextField(
                        value = newH, onValueChange = { newH = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.weight(1f), label = { Text("高度") },
                        singleLine = true, textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}

@Composable
private fun SettingsDialog(vm: EditorViewModel) {
    var selectedCategory by remember { mutableStateOf(0) }
    val categories = listOf("常用设置", "网格与背景")
    AlertDialog(
        onDismissRequest = { vm.showSettings = false },
        confirmButton = { TextButton(onClick = { vm.showSettings = false }) { Text("关闭") } },
        title = { Text("设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEachIndexed { i, name ->
                        val sel = selectedCategory == i
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (sel) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2E))
                                .clickable { selectedCategory = i }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(name, style = MaterialTheme.typography.labelSmall, color = if (sel) MaterialTheme.colorScheme.onPrimary else Color.White)
                        }
                    }
                }
                when (selectedCategory) {
                    0 -> {
                        Text("单击动作", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        ShortcutAction.entries.forEach { action ->
                            val sel = vm.shortcutTapAction == action
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                    .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { vm.shortcutTapAction = action }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(if (sel) MaterialTheme.colorScheme.onPrimary else Color(0xFF3A3A3E)))
                                Spacer(Modifier.size(8.dp))
                                Text(action.label, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.size(4.dp))
                        Text("长按动作", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        ShortcutAction.entries.forEach { action ->
                            val sel = vm.shortcutLongPressAction == action
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                    .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { vm.shortcutLongPressAction = action }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(if (sel) MaterialTheme.colorScheme.onPrimary else Color(0xFF3A3A3E)))
                                Spacer(Modifier.size(8.dp))
                                Text(action.label, color = Color.White, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.size(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { vm.autoSaveOnBackground = !vm.autoSaveOnBackground }.padding(8.dp)) {
                            Text("自动保存", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.weight(1f))
                            Switch(checked = vm.autoSaveOnBackground, onCheckedChange = { vm.autoSaveOnBackground = it })
                        }
                        Text("进后台时自动保存画布", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                    1 -> {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable { vm.toggleGrid() }.padding(8.dp)) {
                            Text("显示网格", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.weight(1f))
                            Switch(checked = vm.gridVisible, onCheckedChange = { vm.toggleGrid() })
                        }
                    }
                }
            }
        }
    )
}
