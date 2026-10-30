package com.samrat.aiassistant.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationMonitorService : NotificationListenerService() {

    private val tag = "NotificationMonitor"

    companion object {
        var onNotificationReceived: ((title: String, text: String, pkg: String) -> Unit)? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val extras = sbn.notification.extras ?: return
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val packageName = sbn.packageName ?: ""

        if (title.isNotBlank() && text.isNotBlank()) {
            Log.d(tag, "Notification from $packageName | $title: $text")
            onNotificationReceived?.invoke(title, text, packageName)
        }
    }
}
