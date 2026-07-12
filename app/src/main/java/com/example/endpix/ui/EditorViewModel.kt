package com.example.endpix.ui

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.endpix.R
import com.example.endpix.gl.PixelGLSurfaceView
import com.example.endpix.gl.PixelRenderer
import com.example.endpix.gl.StrokeListener
import com.example.endpix.pixel.DocData
import com.example.endpix.pixel.Document
import com.example.endpix.pixel.Frame
import com.example.endpix.pixel.FrameData
import com.example.endpix.pixel.History
import com.example.endpix.pixel.PpMode
import com.example.endpix.pixel.SelectMode
import com.example.endpix.pixel.Layer
import com.example.endpix.pixel.LayerData
import com.example.endpix.pixel.PerfMode
import com.example.endpix.pixel.PixelCanvas
import com.example.endpix.pixel.SaveData
import com.example.endpix.pixel.ShapeMode
import com.example.endpix.pixel.ShortcutAction
import com.example.endpix.pixel.Tool
import com.example.endpix.pixel.ExpandDir
import com.example.endpix.pixel.compositeFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel(app: Application) : AndroidViewModel(app), StrokeListener {

    val documents = mutableStateListOf<Document>()
    var activeDocIndex by mutableIntStateOf(0)
        private set
    var structureVersion by mutableIntStateOf(0)
        private set

    val activeDoc: Document
        get() = documents[activeDocIndex.coerceIn(0, documents.lastIndex)]

    val renderer: PixelRenderer

    var tool by mutableStateOf(Tool.PENCIL)
    var shapeMode by mutableStateOf(ShapeMode.LINE)
    var shapeSelectorOpen by mutableStateOf(false)
    var shapeFill by mutableStateOf(false)
    var brushSelectorOpen by mutableStateOf(false)
    var brushSize by mutableIntStateOf(1)
    var pixelPerfect by mutableStateOf(false)
    var ppMode by mutableStateOf(PpMode.EXTREME)
    var eraserSelectorOpen by mutableStateOf(false)
    var eraserSize by mutableIntStateOf(1)
    var bucketRemovePixels by mutableStateOf(false)
    var selectSelectorOpen by mutableStateOf(false)
    var selectMode by mutableStateOf(SelectMode.RECT)
    var hasSelection by mutableStateOf(false)
    var selX0 by mutableIntStateOf(0); var selY0 by mutableIntStateOf(0)
    var selX1 by mutableIntStateOf(0); var selY1 by mutableIntStateOf(0)
    private var selSnapshot: IntArray? = null
    private val selLassoPath = ArrayList<IntArray>()
    var perfMode by mutableStateOf(PerfMode.REGION)
    var uiCornerRadius by mutableFloatStateOf(8f)
    var hardwareAcceleration by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var shortcutTapAction by mutableStateOf(ShortcutAction.FIT)
    var shortcutLongPressAction by mutableStateOf(ShortcutAction.NONE)
    var autoSaveOnBackground by mutableStateOf(true)
    var color by mutableIntStateOf(0xFF000000.toInt())
    var gridVisible by mutableStateOf(true)
    var panMode by mutableStateOf(false)
    var expandDir by mutableStateOf(ExpandDir.BOTTOM_RIGHT)
    var zoomPercent by mutableIntStateOf(100)
    var canUndo by mutableStateOf(false)
    var canRedo by mutableStateOf(false)
    var exportMessage by mutableStateOf<String?>(null)

    var fpsDisplay by mutableIntStateOf(0)
    var cpuPercent by mutableIntStateOf(0)
    var gpuPercent by mutableIntStateOf(-1)

    private var lastCpuTime = 0L
    private var lastWallTime = 0L

    val palette = mutableStateListOf(
        0xFF000000.toInt(), 0xFFFFFFFF.toInt(),
        0xFFFF3B30.toInt(), 0xFFFFCC00.toInt(),
        0xFF34C759.toInt(), 0xFF0A84FF.toInt()
    )

    val frameThumbs = mutableStateListOf<ImageBitmap>()
    val layerThumbs = mutableStateListOf<ImageBitmap>()

    var glView: PixelGLSurfaceView? = null
        set(value) {
            field = value
            value?.let {
                it.listener = this
                it.panMode = panMode
                refreshThumbs()
            }
        }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val history = History()

    private var lastX = 0
    private var lastY = 0
    private var anchorX = 0
    private var anchorY = 0
    private var strokeSnapshot: IntArray? = null
    private val lassoPath = ArrayList<IntArray>()
    private val ppPoints = ArrayList<IntArray>()
    private var sMinX = 0; private var sMinY = 0; private var sMaxX = 0; private var sMaxY = 0

    init {
        val saved = loadState()
        if (saved != null) {
            restoreFromSave(saved)
        } else {
            documents.add(Document.create(DEFAULT_W, DEFAULT_H, "Doc 1"))
        }
        renderer = PixelRenderer(activeDoc.displayCanvas)
        renderer.hardwareAcceleration = false
        renderer.onScaleChanged = { zoomPercent = (it * 100).toInt() }
        startPerfMonitor()
    }

    fun activeFrame(): Frame = activeDoc.activeFrame()
    fun activeLayer(): Layer = activeFrame().activeLayer()

    private fun post(r: Runnable) = mainHandler.post(r)

    private fun refreshHistoryFlags() {
        post {
            canUndo = history.canUndo
            canRedo = history.canRedo
        }
    }

    private fun requestRender() {
        renderer.activeFrame = activeDoc.activeFrame()
        glView?.requestRender()
    }

    private fun applyFlush(layer: PixelCanvas, doc: Document) {
        when (perfMode) {
            PerfMode.HA -> {
                if (!renderer.hardwareAcceleration) {
                    renderer.hardwareAcceleration = true
                    renderer.invalidateTexture()
                }
                renderer.activeFrame = activeDoc.activeFrame()
                layer.markDirtyAll()
            }
            else -> {
                if (renderer.hardwareAcceleration) {
                    renderer.hardwareAcceleration = false
                    renderer.invalidateTexture()
                }
                if (perfMode == PerfMode.REGION)
                    layer.getAndClearDirty()?.let { doc.flattenRegion(it[0], it[1], it[2], it[3]) }
                else
                    doc.flatten()
            }
        }
    }

    private fun bumpStructure() {
        structureVersion++
    }

    private fun resetHistory() {
        history.clear()
        canUndo = false
        canRedo = false
    }

    private fun switchRendererTo(doc: Document) {
        renderer.canvas = doc.displayCanvas
        renderer.invalidateTexture()
        renderer.requestFit()
    }

    private fun refreshThumbs() {
        val doc = activeDoc
        val frame = activeFrame()
        glView?.queueEvent {
            val fImgs = ArrayList<ImageBitmap>()
            for (i in doc.frames.indices) {
                val f = doc.frames[i]
                val px = if (i == doc.activeFrameIndex) doc.displayCanvas.pixels.copyOf()
                else compositeFrame(f)
                fImgs.add(makeThumb(px, doc.width, doc.height))
            }
            val lImgs = ArrayList<ImageBitmap>()
            for (l in frame.layers) {
                lImgs.add(makeThumb(l.canvas.pixels.copyOf(), doc.width, doc.height))
            }
            post {
                frameThumbs.clear()
                frameThumbs.addAll(fImgs)
                layerThumbs.clear()
                layerThumbs.addAll(lImgs)
            }
        }
    }

    private fun makeThumb(src: IntArray, w: Int, h: Int): ImageBitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(src, 0, w, 0, 0, w, h)
        val scaled = Bitmap.createScaledBitmap(bmp, THUMB_PX, THUMB_PX, false)
        return scaled.asImageBitmap()
    }

    override fun onStrokeDown(canvasX: Float, canvasY: Float) {
        val t = tool
        val c = color
        val x = Math.round(canvasX)
        val y = Math.round(canvasY)
        val layer = activeLayer()
        val doc = activeDoc
        glView?.queueEvent {
            when (t) {
                Tool.PENCIL -> {
                    val sz = brushSize
                    val pp = pixelPerfect
                    history.push(layer.canvas.snapshot())
                    sMinX = x; sMinY = y; sMaxX = x; sMaxY = y
                    if (pp) layer.canvas.beginStroke()
                    if (sz <= 1 || pp) {
                        if (pp) layer.canvas.ppSet(x, y, c) else layer.canvas[x, y] = c
                    } else {
                        val r = sz / 2
                        var fy = y - r
                        while (fy <= y + r) {
                            var fx = x - r
                            while (fx <= x + r) { layer.canvas[fx, fy] = c; fx++ }
                            fy++
                        }
                    }
                    applyFlush(layer.canvas, doc)
                }
                Tool.ERASER -> {
                    val es = eraserSize
                    history.push(layer.canvas.snapshot())
                    if (es <= 1) {
                        layer.canvas[x, y] = 0
                    } else {
                        val r = es / 2
                        var fy = y - r
                        while (fy <= y + r) {
                            var fx = x - r
                            while (fx <= x + r) { layer.canvas[fx, fy] = 0; fx++ }
                            fy++
                        }
                    }
                    applyFlush(layer.canvas, doc)
                }
                Tool.BUCKET -> {
                    history.push(layer.canvas.snapshot())
                    if (bucketRemovePixels) layer.canvas.floodFill(x, y, 0) else layer.canvas.floodFill(x, y, c)
                    applyFlush(layer.canvas, doc)
                }
                Tool.SELECT -> {
                    if (hasSelection) {
                        confirmSelection(layer, doc)
                    } else {
                        selLassoPath.clear()
                    }
                }
                Tool.SHAPE -> {
                    strokeSnapshot = layer.canvas.snapshot()
                    if (shapeMode == ShapeMode.LASSO) {
                        lassoPath.clear()
                        lassoPath.add(intArrayOf(x, y))
                    }
                }
            }
            refreshHistoryFlags()
            requestRender()
        }
        lastX = x
        lastY = y
        anchorX = x
        anchorY = y
    }

    override fun onStrokeMove(canvasX: Float, canvasY: Float) {
        val t = tool
        val c = color
        val x = Math.round(canvasX)
        val y = Math.round(canvasY)
        val px = lastX
        val py = lastY
        val ax = anchorX
        val ay = anchorY
        val shape = shapeMode
        val layer = activeLayer()
        val doc = activeDoc
        glView?.queueEvent {
            when (t) {
                Tool.PENCIL -> {
                    if (px < sMinX) sMinX = px; if (py < sMinY) sMinY = py
                    if (px > sMaxX) sMaxX = px; if (py > sMaxY) sMaxY = py
                    if (x < sMinX) sMinX = x; if (y < sMinY) sMinY = y
                    if (x > sMaxX) sMaxX = x; if (y > sMaxY) sMaxY = y
                    layer.canvas.drawLineSize(px, py, x, y, c, brushSize, pixelPerfect)
                    applyFlush(layer.canvas, doc)
                }
                Tool.ERASER -> {
                    val es = eraserSize
                    if (es <= 1) layer.canvas.drawLine(px, py, x, y, 0)
                    else {
                        val r = es / 2
                        var dx = Math.abs(x - px); var dy = Math.abs(y - py)
                        val sx = if (px < x) 1 else -1; val sy = if (py < y) 1 else -1
                        var cx = px; var cy = py; var err = dx + dy
                        while (true) {
                            var fy = cy - r
                            while (fy <= cy + r) { var fx = cx - r; while (fx <= cx + r) { layer.canvas[fx, fy] = 0; fx++ }; fy++ }
                            if (cx == x && cy == y) break
                            val e2 = 2 * err
                            if (e2 >= dy) { err -= dy; cx += sx }
                            if (e2 <= dx) { err += dx; cy += sy }
                        }
                    }
                    applyFlush(layer.canvas, doc)
                }
                Tool.SHAPE -> {
                    val fl = shapeFill
                    if (shape == ShapeMode.LASSO) {
                        lassoPath.add(intArrayOf(x, y))
                        strokeSnapshot?.let { layer.canvas.restore(it) }
                        var ii = 0
                        while (ii + 1 < lassoPath.size) {
                            layer.canvas.drawLine(lassoPath[ii][0], lassoPath[ii][1], lassoPath[ii + 1][0], lassoPath[ii + 1][1], c)
                            ii++
                        }
                    } else {
                        strokeSnapshot?.let { layer.canvas.restore(it) }
                        when (shape) {
                            ShapeMode.LINE -> layer.canvas.drawLine(ax, ay, x, y, c)
                            ShapeMode.RECTANGLE -> layer.canvas.drawRect(ax, ay, x, y, c, fl)
                            ShapeMode.CIRCLE -> layer.canvas.drawCircle(ax, ay, x, y, c, fl)
                            ShapeMode.LEAF -> layer.canvas.drawLeaf(ax, ay, x, y, c, fl)
                            ShapeMode.LASSO -> {}
                        }
                    }
                    doc.flatten()
                }
                Tool.SELECT -> {
                    if (!hasSelection) {
                        if (selectMode == SelectMode.RECT) {
                            selX0 = minOf(ax, x); selY0 = minOf(ay, y)
                            selX1 = maxOf(ax, x); selY1 = maxOf(ay, y)
                        } else {
                            selLassoPath.add(intArrayOf(x, y))
                        }
                    }
                }
                else -> {}
            }
            requestRender()
        }
        post { glView?.requestRender() }
        lastX = x
        lastY = y
    }

    override fun onStrokeUp(canvasX: Float, canvasY: Float) {
        val t = tool
        val c = color
        val x = Math.round(canvasX)
        val y = Math.round(canvasY)
        val ax = anchorX
        val ay = anchorY
        val shape = shapeMode
        val layer = activeLayer()
        val doc = activeDoc
        glView?.queueEvent {
            when (t) {
                Tool.SHAPE -> {
                    val fl = shapeFill
                    if (shape == ShapeMode.LASSO) {
                        lassoPath.add(intArrayOf(x, y))
                        strokeSnapshot?.let { layer.canvas.restore(it) }
                        if (lassoPath.size >= 3) layer.canvas.fillPolygon(lassoPath, c)
                        strokeSnapshot?.let { history.push(it) }
                        lassoPath.clear()
                    } else {
                        strokeSnapshot?.let { layer.canvas.restore(it) }
                        when (shape) {
                            ShapeMode.LINE -> layer.canvas.drawLine(ax, ay, x, y, c)
                            ShapeMode.RECTANGLE -> layer.canvas.drawRect(ax, ay, x, y, c, fl)
                            ShapeMode.CIRCLE -> layer.canvas.drawCircle(ax, ay, x, y, c, fl)
                            ShapeMode.LEAF -> layer.canvas.drawLeaf(ax, ay, x, y, c, fl)
                            ShapeMode.LASSO -> {}
                        }
strokeSnapshot?.let { history.push(it) }
                    }
                    doc.flatten()
                }
                Tool.SELECT -> {
                    if (!hasSelection) {
                        if (selectMode == SelectMode.RECT) {
                            selX0 = minOf(ax, x); selY0 = minOf(ay, y)
                            selX1 = maxOf(ax, x); selY1 = maxOf(ay, y)
                        } else {
                            selLassoPath.add(intArrayOf(x, y))
                            selX0 = selLassoPath.minOf { it[0] }; selY0 = selLassoPath.minOf { it[1] }
                            selX1 = selLassoPath.maxOf { it[0] }; selY1 = selLassoPath.maxOf { it[1] }
                        }
                        if (selX1 > selX0 && selY1 > selY0) {
                            selSnapshot = layer.canvas.snapshot()
                            hasSelection = true
                        }
                    }
                }
                else -> {}
            }
            strokeSnapshot = null
            ppPoints.clear()
            if (t == Tool.PENCIL && pixelPerfect && ppMode == PpMode.EXTREME) {
                layer.canvas.cleanPixelJoints(sMinX - 1, sMinY - 1, sMaxX + 1, sMaxY + 1, c)
                doc.flatten()
            }
            refreshHistoryFlags()
            requestRender()
            refreshThumbs()
        }
    }

    override fun onStrokeCancel() {
        val layer = activeLayer()
        val doc = activeDoc
        glView?.queueEvent {
            strokeSnapshot?.let { layer.canvas.restore(it) }
            strokeSnapshot = null
            lassoPath.clear()
            ppPoints.clear()
            refreshHistoryFlags()
            requestRender()
            refreshThumbs()
        }
    }

    fun undo() {
        val layer = activeLayer()
        val doc = activeDoc
        glView?.queueEvent {
            val cur = layer.canvas.snapshot()
            val prev = history.undo(cur)
            if (prev != null) {
                layer.canvas.restore(prev)
                doc.flatten()
            }
            refreshHistoryFlags()
            requestRender()
            refreshThumbs()
        }
    }

    fun redo() {
        val layer = activeLayer()
        val doc = activeDoc
        glView?.queueEvent {
            val cur = layer.canvas.snapshot()
            val next = history.redo(cur)
            if (next != null) {
                layer.canvas.restore(next)
                doc.flatten()
            }
            refreshHistoryFlags()
            requestRender()
            refreshThumbs()
        }
    }

    fun toggleGrid() {
        gridVisible = !gridVisible
        renderer.gridVisible = gridVisible
        requestRender()
    }

    fun executeShortcut(action: ShortcutAction) {
        when (action) {
            ShortcutAction.NONE -> {}
            ShortcutAction.FIT -> fitView()
            ShortcutAction.SAVE -> exportPng()
            ShortcutAction.ENABLE_GRID -> toggleGrid()
            ShortcutAction.OPEN_SETTINGS -> showSettings = true
        }
    }

    fun shortcutActionIcon(action: ShortcutAction): Int = when (action) {
        ShortcutAction.NONE -> R.drawable.ic_shortcut
        ShortcutAction.FIT -> R.drawable.ic_fit
        ShortcutAction.SAVE -> R.drawable.ic_save
        ShortcutAction.ENABLE_GRID -> R.drawable.ic_grid
        ShortcutAction.OPEN_SETTINGS -> R.drawable.ic_menu
    }

    fun togglePan() {
        panMode = !panMode
        glView?.panMode = panMode
    }

    fun applyHardwareAcceleration(enabled: Boolean) {
        hardwareAcceleration = enabled
        renderer.hardwareAcceleration = enabled
        renderer.activeFrame = activeDoc.activeFrame()
        if (!enabled) {
            activeDoc.flatten()
            renderer.invalidateTexture()
        }
        glView?.requestRender()
    }

    fun fitView() {
        renderer.requestFit()
        requestRender()
    }

    fun zoomIn() {
        val cur = renderer.scale
        val next = PixelRenderer.ZOOM_LEVELS.firstOrNull { it > cur + 0.001f }
            ?: PixelRenderer.MAX_ZOOM
        applyZoom(next)
    }

    fun zoomOut() {
        val cur = renderer.scale
        val prev = PixelRenderer.ZOOM_LEVELS.lastOrNull { it < cur - 0.001f }
            ?: PixelRenderer.MIN_ZOOM
        applyZoom(prev)
    }

    private fun applyZoom(level: Float) {
        val center = renderer.viewportCenter()
        renderer.zoomAt(level, center[0], center[1])
        zoomPercent = (renderer.scale * 100).toInt()
        requestRender()
    }

    fun selectColor(c: Int) {
        color = c
        cancelPan()
    }

    fun selectTool(t: Tool) {
        tool = t
        cancelPan()
        if (t != Tool.SHAPE) shapeSelectorOpen = false
    }

    fun onShapeToolTap() {
        cancelPan()
        if (tool != Tool.SHAPE) {
            selectTool(Tool.SHAPE)
            shapeSelectorOpen = true
        } else {
            shapeSelectorOpen = !shapeSelectorOpen
        }
    }

    fun onPencilToolTap() {
        cancelPan()
        if (tool != Tool.PENCIL) {
            selectTool(Tool.PENCIL)
            brushSelectorOpen = true
        } else {
            brushSelectorOpen = !brushSelectorOpen
        }
    }

    fun onEraserToolTap() {
        cancelPan()
        if (tool != Tool.ERASER) {
            selectTool(Tool.ERASER)
            eraserSelectorOpen = true
        } else {
            eraserSelectorOpen = !eraserSelectorOpen
        }
    }

    fun onBucketToolTap() {
        cancelPan()
        selectTool(Tool.BUCKET)
    }

    fun onSelectToolTap() {
        cancelPan()
        if (tool != Tool.SELECT) {
            selectTool(Tool.SELECT)
            selectSelectorOpen = true
        } else {
            selectSelectorOpen = !selectSelectorOpen
        }
    }

    fun onSelectLongPress() {
        selectMode = SelectMode.RECT
        selX0 = 0; selY0 = 0; selX1 = activeDoc.width - 1; selY1 = activeDoc.height - 1
        selSnapshot = activeLayer().canvas.snapshot()
        hasSelection = true
    }

    fun confirmSelection(layer: Layer, doc: Document) {
        selSnapshot = null
        selLassoPath.clear()
        hasSelection = false
        doc.flatten()
    }

    fun cancelSelection(layer: Layer, doc: Document) {
        selSnapshot?.let { layer.canvas.restore(it) }
        selSnapshot = null
        selLassoPath.clear()
        hasSelection = false
        doc.flatten()
        requestRender()
    }

    fun copySelection(layer: Layer, doc: Document) {
        doc.flatten()
        requestRender()
    }

    fun selectShapeMode(mode: ShapeMode) {
        shapeMode = mode
        shapeSelectorOpen = false
    }

    private fun startPerfMonitor() {
        lastCpuTime = Process.getElapsedCpuTime()
        lastWallTime = SystemClock.elapsedRealtime()
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                fpsDisplay = renderer.fps
                val nowCpu = Process.getElapsedCpuTime()
                val nowWall = SystemClock.elapsedRealtime()
                val dCpu = nowCpu - lastCpuTime
                val dWall = nowWall - lastWallTime
                cpuPercent = if (dWall > 0) (dCpu * 100 / dWall).toInt().coerceIn(0, 999) else 0
                lastCpuTime = nowCpu
                lastWallTime = nowWall
                gpuPercent = readGpuPercent()
            }
        }
    }

    private fun readGpuPercent(): Int {
        val paths = listOf(
            "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
            "/sys/module/ged/parameters/gpu_loading",
            "/proc/gpufreq/gpufreq_var_dump",
            "/sys/kernel/debug/ged/gpu_utilization",
            "/sys/devices/platform/gpu/utilization"
        )
        for (path in paths) {
            try {
                val f = java.io.File(path)
                if (f.exists() && f.canRead()) {
                    val content = f.readText().trim()
                    val v = content.split("\\s+".toRegex())[0].toIntOrNull()
                    if (v != null && v in 0..100) return v
                }
            } catch (_: Exception) {}
        }
        return -1
    }

    private fun cancelPan() {
        if (panMode) {
            panMode = false
            glView?.panMode = false
        }
    }

    fun addToPalette(c: Int) {
        if (palette.none { it == c } && c != 0 && palette.size < 24) {
            palette.add(c)
        }
    }

    fun newCanvas(w: Int, h: Int) {
        addDocument(w, h)
    }

    fun addDocument(w: Int, h: Int) {
        val doc = Document.create(w, h, "Doc ${documents.size + 1}")
        documents.add(doc)
        activeDocIndex = documents.lastIndex
        switchRendererTo(doc)
        resetHistory()
        bumpStructure()
        refreshThumbs()
        requestRender()
    }

    fun selectDocument(index: Int) {
        if (index !in documents.indices || index == activeDocIndex) return
        activeDocIndex = index
        switchRendererTo(documents[index])
        resetHistory()
        bumpStructure()
        refreshThumbs()
        requestRender()
    }

    fun closeDocument(index: Int) {
        if (documents.size <= 1 || index !in documents.indices) return
        documents.removeAt(index)
        activeDocIndex = activeDocIndex.coerceAtMost(documents.lastIndex)
        switchRendererTo(activeDoc)
        resetHistory()
        bumpStructure()
        refreshThumbs()
        requestRender()
    }

    fun addFrame() {
        val doc = activeDoc
        doc.addFrame()
        doc.flatten()
        resetHistory()
        bumpStructure()
        refreshThumbs()
        requestRender()
    }

    fun duplicateFrame() {
        val doc = activeDoc
        doc.duplicateFrame(doc.activeFrameIndex)
        doc.flatten()
        resetHistory()
        bumpStructure()
        refreshThumbs()
        requestRender()
    }

    fun deleteFrame(index: Int) {
        val doc = activeDoc
        doc.deleteFrame(index)
        doc.flatten()
        resetHistory()
        bumpStructure()
        refreshThumbs()
        requestRender()
    }

    fun selectFrame(index: Int) {
        val doc = activeDoc
        if (index == doc.activeFrameIndex) return
        doc.selectFrame(index)
        doc.flatten()
        resetHistory()
        bumpStructure()
        refreshThumbs()
        requestRender()
    }

    fun addLayer() {
        val frame = activeFrame()
        frame.addLayer()
        activeDoc.flatten()
        resetHistory()
        bumpStructure()
        refreshThumbs()
        requestRender()
    }

    fun deleteLayer(index: Int) {
        val frame = activeFrame()
        frame.deleteLayer(index)
        activeDoc.flatten()
        resetHistory()
        bumpStructure()
        refreshThumbs()
        requestRender()
    }

    fun selectLayer(index: Int) {
        val frame = activeFrame()
        if (index == frame.activeLayerIndex) return
        frame.selectLayer(index)
        resetHistory()
        bumpStructure()
        refreshThumbs()
    }

    fun toggleLayerVisible(index: Int) {
        val frame = activeFrame()
        if (index !in frame.layers.indices) return
        val layer = frame.layers[index]
        layer.visible = !layer.visible
        activeDoc.flatten()
        bumpStructure()
        refreshThumbs()
        requestRender()
    }

    fun setLayerOpacity(index: Int, opacity: Float) {
        val frame = activeFrame()
        if (index !in frame.layers.indices) return
        frame.layers[index].opacity = opacity.coerceIn(0f, 1f)
        activeDoc.flatten()
        bumpStructure()
        refreshThumbs()
        requestRender()
    }

    fun moveLayer(index: Int, delta: Int) {
        val frame = activeFrame()
        if (index !in frame.layers.indices) return
        val to = (index + delta).coerceIn(0, frame.layers.lastIndex)
        if (to == index) return
        frame.moveLayer(index, to)
        activeDoc.flatten()
        bumpStructure()
        refreshThumbs()
        requestRender()
    }

    fun clearCanvas() {
        val layer = activeLayer()
        val doc = activeDoc
        glView?.queueEvent {
            history.push(layer.canvas.snapshot())
            layer.canvas.fillAll(0)
            doc.flatten()
            refreshHistoryFlags()
            requestRender()
            refreshThumbs()
        }
    }

    fun exportPng() {
        val doc = activeDoc
        val w = doc.width
        val h = doc.height
        val src = doc.displayCanvas.snapshot()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    bmp.setPixels(src, 0, w, 0, 0, w, h)
                    val resolver = getApplication<Application>().contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "not2pix_${System.currentTimeMillis()}.png")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/not2pix")
                    }
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext "保存失败"
                    resolver.openOutputStream(uri)?.use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    } ?: return@withContext "写入失败"
                    "已保存到 Pictures/not2pix"
                } catch (e: Exception) {
                    "出错: ${e.message}"
                }
            }
            exportMessage = result
        }
    }

    fun consumeExportMessage() {
        exportMessage = null
    }

    fun saveState() {
        if (!autoSaveOnBackground) return
        try {
            val data = buildSaveData()
            val file = java.io.File(getApplication<Application>().filesDir, "not2pix_save.dat")
            java.io.ObjectOutputStream(file.outputStream()).use { it.writeObject(data) }
        } catch (_: Exception) {}
    }

    private fun loadState(): SaveData? {
        return try {
            val file = java.io.File(getApplication<Application>().filesDir, "not2pix_save.dat")
            if (file.exists() && file.length() < 200_000_000L) {
                java.io.ObjectInputStream(file.inputStream()).use { it.readObject() as SaveData }
            } else {
                file.delete()
                null
            }
        } catch (_: Exception) {
            try { java.io.File(getApplication<Application>().filesDir, "not2pix_save.dat").delete() } catch (_: Exception) {}
            null
        }
    }

    private fun buildSaveData(): SaveData {
        val docs = documents.map { doc ->
            DocData(doc.name, doc.width, doc.height, doc.frames.map { frame ->
                FrameData(frame.layers.map { layer ->
                    LayerData(layer.name, layer.opacity, layer.visible, layer.canvas.pixels.copyOf())
                }, frame.activeLayerIndex)
            }, doc.activeFrameIndex)
        }
        return SaveData(docs, activeDocIndex, tool, color, shapeMode, shapeFill)
    }

    private fun restoreFromSave(data: SaveData) {
        try {
            documents.clear()
            for (docData in data.docs) {
                if (docData.w <= 0 || docData.h <= 0 || docData.w > 4096 || docData.h > 4096) continue
                val doc = Document(docData.w, docData.h, docData.name)
                for (frameData in docData.frames) {
                    val frame = Frame(docData.w, docData.h)
                    for (layerData in frameData.layers) {
                        if (layerData.pixels.size != docData.w * docData.h) continue
                        val canvas = PixelCanvas(docData.w, docData.h)
                        System.arraycopy(layerData.pixels, 0, canvas.pixels, 0, layerData.pixels.size)
                        canvas.rebuildTexBuffer()
                        frame.layers.add(Layer(canvas, layerData.name, layerData.opacity, layerData.visible))
                    }
                    if (frame.layers.isEmpty()) frame.addLayer("Layer 1")
                    frame.selectLayer(frameData.activeLayer.coerceIn(0, frame.layers.lastIndex))
                    doc.frames.add(frame)
                }
                if (doc.frames.isEmpty()) doc.addFrame()
                doc.selectFrame(docData.activeFrame.coerceIn(0, doc.frames.lastIndex))
                doc.flatten()
                documents.add(doc)
            }
            if (documents.isEmpty()) documents.add(Document.create(DEFAULT_W, DEFAULT_H, "Doc 1"))
            activeDocIndex = data.activeDoc.coerceIn(0, documents.lastIndex)
            tool = data.tool
            color = data.color
            shapeMode = data.shapeMode
            shapeFill = data.shapeFill
        } catch (e: Exception) {
            documents.clear()
            documents.add(Document.create(DEFAULT_W, DEFAULT_H, "Doc 1"))
            activeDocIndex = 0
        }
    }

    fun resizeCanvas(newW: Int, newH: Int, dir: ExpandDir = expandDir) {
        val oldDoc = activeDoc
        val newDoc = Document.create(newW, newH, oldDoc.name)
        val offX = dir.offsetX(oldDoc.width, newW)
        val offY = dir.offsetY(oldDoc.height, newH)
        for (fi in oldDoc.frames.indices) {
            val srcFrame = oldDoc.frames[fi]
            val dstFrame = if (fi < newDoc.frames.size) newDoc.frames[fi] else newDoc.addFrame()
            for (li in srcFrame.layers.indices) {
                val srcLayer = srcFrame.layers[li]
                val dstLayer = if (li < dstFrame.layers.size) dstFrame.layers[li] else dstFrame.addLayer(srcLayer.name)
                dstLayer.opacity = srcLayer.opacity
                dstLayer.visible = srcLayer.visible
                for (y in 0 until oldDoc.height) {
                    for (x in 0 until oldDoc.width) {
                        val nx = x + offX; val ny = y + offY
                        if (nx in 0 until newW && ny in 0 until newH) {
                            dstLayer.canvas[nx, ny] = srcLayer.canvas[x, y]
                        }
                    }
                }
            }
            if (fi < newDoc.frames.size) {
                dstFrame.selectLayer(srcFrame.activeLayerIndex.coerceIn(0, dstFrame.layers.lastIndex))
            }
        }
        newDoc.selectFrame(oldDoc.activeFrameIndex.coerceIn(0, newDoc.frames.lastIndex))
        newDoc.flatten()
        documents[activeDocIndex] = newDoc
        renderer.canvas = newDoc.displayCanvas
        renderer.invalidateTexture()
        renderer.requestFit()
        resetHistory()
        bumpStructure()
        requestRender()
    }

    override fun onCleared() {
        super.onCleared()
        saveState()
        glView?.onPause()
    }

    companion object {
        const val DEFAULT_W = 32
        const val DEFAULT_H = 32
        private const val THUMB_PX = 48
    }
}
