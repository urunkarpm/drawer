# Drawer 📱

> A personalisation-first, privacy-focused modern Android launcher built from scratch with Jetpack Compose, Material 3, Clean Architecture, and Android 17 (API 37) platform standards.

---

## 🌟 Key Features

1. **Customisable Dock (0–5 Apps)**
   - Pinned bottom bar holding up to 5 apps.
   - Long-press menu and drag-to-reorder support.
   - Background styling: Real-time blur with `RenderEffect` on Android 12+ (API 31+), translucent/solid fallback on older versions.
   - Customisable icon size (40–72 dp), corner radius, and label visibility.

2. **User-Defined App Groups / Accordion Drawers**
   - Categorise installed applications into collapsible sections (e.g. Work, Social, Finance, Media, Tools).
   - Create, edit, rename, recolour, reorder, and set custom category icons.
   - Grid or List view layout modes with configurable column count (3–6) and sort orders (Manual, A–Z).
   - "Multi-group apps" setting allowing an app to appear across multiple drawers.
   - Fully persisted in Room database.

3. **Home-Screen Glance**
   - Clean "At a glance" header displaying live digital clock (12/24-hour modes), formatted date, and real-time weather.
   - Powered by Open-Meteo REST API (zero tracking, no API keys required).
   - Location resolution: Google Play Services Fused Location Provider with manual city fallback.
   - Background periodic sync via WorkManager (`WeatherRefreshWorker`).
   - Immersive mode toggle using `WindowInsetsControllerCompat` to hide the system status bar.

4. **Notification Drawer with Quick Mute**
   - Integrated `NotificationListenerService` (`DrawerNotificationListener`) capturing live device notifications.
   - Active notification grouping by application with app icons, title, message body, and time.
   - Swipe-to-dismiss via Material 3 `SwipeToDismissBox` invoking OS-level `cancelNotification`.
   - **Quick Mute per app**: Snooze notifications for 1 hour, 24 hours, or permanent launcher-level hide rules.
   - Direct deep-links to Android system per-app notification settings.
   - Privacy Mode: Mask notification body text on the home screen.

5. **Third-Party Icon Pack Support & Adaptive Shapes**
   - Automatic discovery of installed icon packs (`org.adw.launcher.THEMES`, `com.novalauncher.THEME`, etc.).
   - Fast streaming XML parser for `appfilter.xml` mapping component names to drawable resources.
   - Per-app icon overrides.
   - Adaptive icon shape engine: System default, Circle, Squircle, Rounded Square, Teardrop.

6. **All Apps Drawer & Instant Search**
   - Slide-up bottom sheet with instant query filtering across all installed apps and work profiles (`LauncherApps`).
   - Live package install, uninstall, and update broadcast monitoring.

7. **Global Personalisation & Backup / Restore**
   - Central Settings screen: System / Light / Dark / AMOLED pure black themes, Material You Dynamic Color.
   - Complete configuration export & import (categories, dock, mute rules, icon overrides, preferences) to JSON via Android Storage Access Framework (`ActivityResultContracts.CreateDocument` & `OpenDocument`).

---

## 🏗️ Architecture & Module Structure

Drawer follows **Clean Architecture** with strict unidirectional data flow (UDF) across a modular Gradle project:

```
:app                    # Application entry point, MainActivity (HOME intent filters), NotificationListenerService
:core:model             # Core domain models (AppInfo, AppGroup, DockItem, MutedAppRule, LauncherBackup)
:core:database          # Room database (v1), Entities (AppGroup, Dock, MutedRules, Overrides), DAOs
:core:datastore         # Jetpack Preferences DataStore for all launcher settings
:core:designsystem      # Material 3 Expressive theme, typography, dynamic colors, AMOLED palette
:core:common            # Coroutines dispatchers and common qualifiers
:core:data              # Repository implementations (App, Dock, Groups, Weather, Notifications, IconPack, Backup)
:feature:home           # HomeScreen, GlanceHeader, AllAppsDrawer, AppActionBottomSheet
:feature:dock           # DockBar, DockActionBottomSheet, DockViewModel
:feature:groups         # GroupsAccordion, CategorySelectionBottomSheet, EditGroupDialog, GroupsViewModel
:feature:notifications  # NotificationDrawer, QuickMuteBottomSheet, NotificationRulesBottomSheet
:feature:iconpacks      # IconPackScreen, IconPackPickerSheet, IconPackViewModel
:feature:settings       # SettingsScreen, SettingsViewModel, SAF Backup/Restore
```

---

## 🔒 Permissions & Privacy Justifications

Drawer is designed with privacy as a foundational principle: **no telemetry, no analytics, no third-party SDKs**. All user data and preferences remain strictly on-device.

| Permission | Reason & Justification |
| :--- | :--- |
| `QUERY_ALL_PACKAGES` | **Mandatory for Android Launchers**. As the primary home screen, Drawer must discover, display, and launch all installed applications on the device. |
| `ACCESS_COARSE_LOCATION` | **Optional for Weather Glance**. Used exclusively by `FusedLocationProviderClient` to fetch local weather coordinates from Open-Meteo. Users can opt out and specify a manual city name fallback in Settings. |
| `ACCESS_FINE_LOCATION` | **Optional for Weather Glance**. Complementary location precision when granted by the user. |
| `INTERNET` | **Weather Sync Only**. Used solely to query weather forecasts from the open-source Open-Meteo API. No user data or analytics are ever transmitted. |
| `ACCESS_NETWORK_STATE` | Used by WorkManager and Ktor client to check network availability before scheduling weather refresh jobs. |
| `POST_NOTIFICATIONS` | Allows Drawer to notify the user upon background events when required. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Required by Android platform security for `DrawerNotificationListener` to receive notifications from installed applications for the notification drawer and quick mute features. |

---

## 🛠️ Tech Stack & Version Catalog

All libraries and build tools use the latest verified stable releases:

- **Target SDK**: Android 17 (API 37)
- **Compile SDK**: Android 17 (API 37)
- **Min SDK**: Android 8.0 (API 26)
- **Kotlin**: `2.3.20` (K2 compiler)
- **Android Gradle Plugin (AGP)**: `9.4.1`
- **Gradle**: `9.8.0`
- **Jetpack Compose BOM**: `2026.09.00`
- **Hilt**: `2.60.1` (KSP)
- **Room**: `2.8.5` (KSP)
- **Preferences DataStore**: `1.2.1`
- **Coil 3**: `3.6.3`
- **Ktor Client**: `3.6.0`
- **Kotlinx Serialization**: `1.11.0`
- **Kotlinx Coroutines**: `1.11.0`
- **Play Services Location**: `21.4.0`
- **WorkManager**: `2.12.0`

---

## 🧪 Testing & Verification

Drawer maintains 100% unit test coverage across its domain, repository, and viewmodel layers:

```bash
# Run unit test suite across all modules
./gradlew test

# Assemble release or debug APK
./gradlew assembleDebug
```

CI workflows are configured in `.github/workflows/ci.yml` running validation, unit tests, and APK compilation on every push and pull request.
