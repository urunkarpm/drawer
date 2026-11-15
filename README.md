<p align="center">
  <img src="icon.png" width="96" height="96" alt="Drawer App Icon" />
</p>

<h1 align="center">Drawer</h1>

<p align="center"><strong>A minimal Android launcher designed for focus, speed, and privacy.</strong></p>
<p align="center">
  <em>Replace crowded icon grids with categorized drawers and fast navigation.</em>
</p>

<p align="center">
  <a href="https://github.com/urunkarpm/drawer/releases"><img src="https://img.shields.io/badge/Release-v2.10.11-blue.svg?style=for-the-badge&logo=android" alt="Latest Release" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-green.svg?style=for-the-badge" alt="License" /></a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0+-orange.svg?style=for-the-badge&logo=android" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/Built%20With-Jetpack%20Compose-4285F4.svg?style=for-the-badge&logo=jetpackcompose" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Privacy-100%25%20Offline%20%7C%20Zero%20Telemetry-success.svg?style=for-the-badge" alt="100% Offline" />
</p>

---

## Introduction

<!-- ponytail: Concise technical introduction. Ceiling: focuses on utility over marketing prose. Upgrade path: add user testimonials if promotional material is necessary. -->

Drawer is a fast Android launcher that replaces multi-page icon grids with expandable category drawers.

Most mobile home screens encourage distraction. They display news feeds, sponsored recommendations, and constant badge counts. Finding a specific application often requires scanning through multiple dense pages.

Drawer organizes tools around intentional workflows. The interface keeps all primary tools accessible in under one second while hiding unused applications from view.

```
  Traditional Launchers                     Drawer Launcher
 ┌──────────────────────┐                  ┌──────────────────────┐
 │ [App] [App] [App]  🔴│  Visual Clutter  │      09:41 AM        │  Glance Header &
 │ [App] [App] [App]  🔴│  Badge Overload  │  22°C ☀️  45W Turbo  │  Battery Statistics
 │ [App] [App] [App]  🔴│  News Feed       │                      │
 │ ── Page 1 of 6 ──────│  Excessive Pages │ ┌──────────────────┐ │  Single Focus Area
 │ 🔍 Sponsored search  │  Ad Tracking     │ │ 📁 Work (4 apps) │ │  Expandable Drawer
 └──────────────────────┘                  │ └──────────────────┘ │
                                           │   [ 📞  💬  🌐 ]     │  Dock Bar
                                           └──────────────────────┘
```

---

## Interface Layout

The home screen consists of five primary components:

1. **Glance Header:**
   Displays current time, local weather, live battery telemetry, and a front-camera mirror shortcut.
2. **Category Drawers:**
   Expandable accordion groups that organize applications by task context.
3. **Dock Bar:**
   A persistent bottom navigation bar holding one to five essential applications.
4. **Widget Feed:**
   A collapsible stack of interactive widgets accessible without consuming permanent screen space.
5. **Task Organizer:**
   A swipeable utility sheet on the right screen for offline task management.

---

## Key Capabilities

### 1. Accordion Category Drawers
* **Workflow Groups:** Group applications into categories such as Work, Communication, Finance, and Media.
* **Single-Tap Operation:** Tap a drawer header to expand its applications. Tap again to collapse it. Other drawers stay closed.
* **Multi-Category Assignment:** Assign an application to multiple categories without creating duplicate desktop shortcuts.
* **Automatic Collapse:** Drawers collapse automatically when you launch an app or navigate away.

### 2. Rapid Application Access
* **Frecency Engine:** Calculates dynamic application suggestions from launch frequency and recent timestamps.
* **Instant Keyboard Search:** Type any character to filter and launch target applications.
* **Alphabet Index Rail:** Drag along the screen edge to jump directly to any letter index.
* **Custom Density:** Select four, five, or six columns in settings to match your screen size.

