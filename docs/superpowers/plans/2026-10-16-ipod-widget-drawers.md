# iPod-Style Widget Drawers & Responsive Grid Sizing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete, finished iPod Cover Flow widget drawer system for the left widgets pane in Drawer launcher, supporting category stacks (e.g. Finance, Media), standalone widgets, 4-column responsive grid sizing (2×2, 2×3, 4×2, 4×4), and real-time interactive resizing.

**Architecture:** Extend Room DB with `WidgetDrawerEntity` and updated `WidgetItemEntity` (with `spanX`, `spanY`, `drawerId`). Build an iPod Cover Flow 3D engine using Compose `graphicsLayer` transforms (`rotationY`, `scale`, `translationX`, `zIndex`). Provide a responsive 4-column grid layout for standalone widgets and interactive cell-based resizing with live `AppWidgetHostView` options updates.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3, Foundation Pager & Canvas/GraphicsLayer), Room Database, Hilt, Android AppWidgetHost & AppWidgetManager.

**Spec:** `docs/superpowers/specs/2026-10-16-ipod-widget-drawers-design.md`

## Global Constraints
- Target SDK 35, Min SDK 26, Kotlin 2.0+
- Room DB version bumped to 3; fallback to destructive migration enabled
- Standard 4-column grid division for phone screens
- Android 12+ dynamic widget sizing protocol (`AppWidgetManager.updateAppWidgetOptions`)
- Adhere strictly to the existing frosted glass UI design system

---

### Task 1: Domain & Database Layer (Drawers & Span Dimensions)

**Files:**
- Create: `core/model/src/main/kotlin/com/urunkarpm/drawer/core/model/WidgetDrawer.kt`
- Modify: `core/model/src/main/kotlin/com/urunkarpm/drawer/core/model/WidgetItem.kt`
- Create: `core/database/src/main/kotlin/com/urunkarpm/drawer/core/database/entity/WidgetDrawerEntity.kt`
- Modify: `core/database/src/main/kotlin/com/urunkarpm/drawer/core/database/entity/WidgetItemEntity.kt`
- Modify: `core/database/src/main/kotlin/com/urunkarpm/drawer/core/database/dao/WidgetDao.kt`
- Modify: `core/database/src/main/kotlin/com/urunkarpm/drawer/core/database/DrawerDatabase.kt`
- Modify: `core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/repository/WidgetRepository.kt`
- Modify: `core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/repository/WidgetRepositoryImpl.kt`
- Create: `core/data/src/test/kotlin/com/urunkarpm/drawer/core/data/repository/WidgetRepositoryImplTest.kt`

**Interfaces:**
- Produces:
  - `data class WidgetDrawer(val id: String, val title: String, val orderIndex: Int, val spanX: Int = 4, val spanY: Int = 3, val isExpanded: Boolean = true, val widgets: List<WidgetItem> = emptyList())`
  - `WidgetItem(id: String, appWidgetId: Int, packageName: String, providerClassName: String, drawerId: String? = null, orderIndex: Int = 0, spanX: Int = 4, spanY: Int = 2, heightDp: Int = 180)`
  - `WidgetRepository.getAllDrawers(): Flow<List<WidgetDrawer>>`
  - `WidgetRepository.createDrawer(title: String, spanX: Int = 4, spanY: Int = 3): String`
  - `WidgetRepository.updateDrawer(drawer: WidgetDrawer)`
  - `WidgetRepository.deleteDrawer(id: String, deleteChildren: Boolean)`
  - `WidgetRepository.assignWidgetToDrawer(widgetId: String, drawerId: String?)`

- [ ] **Step 1: Define `WidgetDrawer` and update `WidgetItem` models in `core/model`**
- [ ] **Step 2: Create `WidgetDrawerEntity` and update `WidgetItemEntity` with `drawer_id`, `span_x`, `span_y` in `core/database`**
- [ ] **Step 3: Update `WidgetDao` and `DrawerDatabase` (bump version to 3)**
- [ ] **Step 4: Update `WidgetRepository` and `WidgetRepositoryImpl` to support drawers, items, and assignment**
- [ ] **Step 5: Write unit tests in `WidgetRepositoryImplTest` and verify they pass**
- [ ] **Step 6: Commit changes with message `feat(core): add widget drawers and grid span dimensions to data layer`**

---

### Task 2: HomeViewModel State & Operations

**Files:**
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModel.kt`
- Modify: `feature/home/src/test/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: `WidgetRepository` drawers and widget flows and methods.
- Produces:
  - `HomeUiState.widgetDrawers: List<WidgetDrawer>`
  - `HomeUiState.standaloneWidgets: List<WidgetItem>`
  - `HomeViewModel.createWidgetDrawer(title: String)`
  - `HomeViewModel.deleteWidgetDrawer(id: String, deleteChildren: Boolean)`
  - `HomeViewModel.toggleWidgetDrawerExpand(id: String)`
  - `HomeViewModel.moveWidgetToDrawer(widgetId: String, drawerId: String?)`
  - `HomeViewModel.updateWidgetSpan(widgetId: String, spanX: Int, spanY: Int, heightDp: Int)`
  - `HomeViewModel.reorderWidgetsInDrawer(drawerId: String, fromIndex: Int, toIndex: Int)`

