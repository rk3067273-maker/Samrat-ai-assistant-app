package com.samrat.aiassistant.core

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.gson.JsonObject
import com.samrat.aiassistant.ai.GeminiLiveClient
import com.samrat.aiassistant.ai.LongTermMemory
import com.samrat.aiassistant.ai.VoiceBiometrics
import com.samrat.aiassistant.automation.SystemActionController
import com.samrat.aiassistant.service.AssistantAccessibilityService
import com.samrat.aiassistant.service.ScreenCaptureService
import com.samrat.aiassistant.ui.FloatingSearchPopup
import com.samrat.aiassistant.vision.CameraVisionHelper
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Central Orchestrator for SAMRAT AI Assistant.
 * Routes voice commands, executes tools, handles permissions, and manages UI states.
 */
class AssistantManager(private val context: Context) {

    private val tag = "AssistantManager"

    val memory = LongTermMemory(context)
    val biometrics = VoiceBiometrics(context)
    val systemController = SystemActionController(context)
    val searchPopup = FloatingSearchPopup(context)

    var geminiClient: GeminiLiveClient? = null
        private set

    // Pending permission callback for SAMRAT
    private var pendingPermissionAction: (() -> Unit)? = null

    fun initialize(apiKey: String, assistantName: String, customBehavior: String = "") {
        geminiClient = GeminiLiveClient(apiKey, assistantName, customBehavior).apply {
            onTranscriptReceived = { transcript ->
                Log.d(tag, "AI Transcript: $transcript")
                searchPopup.updateContent(content = transcript)
            }

            onToolCallReceived = { fnName, args ->
                handleToolCall(fnName, args)
            }

            connect()
        }

        searchPopup.onUserQuerySubmitted = { query ->
            // Send user typed query as text
            Log.d(tag, "User queried via popup: $query")
        }
    }

    /**
     * Handle autonomous tool execution requests from the AI
     */
    private fun handleToolCall(functionName: String, args: JsonObject) {
        Log.d(tag, "Executing Tool: $functionName with args: $args")

        when (functionName) {
            "tap_screen" -> {
                val x = args.get("x")?.asFloat ?: 0f
                val y = args.get("y")?.asFloat ?: 0f
                AssistantAccessibilityService.instance?.touchController?.performTap(x, y)
            }

            "multi_tap" -> {
                val x = args.get("x")?.asFloat ?: 0f
                val y = args.get("y")?.asFloat ?: 0f
                val count = args.get("count")?.asInt ?: 2
                AssistantAccessibilityService.instance?.touchController?.performMultiTap(x, y, count)
            }

            "continuous_swiping" -> {
                val query = args.get("target_text")?.asString ?: ""
                val isDown = args.get("scroll_down")?.asBoolean ?: true
                val displayMetrics = context.resources.displayMetrics
                AssistantAccessibilityService.instance?.touchController?.startContinuousSwipingSearch(
                    targetQuery = query,
                    isScrollDown = isDown,
                    screenWidth = displayMetrics.widthPixels.toFloat(),
                    screenHeight = displayMetrics.heightPixels.toFloat(),
                    onTargetFound = { node ->
                        Log.d(tag, "Target found, clicking node")
                        node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                    },
                    onTimeout = {
                        searchPopup.updateContent("Target Not Found", "Continuous swiping finished without finding: $query")
                    }
                )
            }

            "toggle_flashlight" -> {
                val enable = args.get("enable")?.asBoolean ?: false
                systemController.toggleFlashlight(enable)
            }

            "make_call" -> {
                val number = args.get("phone_number")?.asString ?: ""
                requirePermission("Make phone call to $number") {
                    systemController.makePhoneCall(number)
                }
            }

            "send_whatsapp" -> {
                val number = args.get("phone_number")?.asString ?: ""
                val message = args.get("message")?.asString ?: ""
                requirePermission("Send WhatsApp message to $number: \"$message\"") {
                    systemController.sendWhatsAppMessage(number, message)
                }
            }

            "delete_storage_file" -> {
                val filePath = args.get("file_path")?.asString ?: ""
                val file = File(filePath)
                requirePermission("Permanently delete file at: $filePath") {
                    systemController.deleteFile(file)
                }
            }

            "deep_search" -> {
                val query = args.get("query")?.asString ?: ""
                searchPopup.show("DEEP SEARCH: $query", "Gathering latest intelligence and analyzing...")
            }

            "analyze_screen" -> {
                val bitmap = ScreenCaptureService.instance?.captureCurrentScreen()
                if (bitmap != null) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                    geminiClient?.sendVisionFrame(stream.toByteArray())
                }
            }
        }
    }

    /**
     * Ask SAMRAT for permission before executing sensitive or destructive actions
     */
    fun requirePermission(description: String, onApproved: () -> Unit) {
        pendingPermissionAction = onApproved
        val promptText = "SAMRAT, permission required to proceed:\n$description\n\nSay 'Confirm' or tap Confirm on the popup."
        searchPopup.show("PERMISSION REQUIRED", promptText)
    }

    fun grantPendingPermission() {
        pendingPermissionAction?.invoke()
        pendingPermissionAction = null
        searchPopup.updateContent("AUTHORIZED", "Action authorized by SAMRAT. Executed successfully.")
    }

    fun denyPendingPermission() {
        pendingPermissionAction = null
        searchPopup.updateContent("DENIED", "Action cancelled as requested by SAMRAT.")
    }
}
