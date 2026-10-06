# UX Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 12 comprehensive UX and usability improvements across Drawer: eliminate jarring 2-second auto-close timers, resolve accordion 2-column expansion crowding and dock overlap, enable full-screen vertical swipe gestures for opening All Apps, replace the desktop `AlertDialog` with a sleek floating context menu, remove crowded micro-touch targets from category headers, enhance the A-Z alphabet scrubber with an interactive magnifier preview, support search keyboard auto-focus, theme floating pill snackbars, polish dock management sheets, and make status and selfie widgets clean and configurable.

**Architecture:** 
- **Drawer Interaction & Gestures:** Remove `autoCloseOnInactivity` from `GroupsAccordion` and `NotificationDrawer`. Elevate swipe detection to root home container so vertical drag anywhere on empty wallpaper fluidly opens All Apps or expands notifications.
- **Category & Accordion Layout:** Redesign category accordion expansion. When collapsed, categories remain compact dual-pills (if side-by-side) or full-width pills; when expanded in dual-column mode, enforce comfortable max-height with internal scrolling, or expand to full width grid with 4 columns, preventing layout distortion and clipping past the dock. Simplify category headers to single-tap toggle and long-press to edit, removing the tiny 3-dots menu button and accidental mis-taps.
- **Desktop Context Menu:** Replace modal `AlertDialog` with an elegant custom floating menu/sheet anchored to desktop long-press (Wallpaper picker, Add Category, Settings).
- **All Apps Drawer & Scrubber:** Add high-visibility magnifying letter bubble with density-scaled offset in `AlphabetIndexBar`. Add `autoOpenKeyboardInDrawer` preference in DataStore and auto-request focus on search field in `AllAppsDrawer`.
- **Dock & Floating Polish:** Clean up wording in `DockActionBottomSheet` (remove confusing labels, add Replace App action). Style `SnackbarHost` as a floating dark/accent frosted pill rather than full-width white rectangle. Allow toggling `DuoStatusIcon` or auto-hiding when Android status bar is visible.

**Tech Stack:** Jetpack Compose, Material 3, Kotlin Coroutines & Flow, Jetpack DataStore, Hilt.

---

### Task 1: Eliminate 2-Second Inactivity Auto-Close Timer

**Files:**
- Modify: `feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/GroupsAccordion.kt`
- Modify: `feature/notifications/src/main/kotlin/com/urunkarpm/drawer/feature/notifications/NotificationDrawer.kt`

- [ ] Remove `.autoCloseOnInactivity(...)` from `GroupCard` in `GroupsAccordion.kt`.
- [ ] Remove `.autoCloseOnInactivity(...)` from `NotificationDrawer.kt`.
- [ ] Verify unit tests pass and accordion drawers remain open until explicitly toggled or dismissed.

---

### Task 2: Fix Accordion 2-Column Expansion Crowding & Dock Collision

**Files:**
- Modify: `feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/GroupsAccordion.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`

- [ ] In `GroupsAccordion.kt`:
  - When a category expands in `twoDrawersSideBySide` mode, ensure the expanded card has a constrained max height (e.g. `Modifier.heightIn(max = 420.dp)`) with internal scrolling if items exceed the view, OR adapt the expanded section to span full width across both columns so apps render in 3-4 spacious columns without truncating labels (`LibreOffice Viewer`, etc.).
  - Add generous bottom clearance spacer so expanded drawers never clip behind or overlap the dock bar.
- [ ] In `HomeScreen.kt`: Ensure bottom padding and scroll state accommodate expanded category heights smoothly.

---

### Task 3: Enable Reliable Full-Screen Swipe-to-Open Gesture for All Apps

**Files:**
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`

- [ ] Update `HomeScreen.kt` empty-space gesture detector:
  - Add vertical drag detection on the home screen background (not just the dock pill).
  - Swiping UP anywhere on wallpaper or between categories triggers `viewModel.openAllApps()`.
  - Swiping DOWN when notifications exist expands the notification drawer.
  - Preserve double-tap to lock and long-press for desktop menu.

---

### Task 4: Modernize Desktop Long-Press Menu (Replace AlertDialog)

**Files:**
- Create: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/HomeContextMenuSheet.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`

