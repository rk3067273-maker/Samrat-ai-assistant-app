package com.samrat.aiassistant.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Procedural Sci-Fi HUD Audio Synthesizer.
 * Generates high-tech chimes and telemetry audio pulses programmatically.
 */
object SciFiSoundEffects {

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Futuristic Core Activation Chime (Ascending Quantum Resonance)
     */
    fun playActivationSound() {
        scope.launch {
            val sampleRate = 44100
            val durationMs = 380
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val sample = ShortArray(numSamples)

            val frequencies = floatArrayOf(440f, 660f, 880f, 1320f)
            val stepSamples = numSamples / frequencies.size

            for (i in 0 until numSamples) {
                val stepIdx = (i / stepSamples).coerceIn(0, frequencies.size - 1)
                val freq = frequencies[stepIdx]
                val t = i.toDouble() / sampleRate
                val env = 1.0 - (i.toDouble() / numSamples) // Fade out
                val wave = sin(2 * PI * freq * t) * env
                sample[i] = (wave * 30000).toInt().toShort()
            }

            playPcm(sample, sampleRate)
        }
    }

    /**
     * Deactivation Chime (Descending Power Down)
     */
    fun playDeactivationSound() {
        scope.launch {
            val sampleRate = 44100
            val durationMs = 280
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val sample = ShortArray(numSamples)

            val frequencies = floatArrayOf(1200f, 800f, 400f, 200f)
            val stepSamples = numSamples / frequencies.size

            for (i in 0 until numSamples) {
                val stepIdx = (i / stepSamples).coerceIn(0, frequencies.size - 1)
                val freq = frequencies[stepIdx]
                val t = i.toDouble() / sampleRate
                val env = 1.0 - (i.toDouble() / numSamples)
                val wave = sin(2 * PI * freq * t) * env
                sample[i] = (wave * 26000).toInt().toShort()
            }

            playPcm(sample, sampleRate)
        }
    }

    /**
     * Telemetry Click / Touch Beep
     */
    fun playClickSound() {
        scope.launch {
            val sampleRate = 44100
            val durationMs = 45
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val sample = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = 1.0 - (i.toDouble() / numSamples)
                val wave = sin(2 * PI * 1800.0 * t) * env
                sample[i] = (wave * 18000).toInt().toShort()
            }

            playPcm(sample, sampleRate)
        }
    }

    private fun playPcm(pcm: ShortArray, sampleRate: Int) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
        } catch (_: Exception) {}
    }
}
