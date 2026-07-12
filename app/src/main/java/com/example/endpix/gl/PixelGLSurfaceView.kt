package com.example.endpix.gl

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import kotlin.math.hypot

interface StrokeListener {
    fun onStrokeDown(canvasX: Float, canvasY: Float)
    fun onStrokeMove(canvasX: Float, canvasY: Float)
    fun onStrokeUp(canvasX: Float, canvasY: Float)
    fun onStrokeCancel()
}

class PixelGLSurfaceView(
    context: Context,
    private val renderer: PixelRenderer
) : GLSurfaceView(context) {

    var listener: StrokeListener? = null
    var panMode: Boolean = false

    private var mode = IDLE
    private var activePointerId = -1

    private var lastPanX = 0f
    private var lastPanY = 0f

    private var pinchDist = 0f
    private var pinchMidX = 0f
    private var pinchMidY = 0f

    init {
        setEGLContextClientVersion(3)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    private fun toCanvas(x: Float, y: Float): FloatArray = renderer.screenToCanvas(x, y)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                if (panMode) {
                    mode = PAN
                    lastPanX = x
                    lastPanY = y
                } else {
                    activePointerId = event.getPointerId(0)
                    mode = STROKE
                    val c = toCanvas(x, y)
                    listener?.onStrokeDown(c[0], c[1])
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    if (mode == STROKE) {
                        listener?.onStrokeCancel()
                    }
                    mode = PINCH
                    val (d, mx, my) = pinchParams(event)
                    pinchDist = d
                    pinchMidX = mx
                    pinchMidY = my
                }
            }
            MotionEvent.ACTION_MOVE -> {
                when (mode) {
                    STROKE -> {
                        if (activePointerId != -1) {
                            val idx = event.findPointerIndex(activePointerId)
                            if (idx >= 0) {
                                val c = toCanvas(event.getX(idx), event.getY(idx))
                                listener?.onStrokeMove(c[0], c[1])
                            }
                        }
                    }
                    PAN -> {
                        val dx = event.x - lastPanX
                        val dy = event.y - lastPanY
                        renderer.setTransform(
                            renderer.scale,
                            renderer.offsetX + dx,
                            renderer.offsetY + dy
                        )
                        lastPanX = event.x
                        lastPanY = event.y
                        requestRender()
                    }
                    PINCH -> {
                        if (event.pointerCount >= 2) {
                            val (d, mx, my) = pinchParams(event)
                            if (pinchDist > 0f) {
                                val factor = d / pinchDist
                                val newScale = (renderer.scale * factor)
                                    .coerceIn(PixelRenderer.MIN_ZOOM, PixelRenderer.MAX_ZOOM)
                                val cx = (pinchMidX - renderer.offsetX) / renderer.scale
                                val cy = (pinchMidY - renderer.offsetY) / renderer.scale
                                val newOffsetX = mx - cx * newScale
                                val newOffsetY = my - cy * newScale
                                renderer.setTransform(newScale, newOffsetX, newOffsetY)
                                renderer.notifyScale()
                                requestRender()
                            }
                            pinchDist = d
                            pinchMidX = mx
                            pinchMidY = my
                        }
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (mode == PINCH) {
                    mode = IDLE
                } else if (mode == STROKE) {
                    listener?.onStrokeUp(0f, 0f)
                    mode = IDLE
                }
                activePointerId = -1
            }
            MotionEvent.ACTION_UP -> {
                when (mode) {
                    STROKE -> {
                        val c = toCanvas(event.x, event.y)
                        listener?.onStrokeUp(c[0], c[1])
                    }
                    PAN -> requestRender()
                    else -> {}
                }
                mode = IDLE
                activePointerId = -1
            }
            MotionEvent.ACTION_CANCEL -> {
                if (mode == STROKE) listener?.onStrokeCancel()
                mode = IDLE
                activePointerId = -1
            }
        }
        return true
    }

    private fun pinchParams(event: MotionEvent): Triple<Float, Float, Float> {
        val x0 = event.getX(0)
        val y0 = event.getY(0)
        val x1 = event.getX(1)
        val y1 = event.getY(1)
        val dist = hypot(x1 - x0, y1 - y0)
        val mx = (x0 + x1) / 2f
        val my = (y0 + y1) / 2f
        return Triple(dist, mx, my)
    }

    companion object {
        private const val IDLE = 0
        private const val STROKE = 1
        private const val PAN = 2
        private const val PINCH = 3
    }
}
