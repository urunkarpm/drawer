# Hidden Drawer Vault & Discreet Privacy Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Build a secure Hidden Drawer Vault opened via an elastic resistance swipe from the last dock app with BiometricPrompt security, global app/search filtering, notification suppression, and an ambient Material You glowing notification bell indicator for the first 2 unlocks.

**Architecture:** Data layer stores hidden package names and an unlock counter in DataStore. DockBar detects dual-direction over-drag with logarithmic spring physics and haptics to trigger Android BiometricPrompt. On authentication, a Frosted Liquid Glass HiddenVaultDrawer overlay opens. Notifications from hidden packages are suppressed from the normal notification drawer and trigger a 2-unlock breathing glow on the drawer's notification bell.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX Biometric (`BiometricPrompt`), DataStore Preferences, Kotlinx Coroutines/Flow, Material 3, JUnit 4, MockK.

**Spec:** [docs/superpowers/specs/2026-11-14-hidden-drawer-vault-design.md](file:///C:/Users/uprasenjeet/Documents/Drawer/docs/superpowers/specs/2026-11-14-hidden-drawer-vault-design.md)

## Global Constraints
- Target SDK: 35, Min SDK: 26.
- Hardware curve integration via `DeviceScreenCurve`.
- Tactile haptics using `CrispHaptics`.
- Spring animation physics using `FluidSpringPhysics`.
- Clean architecture with zero regressions on existing dock drag-to-reorder and home paging.

---

### Task 1: Preferences & Persistence for Hidden Apps and Unlock Counter

**Files:**
- Modify: `core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt`
- Modify: `core/model/src/main/kotlin/com/urunkarpm/drawer/core/model/LauncherBackup.kt`
- Test: `core/datastore/src/test/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSourceTest.kt`

**Interfaces:**
- Produces:
  - `DrawerPreferencesDataSource.hiddenAppPackages: Flow<Set<String>>`
  - `DrawerPreferencesDataSource.addHiddenApp(packageName: String): Unit`
  - `DrawerPreferencesDataSource.removeHiddenApp(packageName: String): Unit`
  - `DrawerPreferencesDataSource.setHiddenApps(packages: Set<String>): Unit`
  - `DrawerPreferencesDataSource.hiddenNotifUnlockCount: Flow<Int>`
  - `DrawerPreferencesDataSource.setHiddenNotifUnlockCount(count: Int): Unit`
  - `DrawerPreferencesDataSource.decrementHiddenNotifUnlockCount(): Unit`

- [x] **Step 1: Write the failing unit tests**

In `core/datastore/src/test/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSourceTest.kt`, add:
```kotlin
    @Test
    fun testHiddenAppsAndUnlockCountPreferences() = runTest {
        assertEquals(emptySet<String>(), dataSource.hiddenAppPackages.first())
        assertEquals(0, dataSource.hiddenNotifUnlockCount.first())

        dataSource.addHiddenApp("com.secret.chat")
        assertEquals(setOf("com.secret.chat"), dataSource.hiddenAppPackages.first())

        dataSource.addHiddenApp("com.private.vault")
        assertEquals(setOf("com.secret.chat", "com.private.vault"), dataSource.hiddenAppPackages.first())

        dataSource.removeHiddenApp("com.secret.chat")
        assertEquals(setOf("com.private.vault"), dataSource.hiddenAppPackages.first())

        dataSource.setHiddenNotifUnlockCount(2)
        assertEquals(2, dataSource.hiddenNotifUnlockCount.first())

        dataSource.decrementHiddenNotifUnlockCount()
        assertEquals(1, dataSource.hiddenNotifUnlockCount.first())

        dataSource.decrementHiddenNotifUnlockCount()
        assertEquals(0, dataSource.hiddenNotifUnlockCount.first())

        dataSource.decrementHiddenNotifUnlockCount()
        assertEquals(0, dataSource.hiddenNotifUnlockCount.first())
    }
```

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :core:datastore:testDebugUnitTest --tests "com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSourceTest.testHiddenAppsAndUnlockCountPreferences"`
Expected: Compilation failure or method not found.

- [x] **Step 3: Implement minimal preferences in `DrawerPreferencesDataSource.kt` and `LauncherBackup.kt`**

In `core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt`:
```kotlin
    private val HIDDEN_APP_PACKAGES = stringSetPreferencesKey("hidden_app_packages")
    private val HIDDEN_NOTIF_UNLOCK_COUNT = intPreferencesKey("hidden_notif_unlock_count")

    val hiddenAppPackages: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[HIDDEN_APP_PACKAGES] ?: emptySet()
    }

    val hiddenNotifUnlockCount: Flow<Int> = dataStore.data.map { preferences ->
        preferences[HIDDEN_NOTIF_UNLOCK_COUNT] ?: 0
    }

    suspend fun addHiddenApp(packageName: String) {
        dataStore.edit { preferences ->
            val current = preferences[HIDDEN_APP_PACKAGES] ?: emptySet()
            preferences[HIDDEN_APP_PACKAGES] = current + packageName
        }
    }

    suspend fun removeHiddenApp(packageName: String) {
        dataStore.edit { preferences ->
            val current = preferences[HIDDEN_APP_PACKAGES] ?: emptySet()
            preferences[HIDDEN_APP_PACKAGES] = current - packageName
        }
    }

    suspend fun setHiddenApps(packages: Set<String>) {
        dataStore.edit { preferences ->
            preferences[HIDDEN_APP_PACKAGES] = packages
        }
    }

    suspend fun setHiddenNotifUnlockCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[HIDDEN_NOTIF_UNLOCK_COUNT] = count.coerceAtLeast(0)
        }
    }

    suspend fun decrementHiddenNotifUnlockCount() {
        dataStore.edit { preferences ->
            val current = preferences[HIDDEN_NOTIF_UNLOCK_COUNT] ?: 0
            if (current > 0) {
                preferences[HIDDEN_NOTIF_UNLOCK_COUNT] = current - 1
            }
        }
    }
