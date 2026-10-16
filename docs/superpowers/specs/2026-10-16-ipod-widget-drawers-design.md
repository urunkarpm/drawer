# Design Spec: iPod-Style Widget Drawers & Responsive Grid Sizing for Drawer Launcher

**Date:** 2026-10-16  
**Status:** Approved  
**Author:** Antigravity  

---

## 1. Overview & Vision
The Left Widgets Pane in Drawer is redesigned from a simple full-width vertical list into a sophisticated, highly customizable widgets dashboard featuring:
1. **Widget Drawers (Stacks):** Ability to group widgets into named categories (e.g., "Finance", "Media", "News") or keep standalone widgets.
2. **iPod Cover Flow 3D Engine:** In each Widget Drawer, widgets are cycled in an authentic iPod Cover Flow style: the active widget is front-and-center, while adjacent widgets are tilted in 3D perspective to the left and right with depth scaling, shading, and smooth flip gestures.
3. **Responsive Grid Sizing (2×2, 2×3, 4×2, 4×4):** Replaces the rigid full-width stretching. 2×2 widgets take 2 of 4 columns (50% screen width), allowing two widgets to sit side-by-side or individually without oversized blank space.
4. **Interactive Resizer:** Real-time cell resizing (spanX: 2–4, spanY: 1–5) that broadcasts Android `AppWidgetManager.OPTION_APPWIDGET_*` size bundles immediately to the widget host view.

---

## 2. Architecture & Data Model

### 2.1 Domain Models (`core/model`)
```kotlin
data class WidgetDrawer(
    val id: String,
    val title: String,
    val orderIndex: Int,
    val spanX: Int = 4,
    val spanY: Int = 3,
    val isExpanded: Boolean = true,
    val widgets: List<WidgetItem> = emptyList()
)

data class WidgetItem(
    val id: String,
    val appWidgetId: Int,
    val packageName: String,
    val providerClassName: String,
    val drawerId: String? = null,
    val orderIndex: Int = 0,
    val spanX: Int = 4, // 2, 3, or 4 columns
    val spanY: Int = 2, // 1 to 5 rows
    val heightDp: Int = 180
)
```

### 2.2 Room Database (`core/database`)
- **`WidgetDrawerEntity`** in `widget_drawers` table:
  - `id: String` (PK)
  - `title: String`
  - `order_index: Int`
  - `span_x: Int`
  - `span_y: Int`
  - `is_expanded: Boolean`
- **`WidgetItemEntity`** in `widgets` table:
  - `id: String` (PK)
  - `app_widget_id: Int`
  - `package_name: String`
  - `provider_class_name: String`
  - `drawer_id: String?` (Indexed)
  - `order_index: Int`
  - `span_x: Int`
  - `span_y: Int`
  - `height_dp: Int`
- **Room Database Migration**: Version 3 in `DrawerDatabase.kt`. `fallbackToDestructiveMigration` already handles fresh schema transitions safely.

### 2.3 Repository & DAO (`core/data`, `core/database`)
- `WidgetDao` methods:
  - `getWidgetDrawers(): Flow<List<WidgetDrawerEntity>>`
  - `getWidgets(): Flow<List<WidgetItemEntity>>`
  - CRUD operations for drawers and widget items.
- `WidgetRepository` & `WidgetRepositoryImpl`:
  - `getAllDrawers(): Flow<List<WidgetDrawer>>`
  - `getAllWidgets(): Flow<List<WidgetItem>>`
  - `createDrawer(title: String, spanX: Int, spanY: Int): String`
  - `updateDrawer(drawer: WidgetDrawer)`
  - `deleteDrawer(id: String, deleteChildren: Boolean)`
  - `addWidget(widget: WidgetItem)`
  - `updateWidget(widget: WidgetItem)`
  - `deleteWidget(id: String, appWidgetId: Int)`
  - `assignWidgetToDrawer(widgetId: String, drawerId: String?)`

---

