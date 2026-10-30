package com.samrat.aiassistant.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import androidx.core.app.NotificationCompat
import com.samrat.aiassistant.App
import com.samrat.aiassistant.R
import com.samrat.aiassistant.core.AssistantManager
import com.samrat.aiassistant.ui.NeonBubbleVisualizer
import kotlinx.coroutines.*
import kotlin.math.abs

class FloatingOverlayService : Service() {

    private val tag = "FloatingOverlayService"
    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var neonVisualizer: NeonBubbleVisualizer? = null
    private var params: WindowManager.LayoutParams? = null

    companion object {
        var assistantManager: AssistantManager? = null
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1001, buildNotification())
        createFloatingBubble()
        startAudioCapture()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingBubble() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(this)
        bubbleView = inflater.inflate(R.layout.overlay_bubble_layout, null)
        neonVisualizer = bubbleView?.findViewById(R.id.neonVisualizer)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        // Connect amplitude updates from Gemini Live audio output to neon visualizer
        assistantManager?.geminiClient?.onAmplitudeUpdated = { amp ->
            neonVisualizer?.audioAmplitude = amp
        }

        // Drag & Touch interaction
        bubbleView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = true

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params?.x ?: 0
                        initialY = params?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) isClick = false

                        params?.x = initialX + dx
                        params?.y = initialY + dy
                        windowManager?.updateViewLayout(bubbleView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            onBubbleClicked()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager?.addView(bubbleView, params)
    }

    private fun onBubbleClicked() {
        Log.d(tag, "SAMRAT AI Bubble Clicked - Toggling Search / Command Popup")
        neonVisualizer?.triggerExplosionPulse(1.0f)
        assistantManager?.searchPopup?.show("SAMRAT AI ASSISTANT", "Listening for your command...")
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        scope.launch {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                audioRecord?.startRecording()
                isRecording = true
                val buffer = ByteArray(1024)

                while (isActive && isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Forward live voice PCM stream to Gemini Live WebSocket
                        assistantManager?.geminiClient?.sendAudioChunk(buffer.copyOf(read))
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Audio record exception", e)
            }
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, App.CHANNEL_ID_OVERLAY)
            .setContentTitle("SAMRAT AI Active")
            .setContentText("Neural bubble floating and listening")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        if (bubbleView != null) {
            windowManager?.removeView(bubbleView)
            bubbleView = null
        }
    }
}
