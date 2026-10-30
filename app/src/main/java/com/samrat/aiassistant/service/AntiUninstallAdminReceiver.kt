package com.samrat.aiassistant.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AntiUninstallAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "SAMRAT AI Protection Enabled: Device Admin Active", Toast.LENGTH_LONG).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "WARNING: SAMRAT AI is protected by Administrator Policy. Deactivation requires SAMRAT's authorization."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "SAMRAT AI Admin Protection Deactivated", Toast.LENGTH_SHORT).show()
    }
}