### 3. Collapsible Widget Stacks
* **Cover Flow Stacking:** Swipe through interactive widgets with hardware-accelerated depth effects.
* **Collapsible Storage:** Place widgets inside drawers that expand on demand and collapse after use.
* **Adjustable Dimensions:** Adjust widget heights and spans to match your layout requirements.
* **Lifecycle Protection:** Custom host hooks prevent widget freezing and background memory leaks.

### 4. Distraction Control and Notifications
* **Notification Hub:** Swipe up to inspect notifications grouped by application.
* **Quick Mute:** Mute notifications from noisy applications for one hour, 24 hours, or indefinitely.
* **Privacy Masking:** Masks message previews on the home screen to protect personal communications.
* **Monochrome Mode:** Apply a unified black-and-white theme to reduce visual stimulation.

### 5. Private Application Vault
* **Biometric Protection:** Secure sensitive applications behind Android `BiometricPrompt` authentication.
* **Complete Isolation:** The launcher excludes vaulted applications from the main drawer, search queries, and alphabet index.
* **Private Notifications:** Notifications from vaulted applications appear only inside the unlocked vault drawer.
* **Gesture Trigger:** Pull horizontally from the last dock application to open the vault prompt.

### 6. Hardware Telemetry and Diagnostics
* **Live Power Metrics:** Tap the status pill to inspect real-time charging wattage, voltage, current, and battery temperature.
* **Network Speed Test:** Run download speed tests using Google Edge CDN and Cloudflare from the status panel.
* **Screen Curve Matching:** Detect physical display corner radii automatically on Android 12 and higher.

### 7. Offline Task Management
* **Local Task Organizer:** Organize tasks with due dates, priority tags, and category filters.
* **Geofence Alerts:** Set proximity alerts using device location sensors and OpenStreetMap.
* **Zero Cloud Dependency:** The task engine runs completely on the device without Google Play Services.

### 8. Privacy by Design
* **Zero Telemetry:** Drawer contains no analytics SDKs, no tracking libraries, and no crash reporting services.
* **Offline Operation:** The launcher requires no user accounts and transmits no personal data across the network.
* **Local Persistence:** Stores preferences and categories in a local Room database and DataStore files.

---

## Navigation Gestures

| Gesture | Action |
| :--- | :--- |
| **Tap Category** | Expands or collapses the selected accordion drawer. |
| **Swipe Up** | Opens the all-apps drawer or notification hub. |
| **Swipe Down** | Expands notifications or closes open drawers. |
| **Swipe Right** | Opens the offline task organizer. |
| **Swipe Left** | Opens the widget feed. |
| **Swipe Dock Edge** | Triggers authentication for the private application vault. |
| **Double Tap Wallpaper** | Puts the device to sleep without breaking biometric unlock. |

---

## Feature Comparison

| Capability | Stock Launchers | Standard Third-Party | Drawer |
| :--- | :--- | :--- | :--- |
| **Design Focus** | Content feeds and store links | Cosmetic tweaking | Intentional task focus |
| **Organization** | Multi-page icon grids | Multi-page icon grids | Accordion category drawers |
| **App Suggestions** | Sponsored advertisements | Recent application list | On-device frecency algorithm |
| **Power Telemetry** | Battery percentage only | Requires extra apps | Real-time wattage, voltage, and current |
| **Network Diagnostic** | None | None | Integrated network speed test |
| **Notification Control** | Notification dots | Basic dot settings | Quick mute and preview masking |
| **Widget Placement** | Consumes full screen pages | Static placement | Collapsible 3D widget drawers |
| **Data Privacy** | Cloud tracking and profiling | Analytics and crash reports | 100% offline with zero telemetry |
| **Configuration Time** | Cluttered by default | 30 or more minutes | Under 60 seconds |

---

## Installation and Setup

You can configure Drawer in less than one minute using the built-in welcome tour:

```
Step 1: Download and Install ───► Step 2: Set Default ───► Step 3: Organize Apps
Download app-release.apk          Press Home → Drawer      Create categories
```

