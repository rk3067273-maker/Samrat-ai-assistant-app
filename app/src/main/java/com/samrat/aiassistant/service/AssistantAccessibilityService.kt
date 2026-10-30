package com.samrat.aiassistant.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.samrat.aiassistant.automation.ScreenTouchController

class AssistantAccessibilityService : AccessibilityService() {

    private val tag = "AssistantAccessibility"

    companion object {
        var instance: AssistantAccessibilityService? = null
            private set
    }

    var touchController: ScreenTouchController? = null
        private set

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        touchController = ScreenTouchController(this)
        Log.d(tag, "SAMRAT AI Accessibility Service Connected & Ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Can monitor foreground package changes, focused windows, etc.
    }

    override fun onInterrupt() {
        Log.w(tag, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        touchController = null
    }

    /**
     * Collect all visible text nodes from the active screen
     */
    fun extractActiveScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        collectText(root, sb)
        return sb.toString()
    }

    private fun collectText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return
        node.text?.let {
            if (it.isNotBlank()) sb.append(it).append("\n")
        }
        node.contentDescription?.let {
            if (it.isNotBlank()) sb.append(it).append("\n")
        }
        for (i in 0 until node.childCount) {
            collectText(node.getChild(i), sb)
        }
    }
}
