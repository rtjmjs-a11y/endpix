package com.example.endpix.pixel

class Document(val width: Int, val height: Int, var name: String) {

    val frames = mutableListOf<Frame>()
    var activeFrameIndex: Int = 0
        private set

    val displayCanvas = PixelCanvas(width, height)

    fun activeFrame(): Frame {
        val idx = activeFrameIndex.coerceIn(0, frames.lastIndex)
        activeFrameIndex = idx
        return frames[idx]
    }

    fun activeLayer(): Layer = activeFrame().activeLayer()

    fun addFrame(): Frame {
        val f = Frame(width, height)
        f.addLayer("Layer 1")
        frames.add(f)
        activeFrameIndex = frames.lastIndex
        return f
    }

    fun duplicateFrame(index: Int): Frame {
        if (index !in frames.indices) return addFrame()
        val src = frames[index]
        val f = Frame(width, height)
        for (layer in src.layers) {
            val copy = PixelCanvas(width, height)
            System.arraycopy(layer.canvas.pixels, 0, copy.pixels, 0, copy.pixels.size)
            copy.rebuildTexBuffer()
            f.layers.add(Layer(copy, layer.name, layer.opacity, layer.visible))
        }
        f.selectLayer(src.activeLayerIndex.coerceIn(0, f.layers.lastIndex))
        frames.add(index + 1, f)
        activeFrameIndex = index + 1
        return f
    }

    fun deleteFrame(index: Int) {
        if (frames.size <= 1) return
        if (index !in frames.indices) return
        frames.removeAt(index)
        activeFrameIndex = activeFrameIndex.coerceAtMost(frames.lastIndex)
    }

    fun selectFrame(index: Int) {
        if (index in frames.indices) activeFrameIndex = index
    }

    fun flatten() {
        val frame = activeFrame()
        val disp = displayCanvas.pixels
        val n = disp.size
        var i = 0
        while (i < n) { disp[i] = 0; i++ }
        compositeLayers(frame, 0, 0, width - 1, height - 1)
        displayCanvas.rebuildTexBuffer()
    }

    fun flattenRegion(minX: Int, minY: Int, maxX: Int, maxY: Int) {
        val sx = minX.coerceIn(0, width - 1)
        val sy = minY.coerceIn(0, height - 1)
        val ex = maxX.coerceIn(0, width - 1)
        val ey = maxY.coerceIn(0, height - 1)
        if (sx > ex || sy > ey) return
        val frame = activeFrame()
        val disp = displayCanvas.pixels
        var y = sy
        while (y <= ey) {
            var x = sx
            while (x <= ex) {
                disp[y * width + x] = 0
                x++
            }
            y++
        }
        compositeLayers(frame, sx, sy, ex, ey)
        displayCanvas.syncTexRegion(sx, sy, ex, ey)
        displayCanvas.markDirtyRegion(sx, sy, ex, ey)
    }

private fun compositeLayers(frame: Frame, sx: Int, sy: Int, ex: Int, ey: Int) {
        val disp = displayCanvas.pixels
        val w = width
        for (layer in frame.layers) {
            if (!layer.visible || layer.opacity <= 0f) continue
            val src = layer.canvas.pixels
            val op = layer.opacity
            if (op >= 1f) {
                var y = sy
                while (y <= ey) {
                    var x = sx
                    while (x <= ex) {
                        val idx = y * w + x
                        val s = src[idx]
                        if (s != 0) disp[idx] = s
                        x++
                    }
                    y++
                }
            } else {
                var y = sy
                while (y <= ey) {
                    var x = sx
                    while (x <= ex) {
                        val idx = y * w + x
                        val s = src[idx]
                        if (s == 0) { x++; continue }
                        val sa = (s ushr 24) / 255f * op
                        if (sa <= 0f) { x++; continue }
                        val d = disp[idx]
                        val da = (d ushr 24) / 255f
                        val oa = sa + da * (1f - sa)
                        if (oa <= 0f) { disp[idx] = 0; x++; continue }
                        val inv = 1f / oa
                        val sr = (s ushr 16 and 0xff) / 255f
                        val sg = (s ushr 8 and 0xff) / 255f
                        val sb = (s and 0xff) / 255f
                        val dr = (d ushr 16 and 0xff) / 255f
                        val dg = (d ushr 8 and 0xff) / 255f
                        val db = (d and 0xff) / 255f
                        disp[idx] = ((oa * 255f).toInt().coerceIn(0, 255) shl 24) or
                            (((sr * sa + dr * da * (1f - sa)) * inv * 255f).toInt().coerceIn(0, 255) shl 16) or
                            (((sg * sa + dg * da * (1f - sa)) * inv * 255f).toInt().coerceIn(0, 255) shl 8) or
                            (((sb * sa + db * da * (1f - sa)) * inv * 255f).toInt().coerceIn(0, 255))
                        x++
                    }
                    y++
                }
            }
        }
    }

    companion object {
        fun create(w: Int, h: Int, name: String): Document {
            val doc = Document(w, h, name)
            doc.addFrame()
            doc.flatten()
            return doc
        }
    }
}

fun compositeFrame(frame: Frame): IntArray {
    val w = frame.width
    val h = frame.height
    val out = IntArray(w * h)
    for (layer in frame.layers) {
        if (!layer.visible || layer.opacity <= 0f) continue
        val src = layer.canvas.pixels
        val op = layer.opacity
        var i = 0
        val n = out.size
        while (i < n) {
            val s = src[i]
            if (s == 0) { i++; continue }
            val sa = (s ushr 24) / 255f * op
            if (sa <= 0f) { i++; continue }
            val d = out[i]
            val da = (d ushr 24) / 255f
            val oa = sa + da * (1f - sa)
            if (oa <= 0f) { out[i] = 0; i++; continue }
            val inv = 1f / oa
            val sr = (s ushr 16 and 0xff) / 255f
            val sg = (s ushr 8 and 0xff) / 255f
            val sb = (s and 0xff) / 255f
            val dr = (d ushr 16 and 0xff) / 255f
            val dg = (d ushr 8 and 0xff) / 255f
            val db = (d and 0xff) / 255f
            val or = (sr * sa + dr * da * (1f - sa)) * inv
            val og = (sg * sa + dg * da * (1f - sa)) * inv
            val ob = (sb * sa + db * da * (1f - sa)) * inv
            val oaI = (oa * 255f + 0.5f).toInt()
            val orI = (or * 255f + 0.5f).toInt()
            val ogI = (og * 255f + 0.5f).toInt()
            val obI = (ob * 255f + 0.5f).toInt()
            out[i] = (oaI shl 24) or (orI shl 16) or (ogI shl 8) or obI
            i++
        }
    }
    return out
}