- [ ] Create `HomeContextMenuSheet.kt` using `ModalBottomSheet` or floating popup card with:
  - 🎨 "Change Wallpaper" (launches `Intent(Intent.ACTION_SET_WALLPAPER)`)
  - ➕ "Add Category" (triggers category creation)
  - ⚙️ "Launcher Settings" (opens Settings overlay)
  - 📱 "System Settings" (opens Android system settings)
- [ ] In `HomeScreen.kt`: Replace `AlertDialog` with `HomeContextMenuSheet`.

---

### Task 5: Streamline Category Header Touch Targets (Remove Micro 3-Dots Button)

**Files:**
- Modify: `feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/GroupsAccordion.kt`

- [ ] Remove the tiny 3-dots overflow button from the category header row.
- [ ] Add `combinedClickable` on the category header surface:
  - **Single tap:** Expands/collapses the accordion.
  - **Long press:** Triggers `onEditGroup()` (or Category options bottom sheet).
- [ ] Retain clear visual chevron and count badge with ample touch padding.

---

### Task 6: Alphabet Scroller Interactive Magnifier Bubble

**Files:**
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AlphabetIndexBar.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AllAppsDrawer.kt`

- [ ] In `AlphabetIndexBar.kt`:
  - Calculate magnifier bubble offset in dp (`-68.dp`) so it floats visibly to the left of the user's thumb regardless of screen density.
  - Add animated scale/fade pop-in when scrubbing starts.
  - Ensure bubble renders in front of other content.

---

### Task 7: Auto-Focus Keyboard Preference for All Apps Search

**Files:**
- Modify: `core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt`
- Modify: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsScreen.kt`
- Modify: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsViewModel.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModel.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AllAppsDrawer.kt`

- [ ] Add `autoOpenKeyboardInDrawer: Boolean` preference (default `false`).
- [ ] Add setting toggle under "Appearance & Display" or "App Categories & Drawers".
- [ ] In `AllAppsDrawer.kt`: When opened with preference enabled, auto-request focus on `TextField` using `FocusRequester`.

---

### Task 8: Themed Floating Pill Snackbars

**Files:**
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`

- [ ] Customise `SnackbarHost` in `HomeScreen.kt`:
  - Style snackbars with a dark frosted pill surface (`RoundedCornerShape(24.dp)`), Material Theme primary/surface colors, and elevation.
  - Position snackbars comfortably above the dock so they do not obstruct dock icons.

---

### Task 9: Polish Dock Action Sheet & Clean Labels

**Files:**
- Modify: `feature/dock/src/main/kotlin/com/urunkarpm/drawer/feature/dock/component/DockActionBottomSheet.kt`
- Modify: `feature/dock/src/main/kotlin/com/urunkarpm/drawer/feature/dock/DockBar.kt`

- [ ] In `DockActionBottomSheet.kt`:
  - Remove confusing `"Edit dock (show - remove badges)"` text and manual `"Move right"` button.
  - Provide crisp, modern options: "Replace App", "Remove from Dock", "App Info", and "Uninstall".
- [ ] In `DockBar.kt`: Clean up empty dock placeholder text to be subtle and minimal.

---

### Task 10: Status Bar & Duo Status Icon Redundancy Cleanup

**Files:**
- Modify: `core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/GlanceHeader.kt`
- Modify: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsScreen.kt`
- Modify: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsViewModel.kt`

- [ ] Add `showDuoStatusWidget` preference (default: only show when status bar is hidden, or user configurable).
- [ ] In `GlanceHeader.kt`: conditionally render `DuoStatusIcon` based on preference so users with visible system status bars don't see duplicate battery/wifi indicators.

---

### Task 11: Front Camera Quick Mirror Setting & Trigger Safeguard

**Files:**
- Modify: `core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt`
- Modify: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsScreen.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`

- [ ] Add `enableCameraMirror` preference toggle in Settings under "At A Glance".
- [ ] Only enable the camera cutout tap target when `enableCameraMirror` is true, preventing accidental triggers when pulling down system notification shade.

---

### Task 12: Verification & Test Suite Execution

**Files:**
- Run: `./gradlew testDebugUnitTest`
- Assemble: `./gradlew assembleDebug`

- [ ] Run full test suite across all modules.
- [ ] Verify zero compilation or lint errors.
