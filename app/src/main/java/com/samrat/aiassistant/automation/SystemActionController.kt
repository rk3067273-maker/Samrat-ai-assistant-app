package com.samrat.aiassistant.automation

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File

class SystemActionController(private val context: Context) {

    private val tag = "SystemActionController"
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var isTorchOn = false

    /**
     * Toggle Flashlight / Torch
     */
    fun toggleFlashlight(enable: Boolean): Boolean {
        return try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, enable)
            isTorchOn = enable
            Log.d(tag, "Flashlight set to: $enable")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to toggle flashlight", e)
            false
        }
    }

    /**
     * Launch Direct Phone Call
     */
    fun makePhoneCall(phoneNumber: String) {
        try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to place call", e)
        }
    }

    /**
     * Send WhatsApp Message directly
     */
    fun sendWhatsAppMessage(phoneNumber: String, message: String) {
        try {
            val cleanNumber = phoneNumber.replace("+", "").replace(" ", "")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to open WhatsApp", e)
        }
    }

    /**
     * File Operations in Storage:
     * Search files matching a query
     */
    fun searchFiles(query: String): List<File> {
        val root = Environment.getExternalStorageDirectory()
        val results = mutableListOf<File>()
        searchRecursive(root, query.lowercase(), results, maxDepth = 4)
        return results
    }

    private fun searchRecursive(dir: File, query: String, results: MutableList<File>, maxDepth: Int) {
        if (maxDepth <= 0 || results.size >= 30) return
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.name.lowercase().contains(query)) {
                results.add(f)
            }
            if (f.isDirectory && !f.name.startsWith(".")) {
                searchRecursive(f, query, results, maxDepth - 1)
            }
        }
    }

    /**
     * Delete file - MUST require SAMRAT's permission before calling
     */
    fun deleteFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                val success = file.delete()
                Log.d(tag, "File deleted: ${file.absolutePath}, result: $success")
                success
            } else false
        } catch (e: Exception) {
            Log.e(tag, "File deletion failed", e)
            false
        }
    }

    /**
     * Copy or Move file
     */
    fun moveFile(source: File, destinationDir: File): Boolean {
        return try {
            if (!destinationDir.exists()) destinationDir.mkdirs()
            val destFile = File(destinationDir, source.name)
            val copied = source.copyTo(destFile, overwrite = true)
            if (copied.exists()) {
                source.delete()
                true
            } else false
        } catch (e: Exception) {
            Log.e(tag, "Move file failed", e)
            false
        }
    }
}
