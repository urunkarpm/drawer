<p align="center">
  <img src="icon.png" width="96" height="96" alt="Drawer App Icon" />
</p>

<h1 align="center">Drawer</h1>

<p align="center"><strong>The intentional Android home screen built for peak focus, fluid speed, and zero clutter.</strong></p>
<p align="center">
  <em>Stop doom-scrolling through icon grids. Turn your phone back into a high-leverage productivity tool.</em>
</p>

<p align="center">
  <a href="https://github.com/urunkarpm/drawer/releases"><img src="https://img.shields.io/badge/Release-v2.0.0-blue.svg?style=for-the-badge&logo=android" alt="Latest Release" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-green.svg?style=for-the-badge" alt="License" /></a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0+-orange.svg?style=for-the-badge&logo=android" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Built%20With-Jetpack%20Compose-4285F4.svg?style=for-the-badge&logo=jetpackcompose" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline%20%7C%20Zero%20Telemetry-success.svg?style=for-the-badge" alt="100% Offline" />
</p>

---

## 🎯 The 24-Hour Productivity Challenge

> **Install Drawer and use it for just one single day.**  
> If your phone doesn’t feel noticeably faster, less stressful, and dramatically less distracting within 24 hours, you can switch back with two taps. But once you experience a home screen that respects your attention and works at the speed of thought, you will never look at a standard launcher the same way again.

Most mobile launchers are designed like digital billboards: cluttered screens, recommended apps, unprompted news feeds, and chaotic notification badges competing for your dopaminergic attention.

**Drawer was built on a different philosophy: Intentionality.**  
Everything you need to get things done is reachable in under a second; everything else stays effortlessly out of your line of sight.

---

## 🌟 What's New in v2.0.0

* **🪡 Needle — 100% On-Device AI & Voice Assistant:** Experience private, local intelligence directly on your home screen. Execute voice or text commands to trigger system toggles (Flashlight, Wi-Fi, Bluetooth, Volume, Camera), launch apps, set timers & alarms, organize categories, and compute quick calculations—with zero cloud latency, zero telemetry, and zero data leaving your phone.
* **🧠 Semantic App Categorization:** Let Needle organize your entire app collection into context-driven drawers with on-device semantic understanding.
* **⚡ 120Hz Zero-Jank App Drawer:** Completely rebuilt the all-apps drawer list pipeline. Removed redundant allocations, eliminated expensive real-time background GPU blurs, and stabilized the search header for ultra-smooth 120fps flings.
* **🛡️ Smart Viewport & Mutual Exclusion:** Opening any category drawer automatically collapses the assistant drawer, and expanding the assistant drawer closes category drawers—eliminating viewport obstructions so all drawers and touch targets remain accessible.
* **🔕 Distraction-Free Toggle & Dev Notes:** On-device AI is disabled by default to maintain minimalist purity; enabling it presents a detailed capability guide.

---

## 🧠 How Drawer Reclaims Your Focus & Time

```
  Traditional Launchers                     Drawer Launcher
 ┌──────────────────────┐                  ┌──────────────────────┐
 │ [App] [App] [App]  🔴│  Visual Clutter  │      09:41 AM        │  Glance Header &
 │ [App] [App] [App]  🔴│  Badge Overload  │  22°C ☀️  45W Turbo  │  Live Charging Stats
 │ [App] [App] [App]  🔴│  News Feed       │                      │
 │ ── Page 1 of 6 ──────│  Endless Swiping │ ┌──────────────────┐ │  Single Focus Area
 │ 🔍 Sponsored search  │  Ad Tracking     │ │ 📁 Work (4 apps) │ │  Expandable Drawer
 └──────────────────────┘                  │ └──────────────────┘ │
                                           │   [ 📞  💬  🌐 ]     │  Floating Glass Dock
                                           └──────────────────────┘
```

### 1. 🪡 Needle — 100% Offline On-Device AI Assistant
Experience natural language speed without compromising your privacy:
* **Voice & Text Quick Actions:** Tap the microphone on the glance header to control your phone at the speed of thought.
* **Deterministic Fast Paths:** Instant execution for timers, alarms, system toggles (Flashlight, Wi-Fi, Bluetooth, Audio), and math calculations without model inference wait times.
* **Intelligent Drawer Management:** Ask Needle to move apps into drawers (*"Move WhatsApp to Social"*, *"Group my finance apps"*) or run semantic categorization across all installed apps.
* **100% Offline & Zero Telemetry:** Built with native on-device inference (`core/needle`). No cloud servers, no account logins, and no data harvesting.

