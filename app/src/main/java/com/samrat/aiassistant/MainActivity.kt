package com.samrat.aiassistant

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.samrat.aiassistant.core.AssistantManager
import com.samrat.aiassistant.databinding.ActivityMainBinding
import com.samrat.aiassistant.service.AntiUninstallAdminReceiver
import com.samrat.aiassistant.service.FloatingOverlayService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var assistantManager: AssistantManager
    private val prefs by lazy { getSharedPreferences("samrat_ai_prefs", Context.MODE_PRIVATE) }

    private var isAssistantRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        assistantManager = AssistantManager(this)
        FloatingOverlayService.assistantManager = assistantManager

        loadSavedPreferences()
        setupListeners()
        requestCorePermissions()
    }

    private fun loadSavedPreferences() {
        val savedKey = prefs.getString("gemini_api_key", "")
        val savedName = prefs.getString("assistant_name", "JARVIS")
        val savedBehavior = prefs.getString("assistant_behavior", "")
        binding.etApiKey.setText(savedKey)
        binding.etAssistantName.setText(savedName)
        binding.etAssistantBehavior.setText(savedBehavior)
    }

    private fun savePreferences() {
        val apiKey = binding.etApiKey.text?.toString()?.trim() ?: ""
        val assistantName = binding.etAssistantName.text?.toString()?.trim() ?: "JARVIS"
        val behavior = binding.etAssistantBehavior.text?.toString()?.trim() ?: ""
        prefs.edit()
            .putString("gemini_api_key", apiKey)
            .putString("assistant_name", assistantName)
            .putString("assistant_behavior", behavior)
            .apply()
    }

    private fun setupListeners() {
        // 1. Overlay Permission
        binding.btnOverlayPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "Floating Overlay Permission is already Granted!", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. Accessibility Permission
        binding.btnAccessibilityPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable 'SAMRAT AI' in Accessibility settings", Toast.LENGTH_LONG).show()
        }

        // 3. Device Admin (Anti-Uninstall)
        binding.btnDeviceAdmin.setOnClickListener {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(this, AntiUninstallAdminReceiver::class.java)
            if (!dpm.isAdminActive(adminComponent)) {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Protect SAMRAT AI from unauthorized deactivation or uninstallation."
                    )
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Device Administrator is already ACTIVE!", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Notification Listener Permission
        binding.btnNotificationPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Allow SAMRAT AI to read notifications", Toast.LENGTH_LONG).show()
        }

        // 5. Toggle Assistant
        binding.btnToggleAssistant.setOnClickListener {
            savePreferences()
            val apiKey = binding.etApiKey.text?.toString()?.trim() ?: ""
            val name = binding.etAssistantName.text?.toString()?.trim() ?: "JARVIS"
            val behavior = binding.etAssistantBehavior.text?.toString()?.trim() ?: ""

            if (apiKey.isEmpty()) {
                Toast.makeText(this, "Please enter your Gemini API Key first!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant Floating Overlay permission first!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!isAssistantRunning) {
                // Initialize Assistant Manager with custom name and behavior
                assistantManager.initialize(apiKey, name, behavior)

                // Start Foreground Overlay Service
                val serviceIntent = Intent(this, FloatingOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }

                isAssistantRunning = true
                binding.btnToggleAssistant.text = "DEACTIVATE SAMRAT AI"
                binding.btnToggleAssistant.setBackgroundColor(ContextCompat.getColor(this, R.color.neon_pink))
                binding.tvSystemStatus.text = "System State: ONLINE & FLOATING"
                Toast.makeText(this, "SAMRAT AI is now active!", Toast.LENGTH_SHORT).show()
            } else {
                val serviceIntent = Intent(this, FloatingOverlayService::class.java)
                stopService(serviceIntent)

                isAssistantRunning = false
                binding.btnToggleAssistant.text = "ACTIVATE SAMRAT AI"
                binding.btnToggleAssistant.setBackgroundColor(ContextCompat.getColor(this, R.color.glow_green))
                binding.tvSystemStatus.text = "System State: STANDBY"
                Toast.makeText(this, "SAMRAT AI Deactivated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestCorePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 101)
        }
    }
}