```
Update `LauncherBackup.kt` to include `val hiddenAppPackages: Set<String> = emptySet()`.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :core:datastore:testDebugUnitTest --tests "com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSourceTest.testHiddenAppsAndUnlockCountPreferences"`
Expected: BUILD SUCCESSFUL and test PASSES.

- [x] **Step 5: Commit**

```powershell
git add core/datastore core/model
git commit -m "feat(datastore): add preferences for hidden apps and unlock counter"
```

---

### Task 2: Dock Elastic Resistance Gesture & Breakpoint Trigger

**Files:**
- Modify: `feature/dock/src/main/kotlin/com/urunkarpm/drawer/feature/dock/DockBar.kt`
- Test: Manual gesture check & build verification

**Interfaces:**
- Consumes:
  - `CrispHaptics.tick()`, `CrispHaptics.confirm()`
- Produces:
  - `DockBar(..., onTriggerHiddenVault: () -> Unit = {})` callback invoked when user pulls past 75dp threshold.

- [x] **Step 1: Implement resistance physics and gesture detection in `DockBar.kt`**

In `feature/dock/src/main/kotlin/com/urunkarpm/drawer/feature/dock/DockBar.kt`:
1. Add `onTriggerHiddenVault: () -> Unit = {}` to `DockBar` parameters.
2. In the `pointerInput` on the dock slots, when a horizontal drag originates on or beyond the last app slot (`visibleApps.size - 1`):
   - Track `vaultPullDelta: Float`.
   - Apply logarithmic resistance:
     `val effectivePullOffset = sign(vaultPullDelta) * (75f * (1f - exp(-abs(vaultPullDelta) / 110f)))`
   - Trigger `crispHaptics.tick()` every 18dp of displacement.
   - When `abs(effectivePullOffset) >= 65f` (or raw pull >= 140px):
     - Play `crispHaptics.confirm()`.
     - Trigger `onTriggerHiddenVault()`.
   - On release (`onDragEnd`, `onDragCancel`), animate `vaultPullDelta` back to `0f` using spring physics.
3. Apply `graphicsLayer { translationX = effectivePullOffset }` to the last app slot or dock row.

- [x] **Step 2: Compile `:feature:dock` to verify implementation**

