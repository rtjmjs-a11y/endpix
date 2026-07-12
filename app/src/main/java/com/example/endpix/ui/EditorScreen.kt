package com.example.endpix.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.example.endpix.R
import com.example.endpix.gl.PixelGLSurfaceView
import com.example.endpix.pixel.ExpandDir
import com.example.endpix.pixel.PerfMode
import com.example.endpix.pixel.PpMode
import com.example.endpix.pixel.SelectMode
import com.example.endpix.pixel.ShapeMode
import com.example.endpix.pixel.ShortcutAction
import com.example.endpix.pixel.Tool

private val CellBg = Color(0xB32A2A2E)
private val LocalCornerRadius = compositionLocalOf { 8f }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: EditorViewModel) {
    var showNewDialog by remember { mutableStateOf(false) }
    var showResizeDialog by remember { mutableStateOf(false) }
    @Suppress("UNUSED_EXPRESSION") vm.structureVersion

    var shapeBtnY by remember { mutableStateOf(0f) }
    var pencilBtnY by remember { mutableStateOf(0f) }
    var eraserBtnY by remember { mutableStateOf(0f) }
    var bucketBtnY by remember { mutableStateOf(0f) }
    var selectBtnY by remember { mutableStateOf(0f) }
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

    CompositionLocalProvider(LocalCornerRadius provides vm.uiCornerRadius) {
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

        ToolColumn(vm, Modifier.align(Alignment.CenterStart), onShapePositioned = { shapeBtnY = it }, onPencilPositioned = { pencilBtnY = it }, onEraserPositioned = { eraserBtnY = it }, onBucketPositioned = { bucketBtnY = it }, onSelectPositioned = { selectBtnY = it })

        PaletteBar(vm, Modifier.align(Alignment.CenterEnd))

        val topOffset = with(density) { topStripsH.toDp() }
        LayerPanel(vm, Modifier.align(Alignment.TopEnd).offset(y = topOffset))

        BottomStrip(vm, Modifier.align(Alignment.BottomCenter).navigationBarsPadding(), onNew = { showNewDialog = true }, onResize = { showResizeDialog = true })

        if (vm.tool == Tool.SHAPE && vm.shapeSelectorOpen) {
            val offsetDp = with(density) { (shapeBtnY - boxY).coerceAtLeast(0f).toDp() }
            ShapeSelector(vm, Modifier.align(Alignment.TopStart).offset(x = 52.dp, y = offsetDp))
        }
        if (vm.tool == Tool.PENCIL && vm.brushSelectorOpen) {
            val offsetDp = with(density) { (pencilBtnY - boxY).coerceAtLeast(0f).toDp() }
            BrushSelector(vm, Modifier.align(Alignment.TopStart).offset(x = 52.dp, y = offsetDp))
        }
        if (vm.tool == Tool.ERASER && vm.eraserSelectorOpen) {
            val offsetDp = with(density) { (eraserBtnY - boxY).coerceAtLeast(0f).toDp() }
            EraserSelector(vm, Modifier.align(Alignment.TopStart).offset(x = 52.dp, y = offsetDp))
        }
        if (vm.tool == Tool.BUCKET) {
            val offsetDp = with(density) { (bucketBtnY - boxY).coerceAtLeast(0f).toDp() }
            BucketSelector(vm, Modifier.align(Alignment.TopStart).offset(x = 52.dp, y = offsetDp))
        }
        if (vm.tool == Tool.SELECT && vm.selectSelectorOpen) {
            val offsetDp = with(density) { (selectBtnY - boxY).coerceAtLeast(0f).toDp() }
            SelectSelector(vm, Modifier.align(Alignment.TopStart).offset(x = 52.dp, y = offsetDp))
        }
    }

    if (vm.showColorPicker) {
        val idx = vm.pickerIndex
        if (idx in vm.palette.indices) {
            ColorPickerDialog(vm.palette[idx], { vm.commitColorPicker(it) }, { vm.showColorPicker = false })
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
}

@Composable
private fun StatusBar(vm: EditorViewModel) {
    val doc = vm.activeDoc
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(LocalCornerRadius.current.dp))
            .background(Color(0x4D000000))
            .padding(horizontal = 8.dp, vertical = 3.dp)
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
                    .clip(RoundedCornerShape(LocalCornerRadius.current.dp))
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
                    .clip(RoundedCornerShape(LocalCornerRadius.current.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else CellBg)
                    .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color(0xFF3A3A3E), RoundedCornerShape(LocalCornerRadius.current.dp))
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
private fun ToolColumn(vm: EditorViewModel, modifier: Modifier = Modifier, onShapePositioned: (Float) -> Unit, onPencilPositioned: (Float) -> Unit, onEraserPositioned: (Float) -> Unit, onBucketPositioned: (Float) -> Unit, onSelectPositioned: (Float) -> Unit) {
    val shapeIcon = when (vm.shapeMode) {
        ShapeMode.LINE -> R.drawable.ic_line
        ShapeMode.RECTANGLE -> if (vm.shapeFill) R.drawable.ic_rect_filled else R.drawable.ic_rect
        ShapeMode.CIRCLE -> if (vm.shapeFill) R.drawable.ic_circle_filled else R.drawable.ic_circle
        ShapeMode.LEAF -> if (vm.shapeFill) R.drawable.ic_leaf_filled else R.drawable.ic_leaf
        ShapeMode.LASSO -> R.drawable.ic_lasso
    }
    Column(
        modifier = modifier.width(48.dp).fillMaxHeight().padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(modifier = Modifier.onGloballyPositioned { onPencilPositioned(it.positionInRoot().y) }) {
            IconSquareButton(iconRes = R.drawable.ic_pencil, selected = vm.tool == Tool.PENCIL, size = 40) { vm.onPencilToolTap() }
            if (vm.tool == Tool.PENCIL && vm.brushSize > 1 && !vm.pixelPerfect) {
                Text("${vm.brushSize}", color = Color(0xFF64B5F6), style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = -2.dp)
                        .clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(0xDD1A1A1E)).padding(horizontal = 3.dp))
            }
        }
        Box(modifier = Modifier.onGloballyPositioned { onEraserPositioned(it.positionInRoot().y) }) {
            IconSquareButton(iconRes = R.drawable.ic_eraser, selected = vm.tool == Tool.ERASER, size = 40) { vm.onEraserToolTap() }
            if (vm.tool == Tool.ERASER && vm.eraserSize > 1) {
                Text("${vm.eraserSize}", color = Color(0xFF64B5F6), style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = -2.dp)
                        .clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(0xDD1A1A1E)).padding(horizontal = 3.dp))
            }
        }
        Box(modifier = Modifier.onGloballyPositioned { onBucketPositioned(it.positionInRoot().y) }) {
            IconSquareButton(iconRes = R.drawable.ic_bucket, selected = vm.tool == Tool.BUCKET, size = 40) { vm.onBucketToolTap() }
        }
        Box(modifier = Modifier.onGloballyPositioned { onSelectPositioned(it.positionInRoot().y) }) {
            val selIcon = if (vm.selectMode == SelectMode.RECT) R.drawable.ic_select_rect else R.drawable.ic_select_lasso
            IconSquareButton(iconRes = selIcon, selected = vm.tool == Tool.SELECT, size = 40) { vm.onSelectToolTap() }
        }
        IconSquareButton(
            iconRes = shapeIcon, selected = vm.tool == Tool.SHAPE, size = 40,
            modifier = Modifier.onGloballyPositioned { onShapePositioned(it.positionInRoot().y) }
        ) { vm.onShapeToolTap() }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PaletteBar(vm: EditorViewModel, modifier: Modifier = Modifier) {
    var popupIdx by remember { mutableIntStateOf(-1) }
    Column(
        modifier = modifier.width(48.dp).fillMaxHeight().padding(4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        vm.palette.forEachIndexed { idx, c ->
            Box(modifier = Modifier) {
                Box(
                    modifier = Modifier.padding(vertical = 2.dp).size(30.dp)
                        .clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(c))
                        .border(width = if (vm.color == c) 2.dp else 1.dp, color = if (vm.color == c) MaterialTheme.colorScheme.primary else Color(0xFF3A3A3E), shape = RoundedCornerShape(LocalCornerRadius.current.dp))
                        .combinedClickable(
                            onClick = { if (vm.eyedropperActive) { vm.selectColor(c); vm.eyedropperActive = false } else popupIdx = idx },
                            onLongClick = { vm.selectColor(c) }
                        )
                )
                DropdownMenu(expanded = popupIdx == idx, onDismissRequest = { popupIdx = -1 }) {
                    DropdownMenuItem(text = { Row { Icon(painterResource(R.drawable.ic_palette), null, Modifier.size(18.dp), tint = Color.White); Spacer(Modifier.width(8.dp)); Text("调色盘", color = Color.White) } }, onClick = { popupIdx = -1; vm.openColorPicker(idx) })
                    DropdownMenuItem(text = { Row { Icon(painterResource(R.drawable.ic_eyedropper), null, Modifier.size(18.dp), tint = Color.White); Spacer(Modifier.width(8.dp)); Text("取色器", color = Color.White) } }, onClick = { popupIdx = -1; vm.eyedropperActive = true })
                }
            }
        }
        if (vm.eyedropperActive) {
            Spacer(Modifier.size(4.dp))
            Icon(painterResource(R.drawable.ic_eyedropper), "取色中", tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp))
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
            .clip(RoundedCornerShape(LocalCornerRadius.current.dp))
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
                    .clip(RoundedCornerShape(LocalCornerRadius.current.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else CellBg)
                    .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(LocalCornerRadius.current.dp))
                    .clickable { vm.selectLayer(idx) }
                    .padding(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (thumb != null) {
                        Image(bitmap = thumb, contentDescription = layer.name, contentScale = ContentScale.FillBounds, modifier = Modifier.size(20.dp).clip(RoundedCornerShape(LocalCornerRadius.current.dp)))
                    } else {
                        Box(modifier = Modifier.size(20.dp).background(Color(0xFF3A3A3E), RoundedCornerShape(LocalCornerRadius.current.dp)))
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
                        .clip(RoundedCornerShape(LocalCornerRadius.current.dp))
                        .background(Color(0xFF2A2A2E))
                        .combinedClickable(
                            onClick = { vm.executeShortcut(vm.shortcutTapAction) },
                            onLongClick = { vm.executeShortcut(vm.shortcutLongPressAction) }
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
            .clip(RoundedCornerShape(LocalCornerRadius.current.dp))
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
                    .clip(RoundedCornerShape(LocalCornerRadius.current.dp))
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
                .width(70.dp)
                .height(1.dp)
                .background(Color(0x66FFFFFF))
        )
        Row(
            modifier = Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(LocalCornerRadius.current.dp))
                .background(if (vm.shapeFill) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { vm.shapeFill = !vm.shapeFill }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painterResource(R.drawable.ic_bucket), contentDescription = "填充", tint = if (vm.shapeFill) MaterialTheme.colorScheme.onPrimary else Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("空心填充", style = MaterialTheme.typography.labelSmall, color = if (vm.shapeFill) MaterialTheme.colorScheme.onPrimary else Color.White, maxLines = 1)
        }
    }
}

@Composable
private fun BrushSelector(vm: EditorViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(0xE62A2A2E)).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val ppDisabled = vm.pixelPerfect
        Text("画笔大小: ${if (ppDisabled) 1 else vm.brushSize}", color = if (ppDisabled) Color.Gray else Color.White, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp).width(160.dp)) {
            Text("1", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Slider(value = if (ppDisabled) 1f else vm.brushSize.toFloat(), onValueChange = { if (!ppDisabled) vm.brushSize = it.toInt().coerceIn(1, 10) }, valueRange = 1f..10f, steps = 8, modifier = Modifier.weight(1f), enabled = !ppDisabled)
            Text("10", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
        Text("像素完美(1px)", color = if (vm.pixelPerfect) Color.Black else Color.White, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(160.dp).clip(RoundedCornerShape(LocalCornerRadius.current.dp))
                .background(if (vm.pixelPerfect) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { vm.pixelPerfect = !vm.pixelPerfect }.padding(horizontal = 6.dp, vertical = 6.dp))
        if (vm.pixelPerfect) {
            Row(modifier = Modifier.width(160.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PpMode.entries.forEach { mode ->
                    val sel = vm.ppMode == mode
                    Text(mode.label, color = if (sel) Color.Black else Color.White, style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(LocalCornerRadius.current.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2E))
                            .clickable { vm.ppMode = mode }.padding(vertical = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun EraserSelector(vm: EditorViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(0xE62A2A2E)).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("橡皮擦大小: ${vm.eraserSize}", color = Color.White, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(24.dp).width(160.dp)) {
            Text("1", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Slider(value = vm.eraserSize.toFloat(), onValueChange = { vm.eraserSize = it.toInt().coerceIn(1, 10) }, valueRange = 1f..10f, steps = 8, modifier = Modifier.weight(1f))
            Text("10", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun BucketSelector(vm: EditorViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(0xE62A2A2E)).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("移除像素", color = if (vm.bucketRemovePixels) Color.Black else Color.White, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(160.dp).clip(RoundedCornerShape(LocalCornerRadius.current.dp))
                .background(if (vm.bucketRemovePixels) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { vm.bucketRemovePixels = !vm.bucketRemovePixels }.padding(horizontal = 6.dp, vertical = 6.dp))
    }
}

@Composable
private fun SelectSelector(vm: EditorViewModel, modifier: Modifier = Modifier) {
    val layer = vm.activeLayer()
    val doc = vm.activeDoc
    Column(
        modifier = modifier.clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(0xE62A2A2E)).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SelectMode.entries.forEach { mode ->
            val sel = vm.selectMode == mode
            Text(if (mode == SelectMode.RECT) "方形选择" else "套索选择", color = if (sel) Color.Black else Color.White, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(160.dp).clip(RoundedCornerShape(LocalCornerRadius.current.dp))
                    .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { vm.selectMode = mode }.padding(horizontal = 6.dp, vertical = 6.dp))
        }
        if (vm.hasSelection) {
            Spacer(Modifier.height(2.dp).width(70.dp).background(Color(0x66FFFFFF)))
            Row(modifier = Modifier.width(160.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("确认", color = Color.Black, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(LocalCornerRadius.current.dp))
                        .background(Color(0xFF4CAF50)).clickable { vm.confirmSelection(layer, doc) }.padding(vertical = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("复制", color = Color.Black, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(LocalCornerRadius.current.dp))
                        .background(Color(0xFF2196F3)).clickable { vm.copySelection(layer, doc) }.padding(vertical = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("取消", color = Color.Black, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(LocalCornerRadius.current.dp))
                        .background(Color(0xFFF44336)).clickable { vm.cancelSelection(layer, doc) }.padding(vertical = 4.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
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
            .clip(RoundedCornerShape(LocalCornerRadius.current.dp))
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
                        Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(if (!useCustom && selected == w to h) MaterialTheme.colorScheme.primary else Color(0xFF3A3A3E)))
                        Spacer(Modifier.size(12.dp))
                        Text("$w × $h")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().clickable { useCustom = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(if (useCustom) MaterialTheme.colorScheme.primary else Color(0xFF3A3A3E)))
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
    var selectedDir by remember { mutableStateOf(vm.expandDir) }
    var dirPickerOpen by remember { mutableStateOf(false) }
    val dirs = listOf(
        listOf(ExpandDir.TOP_LEFT, ExpandDir.TOP, ExpandDir.TOP_RIGHT),
        listOf(ExpandDir.LEFT, ExpandDir.CENTER, ExpandDir.RIGHT),
        listOf(ExpandDir.BOTTOM_LEFT, ExpandDir.BOTTOM, ExpandDir.BOTTOM_RIGHT)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = {
            val w = newW.toIntOrNull()?.coerceIn(1, 4096) ?: doc.width
            val h = newH.toIntOrNull()?.coerceIn(1, 4096) ?: doc.height
            vm.expandDir = selectedDir; onConfirm(w, h)
        }) { Text("调整") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("调整画布大小") },
        text = { Column {
            Text("当前: ${doc.width} × ${doc.height}", color = Color.White, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedTextField(value = newW, onValueChange = { newW = it.filter { c -> c.isDigit() } }, modifier = Modifier.weight(1f), label = { Text("宽度") }, singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
                Text(" × ", color = Color.White)
                androidx.compose.material3.OutlinedTextField(value = newH, onValueChange = { newH = it.filter { c -> c.isDigit() } }, modifier = Modifier.weight(1f), label = { Text("高度") }, singleLine = true, textStyle = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.size(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("扩展方向: ", color = Color.White, style = MaterialTheme.typography.bodySmall)
                Box {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { dirPickerOpen = !dirPickerOpen }) {
                        Icon(painter = painterResource(selectedDir.iconRes()), contentDescription = selectedDir.label, tint = Color(0xFF64B5F6), modifier = Modifier.size(20.dp))
                        Text(selectedDir.label, color = Color(0xFF64B5F6), style = MaterialTheme.typography.bodySmall)
                    }
                    DropdownMenu(expanded = dirPickerOpen, onDismissRequest = { dirPickerOpen = false }) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            for (row in dirs) {
                                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                    for (dir in row) {
                                        val isSel = dir == selectedDir
                                        Box(modifier = Modifier.size(40.dp).clickable { selectedDir = dir; dirPickerOpen = false }.then(if (isSel) Modifier.background(Color(0x30FFFFFF), RoundedCornerShape(LocalCornerRadius.current.dp)) else Modifier), contentAlignment = Alignment.Center) {
                                            Icon(painter = painterResource(dir.iconRes()), contentDescription = dir.label, tint = if (isSel) Color(0xFF64B5F6) else Color(0xB0FFFFFF), modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } }
)
}

@Composable
private fun ShortcutPicker(label: String, selected: ShortcutAction, onSelect: (ShortcutAction) -> Unit, icon: @Composable (ShortcutAction) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(0xFF2A2A2E)).clickable { open = true }.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("$label: ", color = Color.White, style = MaterialTheme.typography.bodySmall)
            icon(selected)
            Text(selected.label, color = Color(0xFF64B5F6), style = MaterialTheme.typography.bodySmall)
            Text("▼", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Column(modifier = Modifier.padding(4.dp)) {
                ShortcutAction.entries.forEach { action ->
                    val sel = action == selected
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onSelect(action); open = false }.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        icon(action)
                        Spacer(Modifier.width(6.dp))
                        Text(action.label, style = MaterialTheme.typography.bodySmall, color = if (sel) MaterialTheme.colorScheme.onPrimary else Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsDialog(vm: EditorViewModel) {
    var selectedCategory by remember { mutableStateOf(0) }
    val categories = listOf("常用设置", "网格与背景", "特殊选项")
    AlertDialog(
        onDismissRequest = { vm.showSettings = false },
        confirmButton = { TextButton(onClick = { vm.showSettings = false }) { Text("关闭") } },
        title = { Text("设置") },
        shape = RoundedCornerShape(LocalCornerRadius.current.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.forEachIndexed { i, name ->
                        val sel = selectedCategory == i
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(if (sel) MaterialTheme.colorScheme.primary else Color(0xFF2A2A2E))
                                .clickable { selectedCategory = i }.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) { Text(name, style = MaterialTheme.typography.labelSmall, color = if (sel) MaterialTheme.colorScheme.onPrimary else Color.White) }
                    }
                }
                when (selectedCategory) {
                    0 -> {
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(0x4DFFFFFF)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text("快捷键", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.size(4.dp))
                        ShortcutPicker(label = "单击动作", selected = vm.shortcutTapAction, onSelect = { vm.shortcutTapAction = it }, icon = { painterResource(vm.shortcutActionIcon(it)) })
                        Spacer(Modifier.size(6.dp))
                        ShortcutPicker(label = "长按动作", selected = vm.shortcutLongPressAction, onSelect = { vm.shortcutLongPressAction = it }, icon = { painterResource(vm.shortcutActionIcon(it)) })
                        Spacer(Modifier.size(6.dp))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(0x4DFFFFFF)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text("UI图图形", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.size(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 4.dp)) {
                            Text("UI圆角: ${vm.uiCornerRadius.toInt()}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.weight(1f))
                            Text("0", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                            Slider(value = vm.uiCornerRadius, onValueChange = { vm.uiCornerRadius = it.coerceIn(0f, 20f) }, valueRange = 0f..20f, steps = 19, modifier = Modifier.width(100.dp))
                            Text("20", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.size(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(LocalCornerRadius.current.dp)).clickable { vm.autoSaveOnBackground = !vm.autoSaveOnBackground }.padding(8.dp)) {
                            Text("自动保存", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.weight(1f))
                            Switch(checked = vm.autoSaveOnBackground, onCheckedChange = { vm.autoSaveOnBackground = it })
                        }
                        Text("进后台时自动保存画布", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                    }
                    1 -> {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(LocalCornerRadius.current.dp)).clickable { vm.toggleGrid() }.padding(8.dp)) {
                            Text("显示网格", color = Color.White, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.weight(1f))
                            Switch(checked = vm.gridVisible, onCheckedChange = { vm.toggleGrid() })
                        }
                    }
                    2 -> {
                        Text("性能方案", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        PerfMode.entries.forEach { mode ->
                            val sel = vm.perfMode == mode
                            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent).clickable { vm.perfMode = mode }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Text(mode.label, color = if (sel) MaterialTheme.colorScheme.onPrimary else Color.White, style = MaterialTheme.typography.bodySmall)
                                Text(mode.desc, color = if (sel) MaterialTheme.colorScheme.onPrimary else Color.Gray, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ColorPickerDialog(initialColor: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var h by remember { mutableFloatStateOf(0f) }
    var s by remember { mutableFloatStateOf(0f) }
    var v by remember { mutableFloatStateOf(0f) }
    var a by remember { mutableFloatStateOf(1f) }
    var hexText by remember { mutableStateOf("") }
    val initA = Color(initialColor).alpha
    val initHsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(Color(initialColor).red.toInt(),Color(initialColor).green.toInt(),Color(initialColor).blue.toInt(),initHsv)
    LaunchedEffect(initialColor) { h=initHsv[0]; s=initHsv[1]; v=initHsv[2]; a=initA }
    if (hexText.isBlank()) { val c = Color.hsv(h,s,v,a); hexText = String.format("#%02X%02X%02X",(c.red*255).toInt(),(c.green*255).toInt(),(c.blue*255).toInt()) }
    val newColor = Color.hsv(h,s,v,a)
    val newArgb = (a*255).toInt() shl 24 or ((newColor.red*255).toInt() shl 16) or ((newColor.green*255).toInt() shl 8) or (newColor.blue*255).toInt()
    val sq = 240.dp
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {}, dismissButton = {},
        shape = RoundedCornerShape(LocalCornerRadius.current.dp),
        text = { Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HueBar(Modifier.width(28.dp).height(sq).clip(RoundedCornerShape(LocalCornerRadius.current.dp)), h) { h = it; hexText = "" }
                Spacer(Modifier.width(6.dp))
                SvSquare(Modifier.size(sq).clip(RoundedCornerShape(LocalCornerRadius.current.dp)), h, s, v) { ns, nv -> s = ns; v = nv; hexText = "" }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(sq + 34.dp)) {
                Canvas(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(LocalCornerRadius.current.dp))) {
                    drawRect(Color(initialColor))
                    val path = Path().apply { moveTo(size.width, 0f); lineTo(0f, size.height); lineTo(size.width, size.height); close() }
                    drawPath(path, newColor)
                }
                Spacer(Modifier.width(6.dp))
                AlphaBar(Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(LocalCornerRadius.current.dp)), h, s, v, a) { a = it; hexText = "" }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val clip = LocalClipboardManager.current
                Box(Modifier.border(1.dp,Color(0x66FFFFFF),RoundedCornerShape(LocalCornerRadius.current.dp)).clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(0x18FFFFFF))) {
                    BasicTextField(value = hexText, onValueChange = { txt ->
                        val clean = txt.replace("#","").take(8)
                        hexText = "#$clean"
                        if (clean.length == 6 || clean.length == 8) { try { val c = android.graphics.Color.parseColor(if (clean.length == 6) clean else clean.takeLast(6)); val hv = FloatArray(3); android.graphics.Color.RGBToHSV((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF, hv); h=hv[0];s=hv[1];v=hv[2]; if (clean.length == 8) a = ((Integer.parseInt(clean.take(2),16))/255f).coerceIn(0f,1f) } catch (_:Exception){} }
                    }, singleLine = true, textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp).widthIn(min = 70.dp)
                            .pointerInput(Unit) { detectTapGestures(onLongPress = { clip.setText(AnnotatedString(hexText)) }) }
                    )
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).offset(y = 4.dp)) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(0xFF4CAF50)).clickable { onConfirm(newArgb) }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text("确定", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.width(24.dp))
                    Box(Modifier.weight(1f).clip(RoundedCornerShape(LocalCornerRadius.current.dp)).background(Color(0xFFF44336)).clickable { onDismiss() }.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text("取消", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        } }
    )
}

@Composable
private fun HueBar(modifier: Modifier, hue: Float, onHueChanged: (Float) -> Unit) {
    val hueColors = remember { (0..360 step 30).map { Color.hsv(it.toFloat(),1f,1f) }.toList() }
    Canvas(modifier = modifier.pointerInput(Unit) {
        detectTapGestures { onHueChanged((1f-(it.y/size.height).coerceIn(0f,1f))) }
        detectDragGestures { change, _ -> change.consume(); onHueChanged((1f-(change.position.y/size.height).coerceIn(0f,1f))) }
    }) {
        drawRect(Brush.verticalGradient(hueColors))
        drawRect(Color.White, Offset(size.width * 0.1f, (1f-hue)*size.height - 1.5f), Size(size.width * 0.8f, 3f))
    }
}

@Composable
private fun SvSquare(modifier: Modifier, hue: Float, sat: Float, val_: Float, onSvChanged: (Float, Float) -> Unit) {
    val hueColor = Color.hsv(hue, 1f, 1f)
    Canvas(modifier = modifier.pointerInput(Unit) {
        detectTapGestures { val sx=(it.x/size.width).coerceIn(0f,1f); val vy=1f-(it.y/size.height).coerceIn(0f,1f); onSvChanged(sx,vy) }
        detectDragGestures { change, _ -> change.consume(); val sx=(change.position.x/size.width).coerceIn(0f,1f); val vy=1f-(change.position.y/size.height).coerceIn(0f,1f); onSvChanged(sx,vy) }
    }) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        drawCircle(Color.White, radius = 7f, center = Offset(sat*size.width, (1f-val_)*size.height))
        drawCircle(Color.Black, radius = 5.5f, center = Offset(sat*size.width, (1f-val_)*size.height))
    }
}

@Composable
private fun AlphaBar(modifier: Modifier, hue: Float, sat: Float, val_: Float, alpha: Float, onAlphaChanged: (Float) -> Unit) {
    val color = Color.hsv(hue, sat, val_, 1f)
    Canvas(modifier = modifier.pointerInput(Unit) {
        detectTapGestures { onAlphaChanged((it.x/size.width).coerceIn(0f,1f)) }
        detectDragGestures { change, _ -> change.consume(); onAlphaChanged((change.position.x/size.width).coerceIn(0f,1f)) }
    }) {
        val sq = 4f; val n = (size.width/sq).toInt(); val m = (size.height/sq).toInt()
        for (dx in 0 until n) for (dy in 0 until m) drawRect(if((dx+dy)%2==0) Color.White else Color.Gray, Offset(dx*sq,dy*sq), Size(sq,sq))
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, color)))
        drawRect(Color.White, Offset(alpha*size.width - 1.5f, size.height*0.1f), Size(3f, size.height*0.8f))
    }
}