### 2. 🗂️ Accordion Drawers — Context-Driven Organization
Instead of spreading 80+ apps across 5 sprawling home screen pages, group your tools by your mental workflow:
* **Workflow Buckets:** Group apps into expandable categories like **`Deep Work`**, **`Communication`**, **`Finance`**, **`Learning`**, and **`Essentials`**.
* **One Tap Expands, One Tap Closes:** Expand only the drawer you need right now. Everything else stays collapsed, shielding your brain from visual overwhelm.
* **Multi-Drawer Versatility:** Need an app like Slack in both *Work* and *Chat*? Add it to both without duplicating shortcuts or wrestling with folder restrictions.
* **Smart Auto-Collapse:** Keeps your screen pristine automatically so you're never greeted by yesterday's mess.

### 3. 🪟 3D iPod Cover Flow & Expandable Widget Drawers
Widgets are powerful for productivity, but standard launchers force you to dedicate entire screen pages to them.
* **Cover Flow 3D Stacking:** Flip smoothly through interactive productivity widgets (Calendar, Tasks, Notes, Music) with tactile, physics-based depth.
* **Widget Accordion Drawers:** Tuck widgets into collapsible drawers. Pull up your day's schedule or task list with a tap, take action, and let it auto-collapse in 5 seconds.
* **Interactive Resizing:** Customize widget heights and grid spans to fit your exact information density requirements.
* **Bulletproof Engine:** Engineered with robust lifecycle hooks to prevent widget freezing and host memory leaks.

### 4. ⚡ Zero-Lag Launching & Smart Frecency Suggestions
When you know what you want to do, every millisecond of friction matters.
* **Frecency Predictive Row:** Surfaces your most relevant apps dynamically based on a smart frequency + recency calculation.
* **Instant Type-to-Filter:** Press a key and launch target apps in sub-milliseconds.
* **Thumb-Glide Alphabet Rail:** Slide your thumb down the screen edge to jump instantly to any letter index—effortless single-handed navigation on large devices.
* **Hidden Apps Vault:** Conceal apps you rarely need (or apps that trigger unhealthy habits) so you aren’t tempted by idle dopamine loops.

### 5. 🔕 Distraction Firewall — Notification Hub with Quick Mute
Notifications are the single biggest killer of deep work and flow state. Drawer puts you firmly in control:
* **Swipe-Up Notification Hub:** See incoming alerts neatly grouped by application—no chaotic notification shade pulling down over your workspace.
* **Home-Screen Quick Mute:** A chat group or noisy app breaking your concentration? Mute it for **1 hour**, **24 hours**, or **permanently** right from the launcher—without digging through system settings menus.
* **Privacy Mask Mode:** Instantly masks message previews on your home screen to keep sensitive work emails, two-factor codes, and private messages hidden from passersby.

### 6. 🔋 Live Power Stats, Quick Speed Test & Frosted Glass Dock
* **Live Battery & Fast-Charging Telemetry:** Tap the status icon to see real-time charging wattage (`W`), voltage (`V`), current (`mA`), battery temperature, and OEM fast-charging brand detection.
* **Dual-Stream Speed Test:** Run an instantaneous download speed test powered by Google Edge CDN and Cloudflare directly from the status popup.
* **Floating Glass Dock:** Pin 1 to 5 vital daily drivers (Phone, Browser, Notes, Terminal) in an elegant, blurred frosted glass bar.
* **Mirror Glance Widget:** Heading into an important video call? Tap the discreet front-camera glance right on your home screen to check your appearance without opening a heavy camera application.

### 7. 🛡️ 100% Offline, Ad-Free, & Zero Battery Drain
* **Zero Analytics & Trackers:** No cloud telemetry, no background pings, no accounts, and no data harvesting. 100% of your data stays on your device.
* **Pure 120Hz Smoothness:** Crafted in modern Jetpack Compose with optimized redraw cycles, true AMOLED black themes, and zero background battery drain.

---

## 📊 Stock Launchers vs. Drawer

| Capability | Stock / OEM Launchers | Nova / Third-Party | ✨ **Drawer** |
| :--- | :---: | :---: | :---: |
| **Mindset** | App Storefront & Feeds | Tweaker's Sandbox | **Intentional Productivity** |
| **Organization** | Rigid icons & nested folders | Complex grid layouts | **Accordion Drawers & 3D Stacks** |
| **App Predictions** | Ad/sponsored recommendations | Static recent apps | **On-Device Frecency Engine** |
| **Power Telemetry** | Basic percentage only | Requires 3rd-party apps | **Live Wattage, V/mA & OEM Turbo** |
| **Speed Benchmark** | ❌ None (ad-laden apps) | ❌ None | **Built-in Google Edge Speed Test** |
| **Distraction Defense** | Badges & push alerts | Basic badge toggles | **Home Screen Quick Mute & Privacy Mask** |
| **Widget Handling** | Screen hogging | Static placement | **Collapsible Widget Drawers & Cover Flow** |
| **Wallpapers & Fonts** | Fixed system defaults | Limited fonts | **Pantone Studio & Typography Packs** |
| **On-Device AI Assistant** | Cloud Gemini/Siri/Copilot (data harvesting) | ❌ None | **🪡 Needle (100% Offline & Private)** |
| **Privacy & Telemetry** | Cloud sync & ad analytics | Often tracks crash analytics | **100% Offline & Zero Telemetry** |
| **Setup Time** | Cluttered by default | 30+ minutes configuring | **Productive in under 60 seconds** |