Run: `.\gradlew.bat :feature:dock:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [x] **Step 3: Commit**

```powershell
git add feature/dock
git commit -m "feat(dock): add elastic resistance gesture and breakpoint trigger for hidden vault"
```

---

### Task 3: Global App Filtering & Vault State in HomeViewModel

**Files:**
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModel.kt`
- Test: `feature/home/src/test/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes:
  - `DrawerPreferencesDataSource.hiddenAppPackages`
  - `DrawerPreferencesDataSource.hiddenNotifUnlockCount`
- Produces:
  - `HomeUiState.hiddenAppPackages: Set<String>`
  - `HomeUiState.isVaultOpen: Boolean`
  - `HomeUiState.hiddenNotifUnlockCount: Int`
  - `HomeViewModel.openVault()`
  - `HomeViewModel.closeVault()`
  - `HomeViewModel.addHiddenApp(pkg: String)`
  - `HomeViewModel.removeHiddenApp(pkg: String)`

- [x] **Step 1: Write unit tests in `HomeViewModelTest.kt`**

Test that:
1. `allApps` emitted by `HomeViewModel` excludes packages in `hiddenAppPackages`.
2. `openVault()` sets `isVaultOpen = true` and resets `hiddenNotifUnlockCount = 0`.
3. `closeVault()` sets `isVaultOpen = false`.
4. Adding an app to hidden packages removes it from `allApps`.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :feature:home:testDebugUnitTest --tests "com.urunkarpm.drawer.feature.home.HomeViewModelTest.testHiddenAppsExclusion"`
Expected: FAIL.

- [x] **Step 3: Implement vault state and filtering in `HomeViewModel.kt`**

1. Collect `preferencesDataSource.hiddenAppPackages` and combine with app list flows.
2. In `allApps` computation:
   `val filteredApps = rawApps.filter { it.packageName !in hiddenAppPackages }`
3. In `suggestedApps`:
   Filter out hidden packages.
4. Add `isVaultOpen` state flow and helper methods:
   - `openVault()`: sets `_isVaultOpen.value = true`, launches `preferencesDataSource.setHiddenNotifUnlockCount(0)`.
   - `closeVault()`: sets `_isVaultOpen.value = false`.
   - `addHiddenApp(pkg: String)`: persists to DataStore, unpins from dock if present.
   - `removeHiddenApp(pkg: String)`: removes from DataStore.

- [x] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :feature:home:testDebugUnitTest --tests "com.urunkarpm.drawer.feature.home.HomeViewModelTest.testHiddenAppsExclusion"`
Expected: PASS.

- [x] **Step 5: Commit**

```powershell
git add feature/home
git commit -m "feat(home): add vault state and global hidden app filtering in HomeViewModel"
```

---

### Task 4: Hidden Vault Drawer UI & BiometricPrompt Integration

**Files:**
- Create: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/HiddenVaultDrawer.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`
- Test: Compile & visual inspection

**Interfaces:**
- Consumes:
  - `HomeUiState.hiddenAppPackages`
  - `HomeViewModel.openVault()`, `closeVault()`, `addHiddenApp()`, `removeHiddenApp()`
  - `DeviceScreenCurve.toRoundedCornerShape()`
- Produces:
  - `HiddenVaultDrawer` composable overlay in `HomeScreen`.
  - Android `BiometricPrompt` triggering on dock elastic breakpoint.

- [x] **Step 1: Create `HiddenVaultDrawer.kt`**

Build `HiddenVaultDrawer.kt`:
1. Fullscreen or bottom-sheet overlay with Frosted Liquid Glass surface and hardware display curve.
2. Header with Shield/Lock icon, "Private Vault", app count badge, search filter, and "+ Add Apps" button.
3. App Grid: displays `hiddenApps` with launcher app theme, launch on click, and long-press menu for "Unhide App" and "App Info".
4. "Add Apps" Sheet: Searchable dialog listing non-hidden installed apps with checkboxes and "Hide Selected" button.
5. BackHandler: closes vault on back gesture.

- [x] **Step 2: Wire `BiometricPrompt` and `HiddenVaultDrawer` into `HomeScreen.kt`**

1. Implement `launchVaultBiometricAuth(context: Context, onSuccess: () -> Unit)` using `androidx.biometric.BiometricPrompt`:
   - Authenticators: `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`.
   - Title: "Unlock Private Vault".
2. In `DockBar(..., onTriggerHiddenVault = { launchVaultBiometricAuth(context) { viewModel.openVault() } })`.
3. Render `HiddenVaultDrawer` when `uiState.isVaultOpen` is true with slide-up and fade transition.

