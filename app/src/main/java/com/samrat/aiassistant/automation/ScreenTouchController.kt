package com.samrat.aiassistant.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class ScreenTouchController(private val service: AccessibilityService) {

    private val tag = "ScreenTouchController"
    private var isContinuousSwipingActive = false
    private var swipeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    /**
     * Perform a single tap at (x, y) coordinates
     */
    fun performTap(x: Float, y: Float, onComplete: (() -> Unit)? = null) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()

        service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(tag, "Tap executed at ($x, $y)")
                onComplete?.invoke()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(tag, "Tap cancelled at ($x, $y)")
            }
        }, null)
    }

    /**
     * Perform multi-tap (e.g. double tap, triple tap) at (x, y)
     */
    fun performMultiTap(x: Float, y: Float, count: Int, intervalMs: Long = 120) {
        scope.launch {
            for (i in 1..count) {
                performTap(x, y)
                delay(intervalMs)
            }
        }
    }

    /**
     * Perform a directional swipe from (startX, startY) to (endX, endY)
     */
    fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300,
        onComplete: (() -> Unit)? = null
    ) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()

        service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(tag, "Swipe completed from ($startX, $startY) to ($endX, $endY)")
                onComplete?.invoke()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(tag, "Swipe cancelled")
            }
        }, null)
    }

    /**
     * Continuous swiping loop:
     * Keeps scrolling/swiping down/up until the requested text is found on screen or stopped.
     */
    fun startContinuousSwipingSearch(
        targetQuery: String,
        isScrollDown: Boolean = true,
        screenWidth: Float,
        screenHeight: Float,
        onTargetFound: (AccessibilityNodeInfo) -> Unit,
        onTimeout: () -> Unit
    ) {
        stopContinuousSwiping()
        isContinuousSwipingActive = true

        val startX = screenWidth / 2f
        val startY = if (isScrollDown) screenHeight * 0.75f else screenHeight * 0.25f
        val endY = if (isScrollDown) screenHeight * 0.25f else screenHeight * 0.75f

        swipeJob = scope.launch {
            var attempts = 0
            val maxAttempts = 25 // Avoid infinite loops

            while (isActive && isContinuousSwipingActive && attempts < maxAttempts) {
                // Check if target text exists in current view hierarchy
                val rootNode = service.rootInActiveWindow
                val matchingNodes = rootNode?.findAccessibilityNodeInfosByText(targetQuery)

                if (!matchingNodes.isNullOrEmpty()) {
                    val targetNode = matchingNodes[0]
                    Log.d(tag, "Target found: ${targetNode.text}")
                    isContinuousSwipingActive = false
                    onTargetFound(targetNode)
                    return@launch
                }

                // If not found, swipe once
                performSwipe(startX, startY, startX, endY, 350)
                attempts++
                delay(1200) // Allow UI to render after scroll
            }

            if (isContinuousSwipingActive) {
                isContinuousSwipingActive = false
                onTimeout()
            }
        }
    }

    fun stopContinuousSwiping() {
        isContinuousSwipingActive = false
        swipeJob?.cancel()
    }
}
