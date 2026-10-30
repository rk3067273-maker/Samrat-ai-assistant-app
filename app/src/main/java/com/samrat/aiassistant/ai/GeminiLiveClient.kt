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
    var onToolCallReceived: ((callId: String?, functionName: String, args: JsonObject) -> Unit)? = null
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

                // Autonomous Neural Tools declarations
                add("tools", buildToolsJson())
            }
            add("setup", setup)
        }

        webSocket?.send(setupJson.toString())
        Log.d(tag, "Gemini Live Setup Configuration Dispatched")
    }

    private fun buildToolsJson(): com.google.gson.JsonArray {
        val toolsArray = com.google.gson.JsonArray()
        val toolsObj = JsonObject()
        val funcDecls = com.google.gson.JsonArray()

        fun addFunc(name: String, desc: String, props: Map<String, Pair<String, String>>, required: List<String>) {
            val f = JsonObject().apply {
                addProperty("name", name)
                addProperty("description", desc)
                val params = JsonObject().apply {
                    addProperty("type", "OBJECT")
                    val properties = JsonObject().apply {
                        for ((propName, propDetails) in props) {
                            val (type, description) = propDetails
                            val p = JsonObject().apply {
                                addProperty("type", type)
                                addProperty("description", description)
                            }
                            add(propName, p)
                        }
                    }
                    add("properties", properties)
                    if (required.isNotEmpty()) {
                        val req = com.google.gson.JsonArray().apply {
                            required.forEach { add(it) }
                        }
                        add("required", req)
                    }
                }
                add("parameters", params)
            }
            funcDecls.add(f)
        }

        addFunc(
            "tap_screen",
            "Taps on the Android screen at specific x and y coordinates",
            mapOf("x" to Pair("NUMBER", "X coordinate in pixels"), "y" to Pair("NUMBER", "Y coordinate in pixels")),
            listOf("x", "y")
        )
        addFunc(
            "multi_tap",
            "Taps multiple times rapidly at specific x and y coordinates",
            mapOf("x" to Pair("NUMBER", "X coordinate"), "y" to Pair("NUMBER", "Y coordinate"), "count" to Pair("INTEGER", "Number of taps")),
            listOf("x", "y")
        )
        addFunc(
            "continuous_swiping",
            "Continuously scrolls the screen looking for specific target text",
            mapOf("target_text" to Pair("STRING", "Text label or element to find"), "scroll_down" to Pair("BOOLEAN", "True for scroll down, false for up")),
            listOf("target_text")
        )
        addFunc(
            "toggle_flashlight",
            "Turns the phone camera flashlight on or off",
            mapOf("enable" to Pair("BOOLEAN", "True to turn on, false to turn off")),
            listOf("enable")
        )
        addFunc(
            "make_call",
            "Initiates a phone call (strictly requires SAMRAT's verbal permission)",
            mapOf("phone_number" to Pair("STRING", "Phone number to call")),
            listOf("phone_number")
        )
        addFunc(
            "send_whatsapp",
            "Sends a WhatsApp message to a contact (strictly requires SAMRAT's verbal permission)",
            mapOf("phone_number" to Pair("STRING", "Phone number with country code"), "message" to Pair("STRING", "Message body")),
            listOf("phone_number", "message")
        )
        addFunc(
            "delete_storage_file",
            "Deletes a file on device storage (strictly requires SAMRAT's verbal permission)",
            mapOf("file_path" to Pair("STRING", "Full absolute file path")),
            listOf("file_path")
        )
        addFunc(
            "deep_search",
            "Researches deeply on a topic and shows distilled intelligence on the HUD popup",
            mapOf("query" to Pair("STRING", "The research topic or question")),
            listOf("query")
        )
        addFunc(
            "analyze_screen",
            "Captures the active phone screen and analyzes its content visually",
            emptyMap(),
            emptyList()
        )

        toolsObj.add("functionDeclarations", funcDecls)
        toolsArray.add(toolsObj)
        return toolsArray
    }

    fun sendToolResponse(callId: String?, functionName: String, output: JsonObject) {
        val toolResponseJson = JsonObject().apply {
            val toolResp = JsonObject().apply {
                val functionResponses = com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        if (!callId.isNullOrBlank()) {
                            addProperty("id", callId)
                        }
                        addProperty("name", functionName)
                        val response = JsonObject().apply {
                            add("output", output)
                        }
                        add("response", response)
                    })
                }
                add("functionResponses", functionResponses)
            }
            add("toolResponse", toolResp)
        }
        webSocket?.send(toolResponseJson.toString())
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
                        val callId = if (fn.has("id")) fn.get("id").asString else null
                        val args = if (fn.has("args")) fn.getAsJsonObject("args") else JsonObject()
                        onToolCallReceived?.invoke(callId, name, args)
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
