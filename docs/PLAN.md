# Architecture and Implementation Plan: Drawer (com.urunkarpm.drawer)

## 1. System Overview & Architecture Diagram

```
+---------------------------------------------------------------------------------+
|                                     :app                                        |
|  - MainActivity (HOME / MAIN / DEFAULT intent filters)                          |
|  - DrawerApplication (Hilt AndroidApp)                                         |
|  - DrawerNotificationListener (NotificationListenerService)                    |
+---------------------------------------------------------------------------------+
                                         |
                                         v
+---------------------------------------------------------------------------------+
|                                 Feature Modules                                 |
|  :feature:home            (Glance + Notification Drawer + Groups + Dock container) |
|  :feature:dock            (Dock bar, 0-5 apps, drag-and-drop, dock customization)  |
|  :feature:groups          (Collapsible categories, grid/list, group management) |
|  :feature:notifications   (Active notification tray, swipe dismiss, quick mute) |
|  :feature:iconpacks       (Theme discovery, appfilter.xml parser, icon cache)   |
|  :feature:settings        (Theme/AMOLED, Backup/Restore JSON, Preferences UI)  |
+---------------------------------------------------------------------------------+
                                         |
                                         v
+---------------------------------------------------------------------------------+
|                                   Core Modules                                  |
|  :core:designsystem       (Material 3 Expressive, Dynamic Color, Haze Blur UI)  |
|  :core:data               (Repositories: Apps, Weather, Notifications, Backups)|
|  :core:database           (Room DB: Groups, Dock, MutedRules, IconOverrides)    |
|  :core:datastore          (Preferences DataStore: Glance, Dock, Notification)   |
|  :core:model              (Pure Kotlin data models, immutability, contracts)    |
|  :core:common             (Dispatchers, CoroutineScopes, Formatters, Extensions)|
+---------------------------------------------------------------------------------+
```

### Unidirectional Data Flow (UDF)
```
[User Action / OS Event (LauncherApps/NotificationListener)]
                     |
                     v
             [Domain / ViewModel]
                     |
         (Updates Repository / Room / DataStore)
                     |
                     v
            [StateFlow<UiState>]
                     |
                     v
   [Jetpack Compose View with collectAsStateWithLifecycle]
```

---

## 2. Multi-Module Project Structure

| Module | Responsibility | Dependencies |
| :--- | :--- | :--- |
| `:core:model` | Domain models (`AppInfo`, `AppGroup`, `DockItem`, `GlanceInfo`, `NotificationItem`) | None |
| `:core:common` | Coroutine dispatchers, result wrappers, time utilities | `:core:model` |
| `:core:designsystem` | Material 3 Expressive themes, color schemes, blur surfaces, components | `:core:common` |
| `:core:database` | Room database, entities, DAOs, migrations, type converters | `:core:model`, `:core:common` |
| `:core:datastore` | Preferences DataStore schemas, serializers, preference repositories | `:core:model`, `:core:common` |
| `:core:data` | App repository, Weather repository (Open-Meteo), Notification repository, IconPack repository | `:core:model`, `:core:database`, `:core:datastore`, `:core:common` |
| `:feature:home` | Root home screen orchestrator: Glance + Notifications + Groups + Dock + All Apps Drawer | `:core:data`, `:core:designsystem`, `:feature:dock`, `:feature:groups`, `:feature:notifications` |
| `:feature:dock` | Dock UI, drag-and-drop targets, dock item slots, dock settings | `:core:data`, `:core:designsystem` |
| `:feature:groups` | Custom drawer category lists/grids, add/edit/delete groups, app-to-group assignment | `:core:data`, `:core:designsystem` |
| `:feature:notifications`| Notification list UI, mute dialogs, snooze triggers, privacy toggle | `:core:data`, `:core:designsystem` |
| `:feature:iconpacks` | Icon pack picker dialog, custom Coil fetcher, appfilter parser | `:core:data`, `:core:designsystem` |
| `:feature:settings` | Settings navigation, theme selector, backup/restore JSON exporter/importer | `:core:data`, `:core:designsystem` |
| `:app` | Application class, MainActivity, Hilt DI root, NotificationListenerService, WorkManager initialization | All `:feature:*` and `:core:*` |

---

## 3. Resolved Library Versions (Version Catalog)

All dependencies are pinned to verified latest stable releases:

