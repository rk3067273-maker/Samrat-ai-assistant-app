package com.samrat.aiassistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class App : Application() {

    companion object {
        const val CHANNEL_ID_OVERLAY = "samrat_ai_overlay_channel"
        const val CHANNEL_NAME_OVERLAY = "SAMRAT AI Background Monitor"
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_OVERLAY,
                CHANNEL_NAME_OVERLAY,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps SAMRAT AI overlay and accessibility services alive in background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
