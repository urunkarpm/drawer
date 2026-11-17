# UI/UX Design System & Ergonomic Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Elevate Drawer's visual consistency, touch ergonomics, and tactile responsiveness through unified bottom sheet editing, monochrome priority adaptation, query search highlighting, 48dp WCAG touch targets, and holistic micro-haptics.

**Architecture:** Build on existing Compose Material 3 primitives, `CrispHaptics`, and `FluidSpringPhysics`. Defer all non-essential visual recalculations to draw phase or background coroutines with zero GPU recomposition churn.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Compose Animation / Spring Physics, CrispHaptics, AndroidX Navigation / DataStore.

**Spec:** [UI/UX Design System Specification](file:///C:/Users/uprasenjeet/Documents/Drawer/docs/superpowers/plans/2026-11-17-ui-ux-design-system-polish.md)

## Global Constraints
- Strictly adhere to WCAG AA accessibility standards (minimum 4.5:1 text contrast, minimum 48×48dp touch target hit area).
- Follow senior developer "ponytail" mode: build the clean, minimal solution that works with standard Compose primitives, zero unneeded dependencies, zero 120Hz continuous recomposition churn, and mark deliberate simplifications with ponytail comments naming ceilings and upgrade paths.
- Preserve existing data schemas and preference keys; add backwards-compatible preferences only when needed.

---

### Task 1: Touch Target & Color Swatch Ergonomics in Group & Widget Edit Sheets

**Files:**
- Modify: `feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/component/EditGroupDialog.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/EditWidgetDrawerDialog.kt`

**Interfaces:**
- Consumes: `GroupIcons.availableColors`, `GroupIcons.availableIcons`, `LocalCrispHaptics`
- Produces: 48dp minimum touch bounding boxes with centered 36dp visual swatches, micro-haptic tick on color/icon select

- [ ] **Step 1: Expand touch targets for color swatches in EditGroupDialog**
Wrap each color circle in a `Box(modifier = Modifier.size(48.dp).clickable { crispHaptics.tick(); selectedColor = colorHex }, contentAlignment = Alignment.Center)` with the visual circle at `36.dp`.

- [ ] **Step 2: Expand touch targets for icon selection in EditGroupDialog**
Ensure the icon button container enforces `Modifier.size(48.dp)` with centered 40dp visual chip and fires `crispHaptics.tick()` on click.

- [ ] **Step 3: Mirror touch target and haptic updates in EditWidgetDrawerDialog**
Apply the identical 48dp touch bounds and `crispHaptics.tick()` triggers to color and icon grids in `EditWidgetDrawerDialog.kt`.

- [ ] **Step 4: Verify touch target and visual alignment**
Run `./gradlew.bat :feature:groups:compileDebugKotlin :feature:home:compileDebugKotlin` and confirm error-free compilation.

- [ ] **Step 5: Commit changes**
```bash
git add feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/component/EditGroupDialog.kt feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/EditWidgetDrawerDialog.kt
git commit -m "style: upgrade edit group and widget dialogs with 48dp touch targets and micro-haptics"
```

---

### Task 2: Todo Canvas Monochrome Adaptation & Tactile Completion Pop

**Files:**
- Modify: `feature/todo/src/main/kotlin/com/urunkarpm/drawer/feature/todo/component/TodoHeroFocusCard.kt`
- Modify: `feature/todo/src/main/kotlin/com/urunkarpm/drawer/feature/todo/component/TodoItemRow.kt`
- Modify: `feature/todo/src/main/kotlin/com/urunkarpm/drawer/feature/todo/TodoScreen.kt`

**Interfaces:**
- Consumes: `LocalMonochromeIcons`, `LocalCrispHaptics`, `TodoItem`, `TodoPriority`
- Produces: Editorial monochrome priority stripe/border tinting in Monochrome mode, tactile completion pop with `CrispHaptics.success()`

- [ ] **Step 1: Adapt priority colors in TodoHeroFocusCard and TodoItemRow for Monochrome Vibe**
Check `LocalMonochromeIcons.current`. When true, map `TodoPriority.HIGH`, `NORMAL`, and `LOW` to tonal grayscale tokens (High: `MaterialTheme.colorScheme.onSurface`, Normal: `onSurface.copy(alpha = 0.65f)`, Low: `onSurface.copy(alpha = 0.35f)`) so monochrome aesthetic is strictly honored.

- [ ] **Step 2: Add tactile completion feedback to Todo completion button**
In `TodoHeroFocusCard` and `TodoItemRow`, trigger `crispHaptics.success()` when the completion checkmark is pressed, accompanied by a subtle spring bounce (`animateFloatAsState` scale 0.85f -> 1.0f).

- [ ] **Step 3: Enhance empty state in TodoScreen**
Add a clean, minimalist typography and subtle outline icon empty state when all tasks are complete ("All clear / Focus restored") with a direct `+ Add task` quick button.

- [ ] **Step 4: Verify unit tests**
Run `./gradlew.bat :feature:todo:testDebugUnitTest`.

- [ ] **Step 5: Commit changes**
```bash
git add feature/todo/src/main/kotlin/com/urunkarpm/drawer/feature/todo/
git commit -m "feat(todo): adapt priority colors to monochrome vibe and add tactile completion haptics"
```

---

### Task 3: Matched Query Character Highlighting & Micro-Haptic Scrubber in AllAppsDrawer

**Files:**
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AllAppsDrawer.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AlphabetIndexBar.kt`

**Interfaces:**
- Consumes: `searchQuery`, `AppInfo.label`, `LocalCrispHaptics`
- Produces: `AnnotatedString` highlighting matching query substrings in primary accent color, tactile haptic tick when dragging across alphabet index letters

- [ ] **Step 1: Implement matched query highlighter helper**
Build a memoized `buildHighlightedLabel(label: String, query: String, primaryColor: Color): AnnotatedString` function using `SpanStyle(fontWeight = FontWeight.ExtraBold, color = primaryColor)` on matched substring indices.

- [ ] **Step 2: Apply highlighted label in AllAppsDrawer search results**
When `searchQuery.isNotBlank()`, pass the `AnnotatedString` to the app item text composable instead of plain string.

- [ ] **Step 3: Add micro-haptic ticks to AlphabetIndexBar drag navigation**
In `AlphabetIndexBar.kt`, trigger `crispHaptics.tick()` whenever the active scrubbed character changes index.

- [ ] **Step 4: Verify build and performance**
Run `./gradlew.bat :feature:home:testDebugUnitTest`.

- [ ] **Step 5: Commit changes**
```bash
git add feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AllAppsDrawer.kt feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AlphabetIndexBar.kt
git commit -m "feat(home): add search query highlighting and alphabet scrubber micro-haptics"
```

---

### Task 4: Glance Header Optional Frosted Island Backdrop & Dynamic Contrast Tuning

**Files:**
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/GlanceHeader.kt`
- Modify: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/subscreen/LookSubScreens.kt`
- Modify: `core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt`

**Interfaces:**
- Consumes: `glanceFrostedBackdrop: Boolean`, `rememberWallpaperContrastColors`
- Produces: Ultra-subtle frosted glass pill backdrop option (`Brush.verticalGradient` with 0.75dp specular hairline) ensuring WCAG AA contrast over busy photographic wallpapers

- [ ] **Step 1: Add preference for Glance frosted backdrop in DrawerPreferencesDataSource**
Add `GLANCE_FROSTED_BACKDROP = booleanPreferencesKey("glance_frosted_backdrop")` with default `false`.

- [ ] **Step 2: Render optional hairline glass pill in GlanceHeader**
When enabled, wrap the header content in a subtle `DockSurfaceBackground` or hairline pill with `shape = RoundedCornerShape(24.dp)` and `padding(horizontal = 14.dp, vertical = 8.dp)`.

- [ ] **Step 3: Expose toggle in Appearance Settings**
Add a setting switch in `LookSubScreens.kt` under Appearance -> Glance Header.

- [ ] **Step 4: Verify compilation and tests**
Run `./gradlew.bat :feature:home:compileDebugKotlin :feature:settings:compileDebugKotlin`.

- [ ] **Step 5: Commit changes**
```bash
git add core/datastore/ feature/home/ feature/settings/
git commit -m "feat(glance): add optional frosted hairline backdrop for guaranteed wallpaper legibility"
```

---

### Task 5: End-to-End Visual QA & Regression Test Suite

**Files:**
- Test: `feature/home/src/test/`
- Test: `feature/todo/src/test/`
- Test: `feature/groups/src/test/`

- [ ] **Step 1: Execute all unit and screenshot tests across modified modules**
Run: `./gradlew.bat testDebugUnitTest`

- [ ] **Step 2: Validate zero warnings or regressions**
Verify test results report 100% pass rate.
