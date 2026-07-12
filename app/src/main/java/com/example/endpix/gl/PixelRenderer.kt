package com.example.endpix.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.endpix.pixel.Frame
import com.example.endpix.pixel.Layer
import com.example.endpix.pixel.PixelCanvas
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PixelRenderer(var canvas: PixelCanvas) : GLSurfaceView.Renderer {

    @Volatile var scale: Float = 1f
        private set
    @Volatile var offsetX: Float = 0f
        private set
    @Volatile var offsetY: Float = 0f
        private set
    @Volatile var gridVisible: Boolean = true
    var onScaleChanged: ((Float) -> Unit)? = null

    private var program = 0
    private var uMVP = 0
    private var uViewport = 0
    private var uOffset = 0
    private var uScale = 0
    private var uTex = 0
    private var uTexSize = 0
    private var uShowGrid = 0
    private var uPixelScale = 0
    private var uMode = 0
    private var uOpacity = 0
    private var aPos = 0

    private var textureId = 0
    private var vbo = 0

    private val mvp = FloatArray(16)
    private val ortho = FloatArray(16)
    private val model = FloatArray(16)

    private var viewportW = 1
    private var viewportH = 1

    @Volatile private var textureDirty = true
    @Volatile private var refitRequested = true
    private var initialFitDone = false
    @Volatile var hardwareAcceleration = false

    var activeFrame: Frame? = null

    @Volatile var fps: Int = 0
        private set
    private var fpsCount = 0
    private var fpsLastNano = 0L

    private val quad = floatArrayOf(
        0f, 0f, 0f, 0f,
        1f, 0f, 1f, 0f,
        0f, 1f, 0f, 1f,
        1f, 1f, 1f, 1f
    )

    fun setTransform(scale: Float, offsetX: Float, offsetY: Float) {
        this.scale = scale
        this.offsetX = offsetX
        this.offsetY = offsetY
    }

    fun zoomAt(newScale: Float, focusX: Float, focusY: Float) {
        val s = newScale.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val cx = (focusX - offsetX) / scale
        val cy = (focusY - offsetY) / scale
        offsetX = focusX - cx * s
        offsetY = focusY - cy * s
        scale = s
        onScaleChanged?.invoke(s)
    }

    fun viewportCenter(): FloatArray = floatArrayOf(viewportW / 2f, viewportH / 2f)

    fun notifyScale() { onScaleChanged?.invoke(scale) }

    fun invalidateTexture() { textureDirty = true }

    fun requestFit() { refitRequested = true }

    private fun fitToViewport() {
        val sx = viewportW.toFloat() / canvas.width
        val sy = viewportH.toFloat() / canvas.height
        val fit = minOf(sx, sy) * 0.96f
        scale = snapLevelDown(fit)
        offsetX = (viewportW - canvas.width * scale) / 2f
        offsetY = (viewportH - canvas.height * scale) / 2f
        onScaleChanged?.invoke(scale)
    }

    private fun snapLevelDown(s: Float): Float {
        var chosen = MIN_ZOOM
        for (l in ZOOM_LEVELS) { if (l <= s) chosen = l }
        return chosen
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glDisable(GLES30.GL_BLEND)
        program = ShaderUtil.link(Shaders.vertex, Shaders.fragment)
        uMVP = GLES30.glGetUniformLocation(program, "uMVP")
        uViewport = GLES30.glGetUniformLocation(program, "uViewport")
        uOffset = GLES30.glGetUniformLocation(program, "uOffset")
        uScale = GLES30.glGetUniformLocation(program, "uScale")
        uTex = GLES30.glGetUniformLocation(program, "uTex")
        uTexSize = GLES30.glGetUniformLocation(program, "uTexSize")
        uShowGrid = GLES30.glGetUniformLocation(program, "uShowGrid")
        uPixelScale = GLES30.glGetUniformLocation(program, "uPixelScale")
        uMode = GLES30.glGetUniformLocation(program, "uMode")
        uOpacity = GLES30.glGetUniformLocation(program, "uOpacity")
        aPos = GLES30.glGetAttribLocation(program, "aPos")

        val buffers = IntArray(1)
        GLES30.glGenBuffers(1, buffers, 0)
        vbo = buffers[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        val quadBuffer = java.nio.ByteBuffer.allocateDirect(quad.size * 4)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
        quadBuffer.put(quad); quadBuffer.position(0)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quad.size * 4, quadBuffer, GLES30.GL_STATIC_DRAW)

        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        uploadFullTexture()
    }

    private fun uploadFullTexture() {
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, canvas.width)
        canvas.texBuffer().position(0)
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, canvas.width, canvas.height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, canvas.texBuffer())
        canvas.clearDirty()
        textureDirty = false
    }

    private fun uploadDirtyRegion() {
        if (!canvas.hasDirtyRegion) return
        val minX = canvas.dirtyMinX; val minY = canvas.dirtyMinY
        val maxX = canvas.dirtyMaxX; val maxY = canvas.dirtyMaxY
        val rw = maxX - minX + 1; val rh = maxY - minY + 1
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, canvas.width)
        canvas.texBuffer().position((minY * canvas.width + minX) * 4)
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, minX, minY, rw, rh, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, canvas.texBuffer())
        canvas.clearDirty()
    }

    private fun ensureLayerTexture(layer: Layer) {
        if (layer.glTextureId == 0) {
            val ids = IntArray(1)
            GLES30.glGenTextures(1, ids, 0)
            layer.glTextureId = ids[0]
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, layer.glTextureId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, layer.canvas.width)
            layer.canvas.texBuffer().position(0)
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, layer.canvas.width, layer.canvas.height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, layer.canvas.texBuffer())
            layer.canvas.clearDirty()
        } else if (layer.canvas.hasDirtyRegion) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, layer.glTextureId)
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
            GLES30.glPixelStorei(GLES30.GL_UNPACK_ROW_LENGTH, layer.canvas.width)
            val minX = layer.canvas.dirtyMinX; val minY = layer.canvas.dirtyMinY
            val maxX = layer.canvas.dirtyMaxX; val maxY = layer.canvas.dirtyMaxY
            layer.canvas.texBuffer().position((minY * layer.canvas.width + minX) * 4)
            GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, minX, minY, maxX - minX + 1, maxY - minY + 1, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, layer.canvas.texBuffer())
            layer.canvas.clearDirty()
        }
    }

    private fun drawQuad() {
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glEnableVertexAttribArray(aPos)
        GLES30.glVertexAttribPointer(aPos, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(aPos)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        viewportW = width; viewportH = height
        if (!initialFitDone && width > 0 && height > 0) { refitRequested = true; initialFitDone = true }
    }

    override fun onDrawFrame(gl: GL10?) {
        if (refitRequested) { fitToViewport(); refitRequested = false }
        if (!hardwareAcceleration) {
            if (textureDirty) uploadFullTexture() else uploadDirtyRegion()
        }

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        Matrix.orthoM(ortho, 0, 0f, viewportW.toFloat(), viewportH.toFloat(), 0f, -1f, 1f)
        Matrix.setIdentityM(model, 0)
        Matrix.multiplyMM(mvp, 0, ortho, 0, model, 0)

        GLES30.glUseProgram(program)
        GLES30.glUniformMatrix4fv(uMVP, 1, false, mvp, 0)
        GLES30.glUniform2f(uViewport, viewportW.toFloat(), viewportH.toFloat())
        GLES30.glUniform2f(uOffset, offsetX, offsetY)
        GLES30.glUniform1f(uScale, scale)
        GLES30.glUniform2f(uTexSize, canvas.width.toFloat(), canvas.height.toFloat())
        GLES30.glUniform1f(uPixelScale, scale)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glUniform1i(uTex, 0)

        if (hardwareAcceleration) {
            GLES30.glUniform1f(uMode, 3f)
            GLES30.glUniform1f(uOpacity, 1f)
            GLES30.glUniform1f(uShowGrid, 0f)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
            drawQuad()

            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            val frame = activeFrame
            if (frame != null) {
                for (layer in frame.layers) {
                    if (!layer.visible || layer.opacity <= 0f) continue
                    ensureLayerTexture(layer)
                    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, layer.glTextureId)
                    GLES30.glUniform1f(uMode, 1f)
                    GLES30.glUniform1f(uOpacity, layer.opacity)
                    drawQuad()
                }
            }
            GLES30.glDisable(GLES30.GL_BLEND)

            if (gridVisible) {
                GLES30.glUniform1f(uMode, 2f)
                GLES30.glUniform1f(uOpacity, 1f)
                GLES30.glUniform1f(uShowGrid, 1f)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
                drawQuad()
            }
        } else {
            GLES30.glUniform1f(uMode, 0f)
            GLES30.glUniform1f(uOpacity, 1f)
            GLES30.glUniform1f(uShowGrid, if (gridVisible) 1f else 0f)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
            drawQuad()
        }

        fpsCount++
        val now = System.nanoTime()
        if (fpsLastNano == 0L) fpsLastNano = now
        val elapsed = now - fpsLastNano
        if (elapsed >= 1_000_000_000L) {
            fps = (fpsCount * 1_000_000_000L / elapsed.coerceAtLeast(1L)).toInt()
            fpsCount = 0; fpsLastNano = now
        } else if (elapsed >= 500_000_000L && fpsCount == 0) { fps = 0 }
    }

    fun screenToCanvas(sx: Float, sy: Float): FloatArray {
        return floatArrayOf((sx - offsetX) / scale, (sy - offsetY) / scale)
    }

    companion object {
        val ZOOM_LEVELS = floatArrayOf(1f, 2f, 4f, 8f, 16f, 32f, 64f)
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 64f
    }
}
