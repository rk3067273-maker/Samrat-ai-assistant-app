package com.samrat.aiassistant.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/**
 * 3D Neon Dot Bubble Visualizer.
 * Renders a rotating 3D sphere of neon particles that reacts dynamically
 * to neural voice amplitudes and creates a ripple explosion effect.
 */
class NeonBubbleVisualizer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val pointCount = 120
    private val points3D = ArrayList<Point3D>()
    private var rotationAngleX = 0f
    private var rotationAngleY = 0f

    // Dynamic Audio Amplitude (0.0 to 1.0)
    var audioAmplitude: Float = 0.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            if (field > 0.4f) {
                triggerExplosionPulse(field)
            }
        }

    private var explosionRadius = 0f
    private var isExploding = false

    private val paintCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
    }

    private val paintShockwave = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#00F5FF")
    }

    private val paintSynapse = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.0f
        color = Color.parseColor("#00F5FF")
    }

    private val projX = FloatArray(pointCount)
    private val projY = FloatArray(pointCount)
    private val projZ = FloatArray(pointCount)
    private val projScale = FloatArray(pointCount)

    data class Point3D(var x: Float, var y: Float, var z: Float, val color: Int)

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Required for BlurMaskFilter
        initializeSphere()
    }

    private fun initializeSphere() {
        points3D.clear()
        val colors = intArrayOf(
            Color.parseColor("#00F5FF"), // Neon Cyan
            Color.parseColor("#0077FE"), // Neon Blue
            Color.parseColor("#A855F7"), // Neon Purple
            Color.parseColor("#EC4899")  // Neon Pink
        )

        // Generate Fibonacci Sphere for uniform distribution
        val phi = Math.PI * (3.0 - sqrt(5.0))
        for (i in 0 until pointCount) {
            val y = 1f - (i / (pointCount - 1f)) * 2f
            val radiusAtY = sqrt(1f - y * y)
            val theta = phi * i

            val x = (cos(theta) * radiusAtY).toFloat()
            val z = (sin(theta) * radiusAtY).toFloat()
            val col = colors[Random.nextInt(colors.size)]
            points3D.add(Point3D(x, y, z, col))
        }
    }

    fun triggerExplosionPulse(intensity: Float) {
        isExploding = true
        explosionRadius = 10f * intensity
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = (min(width, height) / 2f) * 0.72f
        val dynamicRadius = baseRadius * (1f + (audioAmplitude * 0.45f))

        // Auto increment rotation angles
        rotationAngleX += 0.020f
        rotationAngleY += 0.028f

        val cosX = cos(rotationAngleX)
        val sinX = sin(rotationAngleX)
        val cosY = cos(rotationAngleY)
        val sinY = sin(rotationAngleY)

        // Draw Shockwave / Explosion Ring if active
        if (isExploding) {
            paintShockwave.alpha = ((1f - (explosionRadius / (baseRadius * 1.5f))) * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(centerX, centerY, baseRadius + explosionRadius, paintShockwave)
            explosionRadius += 3.5f
            if (explosionRadius > baseRadius * 1.5f) {
                isExploding = false
                explosionRadius = 0f
            }
        }

        // Project 3D points
        for (i in 0 until pointCount) {
            val point = points3D[i]
            val y1 = point.y * cosX - point.z * sinX
            val z1 = point.y * sinX + point.z * cosX

            val x2 = point.x * cosY + z1 * sinY
            val z2 = -point.x * sinY + z1 * cosY

            val fov = 3.5f
            val scale = fov / (fov + z2)
            projX[i] = centerX + x2 * dynamicRadius * scale
            projY[i] = centerY + y1 * dynamicRadius * scale
            projZ[i] = z2
            projScale[i] = scale
        }

        // Draw connecting neural lines
        val maxDistSq = (dynamicRadius * 0.48f) * (dynamicRadius * 0.48f)
        for (i in 0 until pointCount step 2) {
            for (j in i + 1 until pointCount step 2) {
                val dx = projX[i] - projX[j]
                val dy = projY[i] - projY[j]
                val distSq = dx * dx + dy * dy
                if (distSq < maxDistSq) {
                    val lineAlpha = ((1f - (distSq / maxDistSq)) * 120).toInt().coerceIn(0, 140)
                    paintSynapse.alpha = lineAlpha
                    canvas.drawLine(projX[i], projY[i], projX[j], projY[j], paintSynapse)
                }
            }
        }

        // Draw Nodes
        for (i in 0 until pointCount) {
            val z2 = projZ[i]
            val depthAlpha = (((z2 + 1f) / 2f) * 190 + 65).toInt().coerceIn(50, 255)
            val pointSize = ((z2 + 1.2f) * 2.8f * (1f + audioAmplitude * 0.8f)).coerceAtLeast(1.8f)

            paintCore.color = points3D[i].color
            paintCore.alpha = depthAlpha
            paintGlow.color = points3D[i].color
            paintGlow.alpha = (depthAlpha * 0.7f).toInt()

            canvas.drawCircle(projX[i], projY[i], pointSize * 1.5f, paintGlow)
            canvas.drawCircle(projX[i], projY[i], pointSize, paintCore)
        }

        // Invalidate continuously for smooth 60 FPS rotation
        postInvalidateOnAnimation()
    }
}
