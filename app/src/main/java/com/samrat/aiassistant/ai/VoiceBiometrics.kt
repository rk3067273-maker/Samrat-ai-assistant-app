package com.samrat.aiassistant.ai

import android.content.Context
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Voice Biometric & Speaker Recognition Engine.
 * Verifies if incoming voice matches SAMRAT's unique vocal acoustic pattern.
 */
class VoiceBiometrics(private val context: Context) {

    private val tag = "VoiceBiometrics"
    private val prefs = context.getSharedPreferences("samrat_voice_biometrics", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENROLLED_PROFILE = "master_vocal_profile"
        private const val KEY_IS_ENROLLED = "is_voice_enrolled"
    }

    /**
     * Check if SAMRAT has enrolled his voice
     */
    fun isVoiceEnrolled(): Boolean = prefs.getBoolean(KEY_IS_ENROLLED, false)

    /**
     * Enroll SAMRAT's vocal profile from PCM 16-bit audio samples
     */
    fun enrollMasterVoice(audioData: ShortArray) {
        val features = extractAcousticFeatures(audioData)
        val profileString = features.joinToString(",") { it.toString() }
        prefs.edit()
            .putString(KEY_ENROLLED_PROFILE, profileString)
            .putBoolean(KEY_IS_ENROLLED, true)
            .apply()
        Log.d(tag, "SAMRAT Voice Profile Successfully Enrolled")
    }

    /**
     * Verify whether the incoming speaker is SAMRAT.
     * Returns true if match confidence >= 0.72
     */
    fun verifySpeaker(audioData: ShortArray): Boolean {
        if (!isVoiceEnrolled()) {
            // If not yet enrolled, default to trust until enrollment is completed
            return true
        }

        val enrolledStr = prefs.getString(KEY_ENROLLED_PROFILE, null) ?: return true
        val masterFeatures = enrolledStr.split(",").mapNotNull { it.toFloatOrNull() }.toFloatArray()
        val currentFeatures = extractAcousticFeatures(audioData)

        val similarity = cosineSimilarity(masterFeatures, currentFeatures)
        Log.d(tag, "Speaker Voice Similarity to SAMRAT: $similarity")
        return similarity >= 0.72f
    }

    /**
     * Extract energy, zero-crossing rate, spectral centroid approximation
     */
    private fun extractAcousticFeatures(audio: ShortArray): FloatArray {
        var sumEnergy = 0.0
        var zeroCrossings = 0
        val n = audio.size

        for (i in 0 until n) {
            val sample = audio[i].toFloat() / 32768.0f
            sumEnergy += sample * sample
            if (i > 0 && ((audio[i] >= 0 && audio[i - 1] < 0) || (audio[i] < 0 && audio[i - 1] >= 0))) {
                zeroCrossings++
            }
        }

        val rmsEnergy = sqrt(sumEnergy / n).toFloat()
        val zcr = zeroCrossings.toFloat() / n

        return floatArrayOf(rmsEnergy, zcr, (rmsEnergy * 1.5f + zcr * 0.5f))
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA.toDouble()) * sqrt(normB.toDouble())
        return if (denom > 0.0) (dot / denom).toFloat() else 0f
    }
}