## 3. iPod Cover Flow 3D Engine (`feature/home/component/IpodCoverFlowWidgetStack.kt`)

### 3.1 3D Perspective Transforms
Using Jetpack Compose `Modifier.graphicsLayer`:
- **Page Offset**: Calculated as `page - pagerState.currentPage - pagerState.currentPageOffsetFraction`.
- **Y-Rotation (`rotationY`)**:
  - `offset < 0` (left cards): rotated $+42^\circ$ inward.
  - `offset > 0` (right cards): rotated $-42^\circ$ inward.
  - `offset == 0` (active card): $0^\circ$ (flat facing forward).
- **Translation X (`translationX`)**:
  - Dynamically adjusted to bring tilted adjacent cards close to the active card's edges, tucking them partially behind.
- **Scale (`scaleX`, `scaleY`)**:
  - Active card: $1.0\times$.
  - Adjacent cards ($|\text{offset}| \approx 1$): $0.82\times$.
  - Further cards: $0.70\times$.
- **Layering (`zIndex`)**:
  - Active card gets highest `zIndex` (e.g. `100f`).
  - Outer cards get decreasing `zIndex` (`100f - abs(offset) * 10f`).
- **Dimming Sheen**:
  - A subtle gradient/tint overlay dims recessed cards by $15\text{--}25\%$, while keeping the active card fully illuminated.
- **Camera Distance**:
  - `cameraDistance = 16f * density` ensures natural focal perspective without extreme fish-eye distortions.

### 3.2 Interactions
- Smooth horizontal drag/swipe gestures with snap physics.
- Tapping on a visible peek of the left or right card smoothly snaps it to the front.
- Vertical scroll events pass through to the outer container.
- Clean widget hosting inside cards with proper clip bounds and gesture isolation.

---

## 4. Grid Feed Layout & Resizing (`WidgetsPage.kt`)

### 4.1 4-Column Grid Flow
- Total grid width corresponds to the available screen width (with adaptive paddings).
- Base column width = `(totalWidth - columnGaps) / 4`.
- Standard row height = `70.dp` (base grid unit).
- Sizing calculation:
  - `spanX = 2` (50% width), `spanY = 2` (~140dp) -> compact 2x2.
  - `spanX = 2`, `spanY = 3` (~210dp) -> 2x3.
  - `spanX = 4`, `spanY = 2` (~140dp) -> 4x2 wide banner.
  - `spanX = 4`, `spanY = 4` (~280dp) -> 4x4 full card.
- Pairs of consecutive 2-column widgets automatically group into a 2-column horizontal row; single 2-column widgets are cleanly aligned; 4-column widgets span the full width.

### 4.2 Interactive Resizer
- Bottom sheet or dialog with quick chips (`2×2`, `2×3`, `4×2`, `4×3`, `4×4`) + fine step adjusters for `spanX` (2 to 4) and `spanY` (1 to 5).
- Updates database and broadcasts new options bundle (`OPTION_APPWIDGET_MIN_WIDTH`, etc.) to the Android widget view in real time.

---

## 5. Management Workflows
1. **Add Flow:**
   - User picks widget -> system prompts destination: Standalone Widget, Existing Drawer, or New Drawer.
2. **Drawer Header & Card:**
   - Frosted glass accordion card with title, icon, item count pill (`3 widgets`), expand/collapse chevron.
   - Header action menu: Add Widget to Stack, Rename Drawer, Change Drawer Size, Delete Drawer.
3. **Card Context Menu:**
   - Move to Drawer / Extract from Drawer.
   - Resize Widget (opens Grid Resizer).
   - Reorder (Move Up/Down or Left/Right in stack).
   - Delete Widget.

---

## 6. Verification Plan
- Unit tests for `WidgetRepository` and `HomeViewModel` (drawer creation, assignment, resizing, deletion).
- Build compilation check with `./gradlew assembleDebug`.
- Live testing and verification on connected device (`10BG4Y0TDS001TD`).
