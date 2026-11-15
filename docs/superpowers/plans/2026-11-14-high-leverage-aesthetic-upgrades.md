# Implementation Plan: High-Leverage Aesthetic Upgrades

**Goal:** Implement 4 high-leverage aesthetic improvements for Drawer: Directional Hairline Glass surface style, Spatial Depth Focus Dimming across category drawers, Typographic Sculpting on the Glance Header, and Universal Monochrome App Icons.

**Guiding Principle:** Senior developer "ponytail" mode — build the minimum that works with standard Compose primitives, zero unneeded dependencies, zero GPU recomposition churn, and mark deliberate simplifications with ponytail comments naming ceilings and upgrade paths.

---

## Proposed Changes

### Core: Design System
#### [DockSurfaceBackground.kt](file:///C:/Users/uprasenjeet/Documents/Drawer/core/designsystem/src/main/kotlin/com/urunkarpm/drawer/core/designsystem/component/DockSurfaceBackground.kt)
- Add `"HAIRLINE"` surface aesthetic to `DockSurfaceBackground` and `DrawerSurfaceBackground`.
- Features an ultra-subtle frosted tint (`surfaceBaseColor.copy(alpha = 0.55f * opacity)`) and a delicate `0.75.dp` top-down specular hairline border (`Brush.verticalGradient(listOf(Color.White.copy(alpha = (0.45f * strokeAlpha).coerceIn(0f, 1f)), Color.White.copy(alpha = (0.08f * strokeAlpha).coerceIn(0f, 1f)), Color.Transparent))`).

#### [AppIconImage.kt](file:///C:/Users/uprasenjeet/Documents/Drawer/core/designsystem/src/main/kotlin/com/urunkarpm/drawer/core/designsystem/component/AppIconImage.kt)
- Expose `LocalMonochromeIcons = compositionLocalOf { false }`.
- When `LocalMonochromeIcons.current` is true, apply `ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })` to `Image(bitmap = bitmap, ...)` in `AppIconContent`.

### Core: DataStore
#### [DrawerPreferencesDataSource.kt](file:///C:/Users/uprasenjeet/Documents/Drawer/core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt)
- Add `MONOCHROME_ICONS = booleanPreferencesKey("monochrome_icons")`.
- Expose `monochromeIcons: Flow<Boolean>` and `suspend fun setMonochromeIcons(enabled: Boolean)`.

### Feature: Home
#### [GlanceHeader.kt](file:///C:/Users/uprasenjeet/Documents/Drawer/feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/GlanceHeader.kt)
- Enable tabular numerals `fontFeatureSettings = "tnum"` on the clock display and temperature text to eliminate digit-jump jitter on time changes.
- Apply tight optical kerning (`letterSpacing = (-1.2).sp` for clock, `(-1.0).sp` for temp).
- Enhance date tracking (`letterSpacing = 0.2.sp`).

#### [HomeViewModel.kt](file:///C:/Users/uprasenjeet/Documents/Drawer/feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModel.kt)
- Expose `monochromeIcons: StateFlow<Boolean>`.

#### [HomeScreen.kt](file:///C:/Users/uprasenjeet/Documents/Drawer/feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt)
- Collect `monochromeIcons` and provide `CompositionLocalProvider(LocalMonochromeIcons provides monochromeIcons)` to all home content, app drawers, docks, and sheets.

### Feature: Groups
#### [GroupsAccordion.kt](file:///C:/Users/uprasenjeet/Documents/Drawer/feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/GroupsAccordion.kt)
- Detect if any group in the accordion is expanded.
- In `GroupCard`, compute `isDimmedSibling = anyGroupExpanded && !isExpanded`.
- When dimmed, smoothly animate alpha to `0.42f` and scale to `0.98f` using `FluidSpringPhysics.fluidSpring()`.

### Feature: Settings
#### [SettingsUiState.kt & SettingsViewModel.kt](file:///C:/Users/uprasenjeet/Documents/Drawer/feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsViewModel.kt)
- Add `monochromeIcons: Boolean = false`, expose `fun setMonochromeIcons(enabled: Boolean)`.

#### [LookSubScreens.kt](file:///C:/Users/uprasenjeet/Documents/Drawer/feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/subscreen/LookSubScreens.kt)
- Add `"HAIRLINE" to "Hairline"` option to `SurfacesSubScreen`'s `SettingsSegmentedRow`.
- Add "Monochrome App Icons" switch in `TypographyIconsSubScreen`.

---

## Tasks

### Task 1: Add Directional Specular Hairline Surface Style (`HAIRLINE`)
- Update `DockSurfaceBackground.kt` for both `DockSurfaceBackground` and `DrawerSurfaceBackground`.
- Expose `"HAIRLINE"` option in `SurfacesSubScreen` in `LookSubScreens.kt`.
- Test: Verify build and surface rendering.

### Task 2: Implement Spatial Depth & Focus Dimming in Category Drawers
- In `GroupsAccordion.kt`, determine if any category drawer is expanded.
- Animate sibling card alpha down to `0.42f` and scale to `0.98f` with `FluidSpringPhysics.fluidSpring()`.
- Test: Expand/collapse transitions.

### Task 3: Typographic Sculpting on Glance Header
- In `GlanceHeader.kt`, add `fontFeatureSettings = "tnum"` and calibrated `letterSpacing` on clock, temperature, and date displays.
- Test: Check time text alignment.

### Task 4: Universal Monochrome / Themed App Icons
- Add preference in `DrawerPreferencesDataSource.kt`.
- Wire through `SettingsViewModel`, `SettingsUiState`, `HomeViewModel`, and `HomeScreen`.
- Implement `LocalMonochromeIcons` in `AppIconImage.kt` using `ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })`.
- Expose switch in `TypographyIconsSubScreen` in `LookSubScreens.kt`.
- Test: Verify icon rendering with both color and monochrome modes.

### Task 5: End-to-End Build & Unit Testing
- Run `./gradlew.bat :core:designsystem:testDebugUnitTest :feature:groups:testDebugUnitTest :feature:home:testDebugUnitTest :feature:settings:testDebugUnitTest`.
- Confirm 100% passes.