- [x] **Step 3: Compile `:feature:home`**

Run: `.\gradlew.bat :feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [x] **Step 4: Commit**

```powershell
git add feature/home
git commit -m "feat(home): create HiddenVaultDrawer UI and integrate BiometricPrompt"
```

---

### Task 5: Discreet Notification Privacy, Bell Glow & Unlock Counter

**Files:**
- Modify: `feature/notifications/src/main/kotlin/com/urunkarpm/drawer/feature/notifications/NotificationsViewModel.kt`
- Modify: `feature/notifications/src/main/kotlin/com/urunkarpm/drawer/feature/notifications/NotificationDrawer.kt`
- Create: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/UnlockReceiver.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`
- Test: `feature/notifications/src/test/kotlin/com/urunkarpm/drawer/feature/notifications/NotificationsViewModelTest.kt`

**Interfaces:**
- Consumes:
  - `DrawerPreferencesDataSource.hiddenAppPackages`
  - `DrawerPreferencesDataSource.hiddenNotifUnlockCount`
- Produces:
  - Notification suppression for hidden packages.
  - Glowing breathing halo on Notification Drawer bell icon when `hiddenNotifUnlockCount > 0`.
  - `UnlockReceiver` decrementing counter on `ACTION_USER_PRESENT`.

- [x] **Step 1: Write test for hidden notification suppression in `NotificationsViewModelTest.kt`**

Verify that `StatusBarNotification` with `packageName in hiddenAppPackages`:
1. Is not emitted in `activeNotifications`.
2. Triggers `setHiddenNotifUnlockCount(2)`.

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :feature:notifications:testDebugUnitTest --tests "com.urunkarpm.drawer.feature.notifications.NotificationsViewModelTest.testHiddenNotificationSuppression"`
Expected: FAIL.

- [x] **Step 3: Implement notification suppression and glow trigger**

1. In `NotificationsViewModel.kt`, inject `DrawerPreferencesDataSource`.
2. When a notification arrives from a package in `hiddenAppPackages`:
   - Discard from public notification list.
   - Call `preferencesDataSource.setHiddenNotifUnlockCount(2)`.
3. In `NotificationDrawer.kt`:
   - Observe `hiddenNotifUnlockCount`.
   - When `hiddenNotifUnlockCount > 0`:
     - Animate a breathing halo around the bell icon:
       ```kotlin
       val infiniteTransition = rememberInfiniteTransition(label = "bell_glow")
       val glowAlpha by infiniteTransition.animateFloat(
           initialValue = 0.25f,
           targetValue = 0.85f,
           animationSpec = infiniteRepeatable(
               animation = tween(1400, easing = FastOutSlowInEasing),
               repeatMode = RepeatMode.Reverse
           ),
           label = "glowAlpha"
       )
       ```
     - Draw radial ambient glow with `MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)`.

- [x] **Step 4: Implement `UnlockReceiver.kt` in `feature:home`**

1. Create `UnlockReceiver.kt`:
   - BroadcastReceiver registered for `Intent.ACTION_USER_PRESENT`.
   - On receive: calls `preferencesDataSource.decrementHiddenNotifUnlockCount()`.
2. Register and unregister `UnlockReceiver` in `HomeScreen.kt` lifecycle.

- [x] **Step 5: Run tests to verify they pass**

Run: `.\gradlew.bat :feature:notifications:testDebugUnitTest`
Expected: PASS.

- [x] **Step 6: Commit**

```powershell
git add feature/notifications feature/home
git commit -m "feat(notifications): implement discreet notification suppression, bell glow, and unlock receiver"
```

---

### Task 6: Comprehensive Verification & Release Build

**Files:**
- Test all modules: `.\gradlew.bat testDebugUnitTest`
- Assemble Debug APK: `.\gradlew.bat assembleDebug`

- [x] **Step 1: Run full unit test suite**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL across all modules.

- [x] **Step 2: Assemble Debug APK**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL with generated APK in `app/build/outputs/apk/debug/app-debug.apk`.

- [x] **Step 3: Commit and update documentation**

```powershell
git add -A
git commit -m "feat(vault): complete Hidden Drawer Vault with discreet privacy engine"
```