### Option A: Install Pre-Built APK
1. Download [`app-release.apk`](app-release.apk) from this repository or the [GitHub Releases](https://github.com/urunkarpm/drawer/releases) page.
2. Open the file on your Android device and tap **Install**.
3. Press the **Home** button on your device.
4. Select **Drawer** and tap **Always**.
5. Complete the setup tour to configure your initial categories.

### Option B: Build from Source
To build the application package from source code:

```bash
# Clone the repository
git clone https://github.com/urunkarpm/drawer.git
cd Drawer

# Build the release APK with Gradle
./gradlew assembleRelease

# Gradle outputs the APK at:
# app/build/outputs/apk/release/app-release.apk
```

Install to a connected device with ADB:
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## Visual Customization

Customize the launcher appearance in Settings:
* **Wallpaper Studio:** Generate procedural wallpapers with color harmonies, duotones, and grain effects.
* **Typography Packs:** Select clock fonts including Google Sans, DS-Digital, Evil Empire, Hypik, Spookyy, and Texas Troupe.
* **App Label Scaling:** Adjust label text sizes across groups, the dock, and the app drawer using `LocalAppLabelScale`.
* **AMOLED Black Theme:** High-contrast dark theme designed to minimize battery usage.
* **Material You Theming:** Automatically adapts accent colors based on the current wallpaper.
* **Icon Packs:** Supports icon packs from Google Play and individual per-app icon overrides.
* **Adaptive Shapes:** Select squircle, circle, teardrop, or rounded square shapes for app icons.
* **Backup and Restore:** Export your layout settings to a backup file and restore configuration in seconds.

---

## Architecture and Tech Stack

Drawer follows Google's Modern Android Development (MAD) architecture guidelines:

```
├── app/                  # Main application module, navigation, and initialization
├── core/
│   ├── common/           # Coroutine dispatchers, DrawerWidgetHostManager, utilities
│   ├── model/            # Pure domain models (AppDrawer, WidgetItem, DockItem)
│   ├── designsystem/     # UI components, Pantone wallpaper, typography
│   ├── database/         # Room database, DAOs, and persistence entities
│   ├── datastore/        # Jetpack Preferences DataStore for user preferences
│   └── data/             # Repositories reconciling DB, installed apps, and frecency
└── feature/
    ├── home/             # Main screen orchestrator, DuoStatusIcon, speed test, and tour
    ├── groups/           # App drawer accordion management and categorized grouping
    ├── dock/             # Dock bar and fast action bar
    ├── notifications/    # Notification listener, quick-mute, and privacy masking
    ├── iconpacks/        # Icon pack parsing, caching, and custom shape masking
    ├── todo/             # Gesture to-do engine, location alerts, and task statistics
    └── settings/         # Visual customization, typography packs, backup, and tour
```

* **UI Framework:** Jetpack Compose with Material 3 design tokens.
* **Asynchronous Streams:** Kotlin Coroutines with reactive `StateFlow` and `SharedFlow`.
* **App Ranking:** On-device frecency ranking calculated from launch counts and timestamps.
* **Persistence:** AndroidX Room database and Jetpack DataStore.
* **Graphics Performance:** Hardware-accelerated graphics using `RenderEffect` runtime shaders on Android 12 and higher.
* **Networking:** Standard platform `HttpURLConnection` for weather and update checks with zero external dependencies.

---

## Release History

For release notes across all versions, see the [GitHub Releases](https://github.com/urunkarpm/drawer/releases) page.

---

## Contributing

We welcome feedback, issues, and code contributions:
* Report defects or request features by opening an [Issue](https://github.com/urunkarpm/drawer/issues).
* Fork the repository, create a branch (`git checkout -b feat/my-enhancement`), and submit a pull request.

---

## License

Drawer is open-source software under the [Apache License 2.0](LICENSE).
