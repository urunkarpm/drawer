# Alphabet Fast-Scroll, App Shortcuts, and Double-Tap to Lock Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 3 high-impact native launcher features in Drawer: Fast Alphabet Jumplist in AllAppsDrawer, Deep App Shortcuts in AppActionBottomSheet, and Double-Tap on Empty Home Space to Lock Screen.

**Architecture:** Leverage native Android framework APIs (LauncherApps shortcuts API, AccessibilityService global action lock) and Jetpack Compose pointer/scroll mechanisms without adding external dependencies, maintaining 120 FPS performance.

**Tech Stack:** Kotlin, Jetpack Compose, Android LauncherApps API, Android AccessibilityService, Hilt, Coroutines.

## Global Constraints
- Target 120 FPS: no recomposition storms or main-thread blocking IPC.
- YAGNI: no unnecessary abstractions or third-party libraries.
- Backward compatibility: gracefully handle permissions and API level differences.
- Keep all unit tests passing and verify on wired ADB device.

---

### Task 1: Alphabet Scrollbar / Fast Jumplist in AllAppsDrawer

**Files:**
- Create: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AlphabetIndexBar.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AllAppsDrawer.kt`

- [ ] **Step 1: Create AlphabetIndexBar composable**
Create a lightweight touch/drag index strip component that renders letters and triggers haptic feedback and an index callback on change.

- [ ] **Step 2: Integrate AlphabetIndexBar into AllAppsDrawer**
Attach index bar to the right edge of `AllAppsDrawer`. When touched, calculate the first item in `apps` starting with that letter and call `gridState.scrollToItem(index)`. Show a letter preview bubble while dragging.

- [ ] **Step 3: Verify build and touch performance**
Run `./gradlew :feature:home:assembleDebug` and ensure compilation passes.

---

### Task 2: Deep App Shortcuts in AppActionBottomSheet

**Files:**
- Create: `core/model/src/main/kotlin/com/urunkarpm/drawer/core/model/AppShortcutInfo.kt`
- Modify: `core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/repository/AppRepository.kt`
- Modify: `core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/repository/AppRepositoryImpl.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModel.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AppActionBottomSheet.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`

- [ ] **Step 1: Create AppShortcutInfo model**
Define data class `AppShortcutInfo(val id: String, val packageName: String, val shortLabel: String, val longLabel: String? = null, val isEnabled: Boolean = true)`.

- [ ] **Step 2: Add shortcut APIs to AppRepository and AppRepositoryImpl**
Add `getShortcuts(app: AppInfo): List<AppShortcutInfo>`, `launchShortcut(app: AppInfo, shortcutId: String): Boolean`, and `getShortcutIcon(app: AppInfo, shortcutId: String): Drawable?` using `LauncherApps`.

- [ ] **Step 3: Expose shortcuts in HomeViewModel and state**
When an app menu opens, query shortcuts and expose them in `HomeUiState`. Add `launchShortcut(shortcutId: String)` to ViewModel.

- [ ] **Step 4: Display shortcuts in AppActionBottomSheet**
Render dynamic/manifest shortcuts at top of sheet with icons and click listeners.

- [ ] **Step 5: Run unit tests**
Run `./gradlew :core:data:testDebugUnitTest :feature:home:testDebugUnitTest`.

---

### Task 3: Double-Tap on Empty Space to Lock Screen

**Files:**
- Create: `app/src/main/kotlin/com/urunkarpm/drawer/service/DrawerAccessibilityService.kt`
- Create: `app/src/main/res/xml/accessibility_service_config.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModel.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`

- [ ] **Step 1: Implement DrawerAccessibilityService**
Implement accessibility service with `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` and static companion helpers to check state and trigger screen lock.

- [ ] **Step 2: Declare service in AndroidManifest.xml and res/xml**
Add accessibility service declaration and XML config.

- [ ] **Step 3: Wire double-tap gesture in HomeScreen.kt**
Add `onDoubleTap` to `detectTapGestures` on empty home screen. Call lock helper; if service not enabled, notify user with option to open Accessibility Settings.

- [ ] **Step 4: Build, test, and install via wired ADB**
Run `./gradlew assembleDebug testDebugUnitTest` and install via `adb -s 10BG4Y0TDS001TD install -r app/build/outputs/apk/debug/app-debug.apk`.