---

## 🚀 Get Started in Under 60 Seconds

You don't need an hour to configure Drawer. It comes with an intuitive, guided onboarding tour and takes less than a minute to set up:

```
Step 1: Download & Install ───► Step 2: Set as Default ───► Step 3: Group by Intent
Download app-release.apk         Press Home → Drawer → Always       Create 3-4 Focus Drawers
```

### Option A: Install Pre-Built APK (Fastest)
1. Download the latest [`app-release.apk`](app-release.apk) directly from this repository or from the [GitHub Releases](https://github.com/urunkarpm/drawer/releases) page.
2. Open the downloaded file on your Android device and tap **Install**.
3. Press your **Home** button, select **Drawer**, and choose **Always**.
4. Follow the brief built-in welcome tour to configure your first drawers!

### Option B: Build from Source
If you are a developer and want to inspect the code or build it yourself:

```bash
# Clone the repository
git clone https://github.com/urunkarpm/drawer.git
cd Drawer

# Build the release APK with Gradle
./gradlew assembleRelease

# The compiled APK will be generated at:
# app/build/outputs/apk/release/app-release.apk
```

To install directly to a connected device via ADB:
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 🎨 Visual Customization Options

Make your productivity workspace match your personal aesthetic:
* **Dynamic Pantone Wallpaper Studio:** Procedural wallpaper generator with customizable harmonies, duotones, and analog grain.
* **Curated Typography Packs:** Select your clock font from Google Sans, DS-Digital, Evil Empire, Hypik, Spookyy, and Texas Troupe.
* **App Label Scaling:** Fine-tune label text size across groups, dock, and all-apps drawer with `LocalAppLabelScale`.
* **Deep AMOLED Pitch Black:** Ultra-battery-efficient, high-contrast black styling.
* **Material You Dynamic Theming:** Automatically adapts accent colors based on your wallpaper.
* **Third-Party Icon Packs:** Full compatibility with icon packs from Google Play, plus per-app icon overrides.
* **Adaptive Shapes:** Choose squircle, circle, teardrop, or rounded square icons to maintain uniform geometry.
* **Double-Tap to Lock:** Double-tap any blank area to sleep your device cleanly without interfering with biometric face or fingerprint unlock.
* **One-Tap Backup & Restore:** Export your complete setup (drawers, widgets, icons, dock layout) to a portable file and restore it in seconds.

---

## 🏗️ Architecture & Tech Stack

For engineers and contributors, Drawer is engineered according to Google's official Modern Android Development (MAD) architecture:

```
├── app/                  # Main application module, navigation & initialization
├── core/
│   ├── common/           # Coroutine dispatchers, DrawerWidgetHostManager, utilities
│   ├── model/            # Pure domain models (AppDrawer, WidgetItem, DockItem)
│   ├── designsystem/     # Glassmorphic components, Pantone wallpaper, typography
│   ├── database/         # Room database, DAOs, and persistence entities
│   ├── datastore/        # Jetpack Preferences DataStore for user preferences
│   ├── needle/           # On-device AI engine, native Cactus bridge, tool dispatcher
│   └── data/             # Repositories reconciling DB, installed apps & frecency state
└── feature/
    ├── home/             # Main screen orchestrator, DuoStatusIcon, speed test & walkthrough
    ├── groups/           # App drawer accordion management & categorized grouping
    ├── dock/             # Floating frosted glass dock & fast action bar
    ├── notifications/    # System notification listener, quick-mute & privacy masking
    ├── iconpacks/        # Icon pack parsing, caching & custom shape masking
    └── settings/         # Visual customization, typography packs, backup & tour
```

* **UI Layer:** 100% Jetpack Compose with Material 3 design tokens.
* **Asynchronous Streams:** Kotlin Coroutines and reactive `StateFlow` / `SharedFlow`.
* **Predictive Algorithm:** On-device Frecency ranking reconciling launch count and timestamps.
* **Persistence:** AndroidX Room DB + Jetpack DataStore (zero raw SQLite/SharedPreferences).
* **Performance:** Hardware-accelerated graphics with RenderEffect runtime blur shaders (Android 12+) and legacy-safe fallbacks.

---

## 🤝 Contributing

We welcome community feedback, pull requests, and ideas!
* Found a bug or have a feature idea? Open an [Issue](https://github.com/urunkarpm/drawer/issues).
* Want to contribute code? Fork the repo, create a feature branch (`git checkout -b feat/my-enhancement`), and submit a Pull Request.

---

## 📜 License

Drawer is open-source software licensed under the [Apache License 2.0](LICENSE).
Your device, your data, your launcher.
