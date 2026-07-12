package com.example.endpix.pixel

import java.nio.ByteBuffer
import java.nio.ByteOrder

class PixelCanvas(val width: Int, val height: Int) {

    val pixels: IntArray = IntArray(width * height)

    private val texBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.BIG_ENDIAN)
    private val texInt = texBuffer.asIntBuffer()

    var dirtyMinX = width
        private set
    var dirtyMinY = height
        private set
    var dirtyMaxX = -1
        private set
    var dirtyMaxY = -1
        private set

    val hasDirtyRegion: Boolean get() = dirtyMaxX >= 0

    fun argbToRgba(c: Int): Int {
        val a = c ushr 24 and 0xff
        val r = c ushr 16 and 0xff
        val g = c ushr 8 and 0xff
        val b = c and 0xff
        return (r shl 24) or (g shl 16) or (b shl 8) or a
    }

    operator fun get(x: Int, y: Int): Int {
        if (x < 0 || y < 0 || x >= width || y >= height) return 0
        return pixels[y * width + x]
    }

    operator fun set(x: Int, y: Int, color: Int) {
        if (x < 0 || y < 0 || x >= width || y >= height) return
        val idx = y * width + x
        pixels[idx] = color
        texInt.put(idx, argbToRgba(color))
        markDirty(x, y)
    }

    fun markDirty(x: Int, y: Int) {
        if (x < dirtyMinX) dirtyMinX = x
        if (y < dirtyMinY) dirtyMinY = y
        if (x > dirtyMaxX) dirtyMaxX = x
        if (y > dirtyMaxY) dirtyMaxY = y
    }

    fun markDirtyRegion(minX: Int, minY: Int, maxX: Int, maxY: Int) {
        dirtyMinX = minX
        dirtyMinY = minY
        dirtyMaxX = maxX
        dirtyMaxY = maxY
    }

    fun markDirtyAll() {
        dirtyMinX = 0
        dirtyMinY = 0
        dirtyMaxX = width - 1
        dirtyMaxY = height - 1
    }

    fun clearDirty() {
        dirtyMinX = width
        dirtyMinY = height
        dirtyMaxX = -1
        dirtyMaxY = -1
    }

    fun rebuildTexBuffer() {
        var i = 0
        val n = pixels.size
        while (i < n) {
            texInt.put(i, argbToRgba(pixels[i]))
            i++
        }
        markDirtyAll()
    }

    fun syncTexRegion(minX: Int, minY: Int, maxX: Int, maxY: Int) {
        val safeMinX = minX.coerceIn(0, width - 1)
        val safeMinY = minY.coerceIn(0, height - 1)
        val safeMaxX = maxX.coerceIn(0, width - 1)
        val safeMaxY = maxY.coerceIn(0, height - 1)
        var y = safeMinY
        while (y <= safeMaxY) {
            var x = safeMinX
            while (x <= safeMaxX) {
                val idx = y * width + x
                texInt.put(idx, argbToRgba(pixels[idx]))
                x++
            }
            y++
        }
    }

    fun getAndClearDirty(): IntArray? {
        if (dirtyMaxX < 0) return null
        val result = intArrayOf(dirtyMinX, dirtyMinY, dirtyMaxX, dirtyMaxY)
        clearDirty()
        return result
    }

    fun peekDirty(): IntArray? {
        if (dirtyMaxX < 0) return null
        return intArrayOf(dirtyMinX, dirtyMinY, dirtyMaxX, dirtyMaxY)
    }

    fun snapshot(): IntArray = pixels.copyOf()

    fun restore(snapshot: IntArray) {
        System.arraycopy(snapshot, 0, pixels, 0, pixels.size)
        rebuildTexBuffer()
    }

    fun texBuffer(): ByteBuffer {
        texBuffer.position(0)
        return texBuffer
    }

    fun fillAll(color: Int) {
        var i = 0
        val n = pixels.size
        val rgba = argbToRgba(color)
        while (i < n) {
            pixels[i] = color
            texInt.put(i, rgba)
            i++
        }
        markDirtyAll()
    }

    fun floodFill(x: Int, y: Int, newColor: Int) {
        if (x < 0 || y < 0 || x >= width || y >= height) return
        val target = pixels[y * width + x]
        if (target == newColor) return
        val stack = ArrayDeque<Int>()
        stack.addLast(y * width + x)
        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            if (idx < 0 || idx >= pixels.size) continue
            if (pixels[idx] != target) continue
            pixels[idx] = newColor
            texInt.put(idx, argbToRgba(newColor))
            val px = idx % width
            val py = idx / width
            markDirty(px, py)
            if (px > 0) stack.addLast(idx - 1)
            if (px < width - 1) stack.addLast(idx + 1)
            if (py > 0) stack.addLast(idx - width)
            if (py < height - 1) stack.addLast(idx + width)
        }
    }

    fun drawLine(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        val dx = Math.abs(x1 - x0)
        val dy = -Math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        var cx = x0
        var cy = y0
        while (true) {
            this[cx, cy] = color
            if (cx == x1 && cy == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; cx += sx }
            if (e2 <= dx) { err += dx; cy += sy }
        }
    }

    fun drawPixelPerfectLine(x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        val dx = Math.abs(x1 - x0)
        val dy = Math.abs(y1 - y0)
        if (dx == 0 && dy == 0) { ppSet(x0, y0, color); return }
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var x = x0
        var y = y0
        if (dx > dy) {
            var d = 2 * dy - dx
            for (i in 0..dx) {
                ppSet(x, y, color)
                if (i == dx) break
                if (d > 0) { y += sy; d -= 2 * dx }
                x += sx; d += 2 * dy
            }
        } else {
            var d = 2 * dx - dy
            for (i in 0..dy) {
                ppSet(x, y, color)
                if (i == dy) break
                if (d > 0) { x += sx; d -= 2 * dy }
                y += sy; d += 2 * dx
            }
        }
    }

    private val strokePixels = HashSet<Long>()
    private fun key(x: Int, y: Int) = (x.toLong() shl 32) or (y.toLong() and 0xFFFFFFFFL)

    fun beginStroke() { strokePixels.clear() }

    fun ppSet(x: Int, y: Int, color: Int) {
        if (x < 0 || y < 0 || x >= width || y >= height) return
        this[x, y] = color
        strokePixels.add(key(x, y))
    }

    fun cleanPixelJoints(minX: Int, minY: Int, maxX: Int, maxY: Int, color: Int) {
        val cx0 = maxOf(0, minX)
        val cy0 = maxOf(0, minY)
        val cx1 = minOf(width - 2, maxX)
        val cy1 = minOf(height - 2, maxY)
        var found = true
        var passes = 0
        while (found && passes < 3) {
            found = false
            passes++
            var cy = cy0
            while (cy <= cy1) {
                var cx = cx0
                while (cx <= cx1) {
                    val a = this[cx, cy] == color
                    val b = this[cx + 1, cy] == color
                    val c = this[cx, cy + 1] == color
                    val d = this[cx + 1, cy + 1] == color
                    val n = (if (a) 1 else 0) + (if (b) 1 else 0) + (if (c) 1 else 0) + (if (d) 1 else 0)
                    if (n >= 3) {
                        var rx = cx; var ry = cy
                        if (n == 4 || !d)      { rx = cx;     ry = cy }
                        else if (!a)            { rx = cx + 1; ry = cy + 1 }
                        else if (!b)            { rx = cx;     ry = cy + 1 }
                        else                   { rx = cx + 1; ry = cy }
                        if (key(rx, ry) in strokePixels) {
                            this[rx, ry] = 0
                            strokePixels.remove(key(rx, ry))
                            found = true
                        }
                    }
                    cx++
                }
                cy++
            }
        }
    }

    private fun wouldCreate2x2(x: Int, y: Int, color: Int): Boolean {
        fun isC(xx: Int, yy: Int) = safeGet(xx, yy) == color
        return (isC(x - 1, y - 1) && isC(x, y - 1) && isC(x - 1, y)) ||
               (isC(x + 1, y - 1) && isC(x, y - 1) && isC(x + 1, y)) ||
               (isC(x - 1, y + 1) && isC(x, y + 1) && isC(x - 1, y)) ||
               (isC(x + 1, y + 1) && isC(x, y + 1) && isC(x + 1, y))
    }

    private fun safeGet(x: Int, y: Int): Int {
        return if (x in 0 until width && y in 0 until height) this[x, y] else 0
    }

    fun drawLineSize(x0: Int, y0: Int, x1: Int, y1: Int, color: Int, size: Int, pixelPerfect: Boolean) {
        if (size <= 1 || pixelPerfect) {
            drawPixelPerfectLine(x0, y0, x1, y1, color)
            return
        }
        val r = size / 2
        var dx = Math.abs(x1 - x0)
        var dy = -Math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        var cx = x0
        var cy = y0
        while (true) {
            var fy = if (pixelPerfect) cy else cy - r
            while (fy <= if (pixelPerfect) cy else cy + r) {
                var fx = if (pixelPerfect) cx else cx - r
                while (fx <= if (pixelPerfect) cx else cx + r) {
                    this[fx, fy] = color
                    fx++
                }
                fy++
            }
            if (cx == x1 && cy == y1) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                cx += sx
            }
            if (e2 <= dx) {
                err += dx
                cy += sy
            }
        }
    }

    fun drawRect(x0: Int, y0: Int, x1: Int, y1: Int, color: Int, filled: Boolean = false) {
        val left = minOf(x0, x1)
        val right = maxOf(x0, x1)
        val top = minOf(y0, y1)
        val bottom = maxOf(y0, y1)
        if (filled) {
            var y = top
            while (y <= bottom) {
                var x = left
                while (x <= right) {
                    this[x, y] = color
                    x++
                }
                y++
            }
            return
        }
        for (x in left..right) {
            this[x, top] = color
            this[x, bottom] = color
        }
        for (y in top..bottom) {
            this[left, y] = color
            this[right, y] = color
        }
    }

    fun drawCircle(x0: Int, y0: Int, x1: Int, y1: Int, color: Int, filled: Boolean = false) {
        val cx = (x0 + x1) / 2f
        val cy = (y0 + y1) / 2f
        val rx = Math.abs(x1 - x0) / 2f
        val ry = Math.abs(y1 - y0) / 2f
        if (rx < 0.5f && ry < 0.5f) {
            this[x0, y0] = color
            return
        }
        if (rx <= 0f || ry <= 0f) {
            this[x0, y0] = color
            return
        }
        if (filled) {
            val left = minOf(x0, x1)
            val right = maxOf(x0, x1)
            val top = minOf(y0, y1)
            val bottom = maxOf(y0, y1)
            var y = top
            while (y <= bottom) {
                var x = left
                while (x <= right) {
                    val ex = (x - cx) / rx
                    val ey = (y - cy) / ry
                    if (ex * ex + ey * ey <= 1f) this[x, y] = color
                    x++
                }
                y++
            }
            return
        }
        val perim = 2f * Math.PI.toFloat() * (rx + ry)
        val steps = perim.toInt().coerceAtLeast(32)
        var i = 0
        while (i < steps) {
            val a = 2f * Math.PI.toFloat() * i / steps
            val px = (cx + rx * Math.cos(a.toDouble()).toFloat()).toInt()
            val py = (cy + ry * Math.sin(a.toDouble()).toFloat()).toInt()
            this[px, py] = color
            i++
        }
    }

    fun drawLeaf(x0: Int, y0: Int, x1: Int, y1: Int, color: Int, filled: Boolean = false) {
        val dx = (x1 - x0).toFloat()
        val dy = (y1 - y0).toFloat()
        val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (len < 1f) {
            this[x0, y0] = color
            return
        }
        val mx = (x0 + x1) / 2f
        val my = (y0 + y1) / 2f
        val px = -dy / len
        val py = dx / len
        val h = len * 0.6f
        val r = Math.sqrt(((len / 2f) * (len / 2f) + h * h).toDouble()).toFloat()
        val c1x = mx + px * h
        val c1y = my + py * h
        val c2x = mx - px * h
        val c2y = my - py * h
        if (filled) {
            val bulge = r - h
            val minX = (minOf(x0, x1) - bulge).toInt()
            val maxX = (maxOf(x0, x1) + bulge).toInt()
            val minY = (minOf(y0, y1) - bulge).toInt()
            val maxY = (maxOf(y0, y1) + bulge).toInt()
            var yy = minY
            while (yy <= maxY) {
                var xx = minX
                while (xx <= maxX) {
                    val d1x = xx - c1x
                    val d1y = yy - c1y
                    val d2x = xx - c2x
                    val d2y = yy - c2y
                    if (d1x * d1x + d1y * d1y <= r * r && d2x * d2x + d2y * d2y <= r * r) {
                        this[xx, yy] = color
                    }
                    xx++
                }
                yy++
            }
        } else {
            val steps = (2f * Math.PI.toFloat() * r).toInt().coerceAtLeast(48)
            var i = 0
            while (i < steps) {
                val a = 2f * Math.PI.toFloat() * i / steps
                val ca = Math.cos(a.toDouble()).toFloat()
                val sa = Math.sin(a.toDouble()).toFloat()
                val p1x = c1x + r * ca
                val p1y = c1y + r * sa
                val d2 = (p1x - c2x) * (p1x - c2x) + (p1y - c2y) * (p1y - c2y)
                if (d2 <= r * r) this[p1x.toInt(), p1y.toInt()] = color
                val p2x = c2x + r * ca
                val p2y = c2y + r * sa
                val d1 = (p2x - c1x) * (p2x - c1x) + (p2y - c1y) * (p2y - c1y)
                if (d1 <= r * r) this[p2x.toInt(), p2y.toInt()] = color
                i++
            }
        }
    }

    fun fillPolygon(pts: ArrayList<IntArray>, color: Int) {
        if (pts.size < 3) return
        var minY = pts[0][1]
        var maxY = pts[0][1]
        for (p in pts) {
            if (p[1] < minY) minY = p[1]
            if (p[1] > maxY) maxY = p[1]
        }
        val n = pts.size
        var y = minY
        while (y <= maxY) {
            val xs = ArrayList<Int>()
            var i = 0
            while (i < n) {
                val p1 = pts[i]
                val p2 = pts[(i + 1) % n]
                val y1 = p1[1]
                val y2 = p2[1]
                if ((y1 <= y && y < y2) || (y2 <= y && y < y1)) {
                    val x1 = p1[0]
                    val x2 = p2[0]
                    val xi = x1 + (y - y1) * (x2 - x1).toFloat() / (y2 - y1).toFloat()
                    xs.add(xi.toInt())
                }
                i++
            }
            xs.sort()
            var j = 0
            while (j + 1 < xs.size) {
                var x = xs[j]
                val xe = xs[j + 1]
                while (x <= xe) {
                    this[x, y] = color
                    x++
                }
                j += 2
            }
            y++
        }
    }
}
