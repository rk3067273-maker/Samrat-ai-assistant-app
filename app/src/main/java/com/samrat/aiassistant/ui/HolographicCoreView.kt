package com.samrat.aiassistant.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/**
 * Ultra-Futuristic Holographic Core View (JARVIS / Sci-Fi HUD Reactor).
 * Renders an animated 3D neural particle lattice connected with glowing cyber lines,
 * surrounded by counter-rotating concentric HUD target rings, reticle ticks, and data arcs.
 */
class HolographicCoreView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val nodeCount = 65
    private val nodes3D = ArrayList<Node3D>()

    private var rotX = 0f
    private var rotY = 0f
    private var ringAngle1 = 0f
    private var ringAngle2 = 0f

    var isCoreActive: Boolean = false
        set(value) {
            field = value
            postInvalidateOnAnimation()
        }

    var audioLevel: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            postInvalidateOnAnimation()
        }

    // Touch interaction for 3D gyro rotation
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // Paint brushes
    private val paintNode = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }

    private val paintRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }

    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }

    data class Node3D(var x: Float, var y: Float, var z: Float, val baseColor: Int)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        initNeuralMesh()
    }

    private fun initNeuralMesh() {
        nodes3D.clear()
        val phi = Math.PI * (3.0 - sqrt(5.0))
        for (i in 0 until nodeCount) {
            val y = 1f - (i / (nodeCount - 1f)) * 2f
            val radiusAtY = sqrt(1f - y * y)
            val theta = phi * i

            val x = (cos(theta) * radiusAtY).toFloat()
            val z = (sin(theta) * radiusAtY).toFloat()

            val color = if (i % 3 == 0) Color.parseColor("#00F5FF") else Color.parseColor("#0077FE")
            nodes3D.add(Node3D(x, y, z, color))
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                rotY += dx * 0.008f
                rotX -= dy * 0.008f
                lastTouchX = event.x
                lastTouchY = event.y
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = (min(width, height) / 2f) * 0.88f
        val coreRadius = maxRadius * 0.62f * (1f + (audioLevel * 0.25f))

        // Auto rotate
        rotX += 0.009f
        rotY += 0.013f
        ringAngle1 += 0.8f
        ringAngle2 -= 1.1f

        val cosX = cos(rotX)
        val sinX = sin(rotX)
        val cosY = cos(rotY)
        val sinY = sin(rotY)

        // 1. Draw Outer Concentric Futuristic HUD Reticle Rings
        drawHudRings(canvas, cx, cy, maxRadius)

        // 2. Project 3D Nodes
        val projectedX = FloatArray(nodeCount)
        val projectedY = FloatArray(nodeCount)
        val projectedZ = FloatArray(nodeCount)
        val scaleArray = FloatArray(nodeCount)

        for (i in 0 until nodeCount) {
            val node = nodes3D[i]
            // Rotate around X then Y
            val y1 = node.y * cosX - node.z * sinX
            val z1 = node.y * sinX + node.z * cosX

            val x2 = node.x * cosY + z1 * sinY
            val z2 = -node.x * sinY + z1 * cosY

            val fov = 3.2f
            val scale = fov / (fov + z2)
            projectedX[i] = cx + x2 * coreRadius * scale
            projectedY[i] = cy + y1 * coreRadius * scale
            projectedZ[i] = z2
            scaleArray[i] = scale
        }

        // 3. Connect close nodes with glowing neural lines (Network Lattice)
        val maxDistSq = (coreRadius * 0.72f) * (coreRadius * 0.72f)
        for (i in 0 until nodeCount) {
            for (j in i + 1 until nodeCount) {
                val dx = projectedX[i] - projectedX[j]
                val dy = projectedY[i] - projectedY[j]
                val distSq = dx * dx + dy * dy

                if (distSq < maxDistSq) {
                    val proximityAlpha = ((1f - (distSq / maxDistSq)) * 140).toInt().coerceIn(0, 160)
                    paintLine.color = if (isCoreActive) Color.parseColor("#00F5FF") else Color.parseColor("#4A6B82")
                    paintLine.alpha = proximityAlpha
                    canvas.drawLine(projectedX[i], projectedY[i], projectedX[j], projectedY[j], paintLine)
                }
            }
        }

        // 4. Render Nodes
        for (i in 0 until nodeCount) {
            val z = projectedZ[i]
            val alpha = (((z + 1f) / 2f) * 190 + 65).toInt().coerceIn(40, 255)
            val size = (scaleArray[i] * 3.4f * (1f + audioLevel * 0.5f)).coerceAtLeast(1.8f)

            val color = if (isCoreActive) nodes3D[i].baseColor else Color.parseColor("#60A5FA")
            paintNode.color = color
            paintNode.alpha = alpha

            canvas.drawCircle(projectedX[i], projectedY[i], size, paintNode)
        }

        // Keep animating smoothly at 60 FPS
        postInvalidateOnAnimation()
    }

    private fun drawHudRings(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val ringColor = if (isCoreActive) Color.parseColor("#00F5FF") else Color.parseColor("#1E3A5F")
        paintRing.color = ringColor

        // Outer Ring 1 with Dash Arcs
        paintRing.strokeWidth = 1.8f
        val rect1 = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rect1, ringAngle1, 90f, false, paintRing)
        canvas.drawArc(rect1, ringAngle1 + 140f, 60f, false, paintRing)
        canvas.drawArc(rect1, ringAngle1 + 240f, 80f, false, paintRing)

        // Middle Ring 2 with Counter-Rotation
        val radius2 = radius * 0.82f
        val rect2 = RectF(cx - radius2, cy - radius2, cx + radius2, cy + radius2)
        paintRing.strokeWidth = 1.2f
        canvas.drawArc(rect2, ringAngle2, 45f, false, paintRing)
        canvas.drawArc(rect2, ringAngle2 + 90f, 120f, false, paintRing)
        canvas.drawArc(rect2, ringAngle2 + 250f, 70f, false, paintRing)

        // Reticle Degree Ticks
        for (angle in 0 until 360 step 30) {
            val rad = Math.toRadians((angle + ringAngle1).toDouble())
            val startX = (cx + (radius * 0.95f) * cos(rad)).toFloat()
            val startY = (cy + (radius * 0.95f) * sin(rad)).toFloat()
            val endX = (cx + radius * cos(rad)).toFloat()
            val endY = (cy + radius * sin(rad)).toFloat()
            canvas.drawLine(startX, startY, endX, endY, paintRing)
        }
    }
}