- **Target / Compile SDK:** Android 17 (API 37)
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Android Gradle Plugin (AGP):** `9.4.1` (or `8.9.1` fallback depending on Gradle version)
- **Gradle:** `9.4.1` / `9.8.0`
- **Kotlin:** `2.3.0` / `2.4.20`
- **KSP:** `2.3.12`
- **Compose BOM:** `2026.09.00`
  - Compose UI: `1.12.1`
  - Compose Foundation: `1.12.1`
  - Material 3: `1.4.0` (with Material 3 Expressive styling)
- **Material 3 Adaptive:** `1.3.0`
- **Navigation Compose:** `2.10.2` (Type-Safe Navigation with `@Serializable`)
- **Hilt:** `2.60.1` (`androidx.hilt:hilt-navigation-compose:1.4.0`)
- **Room:** `2.8.5` (KSP compiler)
- **DataStore Preferences:** `1.2.1`
- **WorkManager:** `2.12.0`
- **Lifecycle Runtime Compose:** `2.11.0`
- **Coil 3:** `3.6.3` (`io.coil-kt.coil3:coil-compose`, `coil-network-okhttp`)
- **Ktor Client:** `3.6.0` (`ktor-client-android`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`)
- **Play Services Location:** `21.4.0`
- **kotlinx-coroutines:** `1.11.0`
- **kotlinx-serialization:** `1.11.0`
- **Detekt:** `1.21.0`
- **JUnit 4 / MockK / Turbine:** `4.13.2` / `1.14.0` / `1.2.0`

---

## 4. Room Database Schema & Entities

Database name: `drawer.db` (Version 1)

### 1. `app_groups` Table
```sql
CREATE TABLE app_groups (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    icon_name TEXT NOT NULL,
    color_hex TEXT NOT NULL,
    order_index INTEGER NOT NULL,
    is_expanded INTEGER NOT NULL DEFAULT 1,
    view_type TEXT NOT NULL DEFAULT 'GRID', -- 'GRID' | 'LIST'
    column_count INTEGER NOT NULL DEFAULT 4,
    sort_order TEXT NOT NULL DEFAULT 'MANUAL' -- 'MANUAL' | 'ALPHABETICAL' | 'USAGE'
);
```

### 2. `app_group_items` Table
```sql
CREATE TABLE app_group_items (
    id TEXT PRIMARY KEY NOT NULL,
    group_id TEXT NOT NULL,
    package_name TEXT NOT NULL,
    activity_name TEXT NOT NULL,
    user_handle_id INTEGER NOT NULL DEFAULT 0,
    order_index INTEGER NOT NULL,
    custom_label TEXT,
    FOREIGN KEY(group_id) REFERENCES app_groups(id) ON DELETE CASCADE
);
CREATE INDEX index_app_group_items_group_id ON app_group_items(group_id);
```

### 3. `dock_items` Table
```sql
CREATE TABLE dock_items (
    position INTEGER PRIMARY KEY NOT NULL, -- 0 to 4
    package_name TEXT NOT NULL,
    activity_name TEXT NOT NULL,
    user_handle_id INTEGER NOT NULL DEFAULT 0,
    custom_label TEXT
);
```

### 4. `muted_app_rules` Table
```sql
CREATE TABLE muted_app_rules (
    package_name TEXT PRIMARY KEY NOT NULL,
    rule_type TEXT NOT NULL, -- 'HIDE', 'SNOOZE_1H', 'SNOOZE_UNTIL_TOMORROW', 'SNOOZE_PERMANENT'
    snoozed_until_timestamp INTEGER,
    auto_dismiss INTEGER NOT NULL DEFAULT 0
);
```

### 5. `icon_pack_overrides` Table
```sql
CREATE TABLE icon_pack_overrides (
    component_name TEXT PRIMARY KEY NOT NULL, -- "pkg/activity"
    icon_pack_package_name TEXT NOT NULL,
    drawable_name TEXT NOT NULL
);
```

### DAOs
- `AppGroupDao`: CRUD for groups and group items with Flow queries.
- `DockDao`: Observe 5 dock slots, insert/delete/swap positions.
- `MutedAppDao`: Observe mute rules, snooze expiration query, upsert/delete rules.
- `IconPackOverrideDao`: Query all overrides, set/clear override for component.

---

## 5. Key Screens & UX Flow

1. **Home Screen (`HomeScreen`)**:
   - Edge-to-edge layout with optional status bar immersive mode toggle.
   - **Glance Section**: Displays large clock (12/24h), current date, weather icon + temp (via Open-Meteo, refreshed periodically via WorkManager or tap-to-refresh).
   - **Notification Section**: Active notifications grouped by app. Swipe-to-dismiss invokes `cancelNotification`. One-tap quick mute opens snooze/hide sheet. Clear all button. Privacy mode toggle (hide content text).
   - **Groups Section**: Vertically scrollable collapsible sections (Work, Social, Finance, etc.). Drag-and-drop support to reorder and drop apps into groups.
   - **Dock Section**: Fixed bottom bar with 0–5 pinned apps. Long-press to remove or reorder. Blur/transparent/solid background styling.
   - **All Apps Drawer / Search**: Swipe-up gesture reveals all installed apps with instant search filter, letter index bar, and long-press action menu ("Pin to Dock", "Add to Group", "App Info", "Uninstall").

2. **Group Management Modal / Bottom Sheet**:
   - Create new category, choose title, choose category icon and accent color.
   - Set view mode (grid vs list), column count (3–6), and sort order (A-Z, manual, usage).

3. **Notification Rules & Privacy Screen (`NotificationRulesScreen`)**:
   - Manage currently muted/snoozed applications.
   - Explanation of launcher-level rules vs OS-level notification blocking, with direct deep-link button to Android System Notification Settings (`Settings.ACTION_APP_NOTIFICATION_SETTINGS`).

4. **Icon Pack Picker & Customization (`IconPackPickerScreen`)**:
   - Discover installed icon packs via intent filters (`org.adw.launcher.THEMES`, etc.).
   - Preview icon pack, apply globally.
   - Adaptive icon shape selector (circle, squircle, rounded square).

5. **Settings Screen (`SettingsScreen`)**:
   - Theme options: Light, Dark, System, AMOLED pure black, Dynamic Color toggle.
   - Layout options: Dock blur/transparency, icon size, label visibility.
   - Glance options: 12/24h format, date style, weather units (Celsius/Fahrenheit), manual city fallback.
   - Backup & Restore: Export full configuration (groups, dock, settings) to a JSON file via Storage Access Framework (`ActivityResultContracts.CreateDocument`); Import JSON configuration (`ActivityResultContracts.OpenDocument`).

---

## 6. Implementation Milestones

- **Scaffold**: Project structure, Gradle Kotlin DSL, version catalog, convention plugins, Hilt DI setup, base theme.
- **M1: Launcher Core + App List**:
  - `MainActivity` registered as `HOME`, `MAIN`, `DEFAULT`.
  - `LauncherApps` and `PackageManager` integration.
  - BroadcastReceiver for `PACKAGE_ADDED`, `PACKAGE_REMOVED`, `PACKAGE_CHANGED`.
  - All Apps grid with search and app launching (including multi-profile/work profile).
- **M2: Dock**:
  - `DockDao` and Room table.
  - 0–5 app capacity enforcement.
  - Long press menu and drag-to-reorder.
  - Background styling (blur with Haze/RenderEffect on API 31+, solid/translucent fallback).
- **M3: App Groups / Drawers**:
  - `AppGroupDao` and `AppGroupItemDao`.
  - Default groups generation (Work, Social, Media, Tools).
  - Expandable/collapsible accordion UI.
  - Grid/List toggle, column count, sorting.
- **M4: Glance**:
  - Live clock & date composables.
  - Open-Meteo Ktor weather client.
  - FusedLocationProviderClient integration + manual fallback.
  - `WeatherRefreshWorker` using WorkManager.
  - Immersive mode status bar toggle via `WindowInsetsControllerCompat`.
- **M5: Notification Listener + Drawer + Quick Mute**:
  - `DrawerNotificationListener` service (`BIND_NOTIFICATION_LISTENER_SERVICE`).
  - Onboarding permission flow (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`).
  - Live notification grouping, dismissal, quick mute / snooze rules.
  - Deep links to system notification settings.
- **M6: Icon Pack Support**:
  - Intent querying for third-party icon packs.
  - XML parsing of `appfilter.xml`.
  - Coil custom fetcher / decoder for icon pack resources.
  - Per-app icon override sheet.
- **M7: Settings, Personalisation, Backup & Restore**:
  - Central settings UI with DataStore preferences.
  - Theme switching (Light/Dark/AMOLED/Material You).
  - JSON serializer for backup & restore via Storage Access Framework.
- **M8: Polish, Performance, Baseline Profiles, Accessibility, Edge Cases, CI**:
  - TalkBack accessibility content descriptions and touch target auditing.
  - Compose strong skipping verification & stability keys.
  - Unit tests for repositories, DAOs, and view models.
  - Detekt and Android Lint verification.
  - GitHub Actions CI workflow configuration.