- [ ] **Step 1: Update `HomeUiState` to expose `widgetDrawers` and `standaloneWidgets`**
- [ ] **Step 2: Combine widget repository flows in `HomeViewModel`**
- [ ] **Step 3: Implement drawer management methods (`createWidgetDrawer`, `toggleWidgetDrawerExpand`, `moveWidgetToDrawer`, `updateWidgetSpan`, etc.)**
- [ ] **Step 4: Write unit tests in `HomeViewModelTest` and verify they pass**
- [ ] **Step 5: Commit changes with message `feat(home): add widget drawers and grid resizing state to HomeViewModel`**

---

### Task 3: iPod Cover Flow 3D Widget Stack Component

**Files:**
- Create: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/IpodCoverFlowWidgetStack.kt`

**Interfaces:**
- Consumes: `widgets: List<WidgetItem>`, `widgetHostManager: DrawerWidgetHostManager`, callbacks for click, menu, and resize.
- Produces:
  - `@Composable fun IpodCoverFlowWidgetStack(...)`:
    - Compose `HorizontalPager` with custom `graphicsLayer` 3D Cover Flow transforms
    - Rotation: $Y$-axis tilt $+42^\circ$ for left cards, $-42^\circ$ for right cards, $0^\circ$ for center active card
    - Dynamic $X$-translation for natural tucking/peeking
    - Scale: $1.0\times$ center, $0.82\times$ adjacent, $0.70\times$ outer
    - Z-index: Active card elevated on top
    - Dimming sheen overlay for recessed cards
    - Tapping left or right peek smoothly animates that card to the center front
    - Live `AppWidgetHostView` container with options bundle synchronization

- [ ] **Step 1: Create `IpodCoverFlowWidgetStack.kt` implementing the 3D graphicsLayer math**
- [ ] **Step 2: Add page indicator dots, active title tag, and tap-to-focus animation**
- [ ] **Step 3: Wrap `AppWidgetHostView` with proper padding, clip, and options bundle calculation**
- [ ] **Step 4: Verify component rendering and animations**
- [ ] **Step 5: Commit changes with message `feat(ui): implement ipod cover flow 3d widget stack component`**

---

### Task 4: Interactive Resizer & Responsive 4-Column Grid Feed

**Files:**
- Create: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/WidgetResizeDialog.kt`
- Create: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/WidgetDrawerCard.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/WidgetsPage.kt`

**Interfaces:**
- Consumes: `WidgetDrawer`, `WidgetItem`, `HomeViewModel` operations.
- Produces:
  - `WidgetResizeDialog`: Quick chips (`2×2`, `2×3`, `4×2`, `4×3`, `4×4`) + interactive column/row stepper, live preview.
  - `WidgetDrawerCard`: Frosted glass accordion housing `IpodCoverFlowWidgetStack` with title, widget counter badge, expand/collapse, add to stack, and menu.
  - `WidgetsPage`: 4-column responsive grid flow, pairing consecutive 2-column widgets side-by-side, rendering standalone cards and drawers, and destination selector dialog ("Standalone" vs "Add to Drawer").

- [ ] **Step 1: Create `WidgetResizeDialog.kt` with presets and interactive steppers**
- [ ] **Step 2: Create `WidgetDrawerCard.kt` with frosted glass styling, header controls, and menu**
- [ ] **Step 3: Update `WidgetsPage.kt` to lay out standalone widgets in responsive 4-column grid (2×2 side-by-side, 4×4 full width)**
- [ ] **Step 4: Add "Add Destination" modal ("Standalone Widget" vs "Drawer") on adding new widget**
- [ ] **Step 5: Add context menu actions: "Move to Drawer...", "Extract from Drawer", "Resize Widget", "Delete"**
- [ ] **Step 6: Commit changes with message `feat(ui): responsive widget grid layout, interactive resizer, and drawer cards`**

---

### Task 5: HomeScreen Wiring, Compilation & Live Verification

**Files:**
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`

**Interfaces:**
- Connects widget host allocation, pick launcher, and configure launcher to handle drawer destination and initial spans.

- [ ] **Step 1: Update `HomeScreen.kt` with new `WidgetsPage` parameters and handlers**
- [ ] **Step 2: Run Gradle compilation `./gradlew assembleDebug` and fix any build errors**
- [ ] **Step 3: Run unit tests `./gradlew testDebugUnitTest` and verify all tests pass**
- [ ] **Step 4: Install APK to connected device `10BG4Y0TDS001TD` and verify visually**
- [ ] **Step 5: Commit changes with message `feat(home): complete ipod widget drawers and grid sizing integration`**
