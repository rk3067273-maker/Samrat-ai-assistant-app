package com.samrat.aiassistant.ai

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.samrat.aiassistant.core.MasterSystemPrompt
import okhttp3.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Ultra-low latency Gemini Multimodal Live API Client.
 * Bidirectional streaming over WebSocket for neural voice & real-time tool executions.
 */
class GeminiLiveClient(
    private val apiKey: String,
    private val assistantName: String = "JARVIS",
    private val customBehavior: String = ""
) {

    private val tag = "GeminiLiveClient"
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    // Audio Output Playback (16-bit PCM 24kHz output from Gemini Live)
    private var audioTrack: AudioTrack? = null

    // Listeners
    var onAmplitudeUpdated: ((Float) -> Unit)? = null
    var onTranscriptReceived: ((String) -> Unit)? = null
    var onToolCallReceived: ((functionName: String, args: JsonObject) -> Unit)? = null
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        val sampleRate = 24000
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
    }

    fun connect() {
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "Connected to Gemini Multimodal Live API")
                onConnectionStateChanged?.invoke(true)
                sendInitialSetup()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(tag, "WebSocket connection failed: ${t.message}", t)
                onConnectionStateChanged?.invoke(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(tag, "WebSocket connection closed: $reason")
                onConnectionStateChanged?.invoke(false)
            }
        })
    }

    private fun sendInitialSetup() {
        val setupJson = JsonObject().apply {
            val setup = JsonObject().apply {
                addProperty("model", "models/gemini-2.0-flash-exp")
                val generationConfig = JsonObject().apply {
                    addProperty("responseModalities", "AUDIO")
                    val speechConfig = JsonObject().apply {
                        val voiceConfig = JsonObject().apply {
                            val prebuiltVoiceConfig = JsonObject().apply {
                                addProperty("voiceName", "Aoede") // Warm & Natural neural voice
                            }
                            add("prebuiltVoiceConfig", prebuiltVoiceConfig)
                        }
                        add("voiceConfig", voiceConfig)
                    }
                    add("speechConfig", speechConfig)
                }
                add("generationConfig", generationConfig)

                // System Instruction with SAMRAT's master prompt
                val systemInstruction = JsonObject().apply {
                    val parts = JsonObject().apply {
                        addProperty("text", MasterSystemPrompt.buildPrompt(assistantName, customBehavior))
                    }
                    val partsArray = com.google.gson.JsonArray().apply { add(parts) }
                    add("parts", partsArray)
                }
                add("systemInstruction", systemInstruction)
            }
            add("setup", setup)
        }

        webSocket?.send(setupJson.toString())
        Log.d(tag, "Gemini Live Setup Configuration Dispatched")
    }

    /**
     * Send Real-Time Mic PCM Audio (16kHz, 16-bit Mono)
     */
    fun sendAudioChunk(pcmData: ByteArray) {
        // Calculate audio amplitude for UI visualizer
        val amplitude = computeAmplitude(pcmData)
        onAmplitudeUpdated?.invoke(amplitude)

        val base64Audio = Base64.encodeToString(pcmData, Base64.NO_WRAP)
        val realtimeInput = JsonObject().apply {
            val input = JsonObject().apply {
                val mediaChunks = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("mimeType", "audio/pcm;rate=16000")
                        addProperty("data", base64Audio)
                    })
                }
                add("mediaChunks", mediaChunks)
            }
            add("realtimeInput", input)
        }
        webSocket?.send(realtimeInput.toString())
    }

    /**
     * Send Image / Screen Frame to Gemini Live
     */
    fun sendVisionFrame(jpegBytes: ByteArray) {
        val base64Image = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        val realtimeInput = JsonObject().apply {
            val input = JsonObject().apply {
                val mediaChunks = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("mimeType", "image/jpeg")
                        addProperty("data", base64Image)
                    })
                }
                add("mediaChunks", mediaChunks)
            }
            add("realtimeInput", input)
        }
        webSocket?.send(realtimeInput.toString())
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val root = JsonParser.parseString(text).asJsonObject
            val serverContent = root.getAsJsonObject("serverContent") ?: return
            val modelTurn = serverContent.getAsJsonObject("modelTurn")

            if (modelTurn != null) {
                val parts = modelTurn.getAsJsonArray("parts") ?: return
                for (part in parts) {
                    val partObj = part.asJsonObject
                    // Check for Audio chunk
                    val inlineData = partObj.getAsJsonObject("inlineData")
                    if (inlineData != null) {
                        val mimeType = inlineData.get("mimeType")?.asString ?: ""
                        if (mimeType.startsWith("audio/")) {
                            val audioBase64 = inlineData.get("data").asString
                            val pcmBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                            playAudioChunk(pcmBytes)
                        }
                    }

                    // Check for Text transcript
                    if (partObj.has("text")) {
                        val transcript = partObj.get("text").asString
                        onTranscriptReceived?.invoke(transcript)
                    }

                    // Check for Tool / Function call
                    if (partObj.has("functionCall")) {
                        val fn = partObj.getAsJsonObject("functionCall")
                        val name = fn.get("name").asString
                        val args = fn.getAsJsonObject("args")
                        onToolCallReceived?.invoke(name, args)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing server message", e)
        }
    }

    private fun playAudioChunk(pcmBytes: ByteArray) {
        val amplitude = computeAmplitude(pcmBytes)
        onAmplitudeUpdated?.invoke(amplitude)
        audioTrack?.write(pcmBytes, 0, pcmBytes.size)
    }

    private fun computeAmplitude(bytes: ByteArray): Float {
        var sum = 0.0
        val shorts = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        for (s in shorts) {
            sum += abs(s.toInt())
        }
        val avg = sum / (shorts.size.coerceAtLeast(1))
        return (avg / 32768.0).toFloat().coerceIn(0f, 1f)
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        audioTrack?.stop()
        audioTrack?.release()
    }
}
