# SAMRAT AI - Neural Autonomous Personal Assistant

> **Creator & Master:** SAMRAT  
> **Architecture:** Native Android (Kotlin) + Gemini Multimodal Live API  
> **Status:** Fully Structured & Production Ready  

---

## 🌟 Overview

**SAMRAT AI** is an ultra-intelligent, deeply integrated personal Android AI assistant custom-engineered exclusively for **SAMRAT**. It operates as a persistent floating 3D companion capable of real-time vision, screen automation, natural neural voice dialogue, and system-level administrative control.

---

## 🚀 Core Features & Implementation

### 1. 3D Neon Particle Floating Bubble (`FloatingOverlayService` & `NeonBubbleVisualizer`)
- **3D Fibonacci Sphere:** 120 neon particles rotating continuously across 3D Euler angles with real-time perspective depth projection.
- **Audio Amplitude Reactivity:** Reacts dynamically to speech volume and generates a visual **explosion / shockwave pulse** when speaking or listening.
- **Floating Window:** Hovers over any application via `SYSTEM_ALERT_WINDOW`, draggable with smooth snap-to-edge physics.

### 2. Autonomous Touch & Continuous Swiping (`AssistantAccessibilityService` & `ScreenTouchController`)
- **Coordinate Tap & Multi-Tap:** Taps exact coordinates `(X, Y)` or clicks interactive UI nodes automatically.
- **Continuous Swiping Loop:** When asked to find an item, it continuously scrolls/swipes up or down until the target text or element is located on screen.
- **App Automation:** Operates WhatsApp, Phone dialer, and system settings hands-free.

### 3. Real-Time Multimodal Vision (`CameraVisionHelper` & `ScreenCaptureService`)
- **Live Camera Vision:** Streams front or rear camera video to Gemini Live to identify real-world objects, documents, or surroundings on command.
- **Live Screen Analysis:** Uses `MediaProjection` to analyze whatever is currently displayed on your phone's screen.

### 4. Ultra-Low Latency Neural Voice (`GeminiLiveClient`)
- **Bidirectional Streaming:** Direct WebSocket connection streaming 16kHz PCM audio to Gemini Live and receiving 24kHz natural human-like speech.
- **Barge-in / Interruptible:** Talk over the assistant at any time; it immediately pauses its output and listens.

### 5. Strict Permission-First Security Protocol (`AssistantManager`)
- **Zero Accidental Actions:** For sensitive operations (deleting files, moving storage items, making calls, sending WhatsApp messages), the assistant asks for SAMRAT's explicit permission before executing.

### 6. Voice Biometrics & Speaker Verification (`VoiceBiometrics`)
- Evaluates acoustic timbre, pitch, RMS energy, and zero-crossing rates.
- Ensures the assistant remains fiercely loyal only to SAMRAT and maintains strict boundaries against unauthorized voices.

### 7. Anti-Uninstall Device Administrator (`AntiUninstallAdminReceiver`)
- Registered with Android's `DevicePolicyManager`.
- Prevents accidental or unauthorized uninstallation of the assistant from device settings.

### 8. Micro Pop-up Screen (`FloatingSearchPopup`)
- A sleek, translucent glass popup that renders deep search results, news, and step-by-step reasoning without interrupting what you are doing.

### 9. Storage & System Controls (`SystemActionController`)
- **Storage:** Recursive file search, copy, move, and safe deletion.
- **Hardware:** Flashlight/torch toggle, notification monitoring, and phone calls.

---

## 📁 Project Structure

```
samrat-ai-assistant/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── layout/
│       │   │   ├── activity_main.xml
│       │   │   ├── overlay_bubble_layout.xml
│       │   │   └── popup_search_layout.xml
│       │   ├── values/
│       │   │   ├── colors.xml
│       │   │   ├── strings.xml
│       │   │   └── themes.xml
│       │   └── xml/
│       │       ├── accessibility_service_config.xml
│       │       └── device_admin_sample.xml
│       └── java/com/samrat/aiassistant/
│           ├── App.kt
│           ├── MainActivity.kt
│           ├── core/
│           │   ├── AssistantManager.kt
│           │   └── MasterSystemPrompt.kt
│           ├── ui/
│           │   ├── FloatingSearchPopup.kt
│           │   └── NeonBubbleVisualizer.kt
│           ├── service/
│           │   ├── AntiUninstallAdminReceiver.kt
│           │   ├── AssistantAccessibilityService.kt
│           │   ├── FloatingOverlayService.kt
│           │   ├── NotificationMonitorService.kt
│           │   └── ScreenCaptureService.kt
│           ├── ai/
│           │   ├── GeminiLiveClient.kt
│           │   ├── LongTermMemory.kt
│           │   └── VoiceBiometrics.kt
│           ├── automation/
│           │   ├── ScreenTouchController.kt
│           │   └── SystemActionController.kt
│           └── vision/
│               └── CameraVisionHelper.kt
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🛠️ How to Build & Run

### Option 1: Android Studio (Recommended)
1. Copy or clone this `samrat-ai-assistant` folder to your computer.
2. Open the project in **Android Studio**.
3. Connect your Android phone via USB debugging (or wireless debugging).
4. Click **Run (`Shift + F10`)** to install directly on your device.

### Option 2: Termux / Command Line
If compiling on Termux, ensure `openjdk-17` and Android SDK Command-Line Tools are installed, then run:
```bash
./gradlew assembleDebug
```
The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`
