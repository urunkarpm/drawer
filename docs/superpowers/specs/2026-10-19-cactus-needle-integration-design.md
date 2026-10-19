# Design Document: Cactus Compute Needle On-Device AI Integration

- **Status:** Approved
- **Date:** 2026-10-19
- **Target Platform:** Android (API 24+)
- **Architecture:** Multi-module Clean Architecture (`:core:needle`, `:feature:home`, `:feature:notifications`, `:feature:settings`)

---

## 1. Executive Summary & Goals

This project integrates **Cactus Compute Needle 3**—an ultra-compact (8–15 MB), sub-2-bit quantized foundation model—directly into Drawer Launcher. Needle executes completely on-device without internet access, aligning with Drawer's philosophy of **100% offline privacy, zero tracking, and zero telemetry**.

### Key Objectives
1. **Natural Language Tool Calling & Mobile Actions:** Allow users to execute phone actions (e.g., "set a timer for 10 minutes", "turn on flashlight", "open camera") directly by typing into the app drawer search bar or speaking into a voice trigger.
2. **Semantic Query Routing & App Search:** Route fuzzy queries and user intent to the right app or system shortcut.
3. **Smart Notification Intelligence:** Extract actionable data (one-tap OTP copy, banking transaction details) and group/filter notification noise on-device.
4. **Lightweight & Modular Delivery:** Keep the base APK lightweight (~5 MB) by downloading the quantized Needle binary (~10 MB) on-demand to internal storage, managed cleanly via a dedicated `:core:needle` module.

---

## 2. Architecture & Module Design

A new independent module `:core:needle` encapsulates the native Cactus runtime, model lifecycle, and tool definitions.

```
+-------------------------------------------------------------+
|              UI Layer (:feature:home, :feature:notifications)|
+-------------------------------------------------------------+
                              │
                              ▼
                     [NeedleEngine] (Public API)
                              │
        ┌─────────────────────┴─────────────────────┐
        ▼                                           ▼
[NeedleInferenceEngine]                    [NeedleModelManager]
        │                                           │
  libcactus.so (ARM64)                    Ktor HTTP Streaming
  MMap weights (needle.bin)               SHA-256 Checksum Verification
  Structured Grammar Decode               StateFlow<ModelStatus>
```

### 2.1 Public API (`:core:needle`)

```kotlin
sealed interface ModelStatus {
    data object NotDownloaded : ModelStatus
    data class Downloading(val progress: Float) : ModelStatus
    data class Ready(val modelPath: String, val sizeMb: Float) : ModelStatus
    data class Error(val cause: String) : ModelStatus
}

interface NeedleEngine {
    val modelStatus: StateFlow<ModelStatus>
    suspend fun predictToolCall(query: String, tools: List<ToolDefinition>): List<ToolCallResult>
    suspend fun <T> extractStructuredData(text: String, schema: JsonSchema, serializer: KSerializer<T>): T?
    suspend fun getEmbedding(text: String): FloatArray?
}
```

### 2.2 Model Lifecycle (`NeedleModelManager`)
- **Storage Location:** `context.filesDir/models/needle_3_cq2.bin`
- **Network Ingestion:** Streamed via `Ktor` directly to a temporary file, verifying SHA-256 integrity before replacing the active model file.
- **Memory Footprint:** Weights are memory-mapped (`mmap`) read-only, maintaining a working RAM profile of ~20–30 MB during inference.

---

## 3. Tool Calling Schema & Action Dispatcher

Needle 3 uses constrained grammar decoding to guarantee that model outputs parse into valid JSON matching declared tool schemas.

### 3.1 Registered Tools

| Tool | Parameters | Android Target |
| :--- | :--- | :--- |
| `set_timer` | `seconds: Int`, `label: String?` | `AlarmClock.ACTION_SET_TIMER` |
| `set_alarm` | `hour: Int`, `minute: Int`, `message: String?` | `AlarmClock.ACTION_SET_ALARM` |
| `toggle_setting` | `setting: "FLASHLIGHT" \| "WIFI" \| "BLUETOOTH"`, `enable: Boolean?` | `CameraManager.setTorchMode` / `ACTION_WIFI_SETTINGS` |
| `open_camera` | `mode: "PHOTO" \| "VIDEO" \| "SELFIE"` | `MediaStore.ACTION_IMAGE_CAPTURE` |
| `open_app` | `query: String` | `PackageManager.getLaunchIntentForPackage` |
| `search_web` | `query: String` | `Intent.ACTION_WEB_SEARCH` |
| `compose_sms` | `recipient: String`, `message: String` | `Intent.ACTION_SENDTO` (pre-filled) |

### 3.2 Action Dispatcher (`NeedleActionDispatcher`)
- Translates `ToolCallResult` into an executable `LauncherAction(title, icon, intent)`.
- If the query does not map to any tool (e.g., standard app names like "calculator"), Needle returns an empty list `[]`, falling back to standard app filtering with zero delay.
- Sensitive actions (SMS, dialing) open system dialogs pre-populated for user confirmation, preventing accidental execution.

---

## 4. UI & Interaction Design

### 4.1 Enhanced Search in `AllAppsDrawer.kt`
- **Dual Flow:** App filtering runs synchronously on keystrokes. A 150ms debounced coroutine queries `NeedleEngine`.
- **Action Suggestion Chip:** When an action is detected, a chip animates into view above the app grid (e.g. `[ ⏱️ Set 10m Timer — Tap to run ]`).
- Tapping or pressing keyboard 'Go' triggers the action.

### 4.2 Voice/AI Button in `GlanceHeader.kt`
- Adds a mic / AI icon to the glance bar.
- Launches system offline speech recognition (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`).
- Feeds recognized speech to Needle; single-turn actions (like "turn on flashlight") execute immediately with an undo snackbar.

### 4.3 Model Controls in `SettingsScreen.kt`
- **Toggle:** Enable/Disable on-device AI.
- **Model Card:** Shows download state, download progress bar, and "Delete Model" button to reclaim device storage.

---

## 5. Smart Notification Intelligence (`feature:notifications`)

Integrated with `DrawerNotificationListener`:
1. **OTP Extraction:** Uses structured schema to detect verification codes, providing a `[ Copy OTP: 123456 ]` quick action.
2. **Financial Alerts:** Extracts debit/credit sums and merchants from bank messages.
3. **Noise Classification:** Classifies incoming notifications (`TRANSACTION`, `AUTH`, `PERSONAL`, `PROMO`), allowing automatic grouping and noise suppression of spam.
4. **Local Embeddings:** Sentence vectors (768-dim) cluster duplicate notifications into stacked cards.

---

## 6. Resource Management & Error Handling

- **Battery Protection:** Inference suspended when device is in Battery Saver Mode.
- **Timeouts:** 500ms execution timeout per inference pass on `Dispatchers.Default`.
- **Graceful Degradation:** If the model is not downloaded or encounters an exception, all features degrade cleanly to standard offline launcher behavior.

---

## 7. Testing Strategy

1. **Unit Tests (`core:needle`):**
   - `NeedleModelManagerTest`: Verify download state transitions, SHA256 validation, and network error recovery.
   - `NeedleActionDispatcherTest`: Verify intent construction and extras for all registered tools.
   - `NotificationExtractionTest`: Verify OTP and expense extraction against sample notification payloads.
2. **UI Tests (`feature:home`, `feature:settings`):**
   - Action Suggestion Chip rendering in `AllAppsDrawer`.
   - Settings download toggle and progress indicator.
