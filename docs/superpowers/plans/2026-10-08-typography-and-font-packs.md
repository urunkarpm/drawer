# Typography & Font Packs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement user-configurable typography (Header & Clock font styles: Sans-Serif, Serif, Monospace, Cursive, Texas Troupe) and adjustable App Label scaling (Small, Normal, Large) across Drawer.

**Architecture:** Preferences are persisted in `core:datastore`, mapped to Compose tokens in `core:designsystem`, and provided at the root of `HomeScreen` via `LocalHeaderFontFamily` and `LocalAppLabelScale` CompositionLocals to cleanly style headers and scale app labels without prop drilling.

**Tech Stack:** Kotlin 2.3, Jetpack Compose, AndroidX Datastore Preferences, Hilt, JUnit4.

**Spec:** [`docs/superpowers/specs/2026-10-08-typography-and-font-packs-design.md`](file:///C:/Users/uprasenjeet/Documents/Drawer/docs/superpowers/specs/2026-10-08-typography-and-font-packs-design.md)

## Global Constraints
- Zero APK bloat: Use native Android font families (`SansSerif`, `Serif`, `Monospace`, `Cursive`) plus the existing bundled `texas_troupe_fp.ttf`.
- Headers & Clock only for decorative fonts; app labels remain in clean system font for maximum legibility.
- Non-breaking defaults: Default header font is `"SYSTEM"`, default app label scale is `"NORMAL"` (1.0f).
- DRY & YAGNI: Use CompositionLocals to avoid threading font parameters through dozens of composables.

---

### Task 1: Preferences Keys & DataSource in `core:datastore`

**Files:**
- Modify: `core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt`
- Test: `core/datastore/src/test/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSourceTest.kt`

**Interfaces:**
- Produces:
  - `DrawerPreferencesDataSource.headerFontFamily: Flow<String>` (values: `"SYSTEM"`, `"SERIF"`, `"MONOSPACE"`, `"CURSIVE"`, `"TEXAS_TROUPE"`)
  - `suspend DrawerPreferencesDataSource.setHeaderFontFamily(font: String)`
  - `DrawerPreferencesDataSource.appLabelScale: Flow<String>` (values: `"SMALL"`, `"NORMAL"`, `"LARGE"`)
  - `suspend DrawerPreferencesDataSource.setAppLabelScale(scale: String)`

- [ ] **Step 1: Write the failing unit test**

Create or update `core/datastore/src/test/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSourceTest.kt`:

```kotlin
package com.urunkarpm.drawer.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DrawerPreferencesDataSourceTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun createDataSource(): DrawerPreferencesDataSource {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_prefs.preferences_pb") }
        )
        return DrawerPreferencesDataSource(testDataStore)
    }

    @Test
    fun defaultHeaderFontFamilyIsSystem() = runTest {
        val dataSource = createDataSource()
        assertEquals("SYSTEM", dataSource.headerFontFamily.first())
    }

    @Test
    fun updateHeaderFontFamilyPersists() = runTest {
        val dataSource = createDataSource()
        dataSource.setHeaderFontFamily("SERIF")
        assertEquals("SERIF", dataSource.headerFontFamily.first())
    }

    @Test
    fun defaultAppLabelScaleIsNormal() = runTest {
        val dataSource = createDataSource()
        assertEquals("NORMAL", dataSource.appLabelScale.first())
    }

    @Test
    fun updateAppLabelScalePersists() = runTest {
        val dataSource = createDataSource()
        dataSource.setAppLabelScale("LARGE")
        assertEquals("LARGE", dataSource.appLabelScale.first())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:datastore:testDebugUnitTest --tests com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSourceTest`
Expected: FAIL (unresolved reference: `headerFontFamily`, `setHeaderFontFamily`, `appLabelScale`, `setAppLabelScale`)

- [ ] **Step 3: Implement minimal code in `DrawerPreferencesDataSource.kt`**

In `core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt`:
1. Add keys in `PreferencesKeys`:
```kotlin
val HEADER_FONT_FAMILY = stringPreferencesKey("header_font_family")
val APP_LABEL_SCALE = stringPreferencesKey("app_label_scale")
```
2. Add Flows and setters:
```kotlin
val headerFontFamily: Flow<String> = dataStore.data.map { it[PreferencesKeys.HEADER_FONT_FAMILY] ?: "SYSTEM" }

suspend fun setHeaderFontFamily(font: String) {
    dataStore.edit { it[PreferencesKeys.HEADER_FONT_FAMILY] = font }
}

val appLabelScale: Flow<String> = dataStore.data.map { it[PreferencesKeys.APP_LABEL_SCALE] ?: "NORMAL" }

suspend fun setAppLabelScale(scale: String) {
    dataStore.edit { it[PreferencesKeys.APP_LABEL_SCALE] = scale }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:datastore:testDebugUnitTest --tests com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSourceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add core/datastore/
git commit -m "feat(datastore): add headerFontFamily and appLabelScale preference keys and flows"
```

---

### Task 2: Typography Tokens, CompositionLocals, and Mappers in `core:designsystem`

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/urunkarpm/drawer/core/designsystem/theme/TypographyPacks.kt`
- Test: `core/designsystem/src/test/kotlin/com/urunkarpm/drawer/core/designsystem/TypographyPacksTest.kt`

**Interfaces:**
- Produces:
  - `fun headerFontFamilyFromName(name: String): FontFamily`
  - `fun labelScaleFromName(name: String): Float`
  - `val LocalHeaderFontFamily: ProvidableCompositionLocal<FontFamily>`
  - `val LocalAppLabelScale: ProvidableCompositionLocal<Float>`

- [ ] **Step 1: Write the failing test**

Create `core/designsystem/src/test/kotlin/com/urunkarpm/drawer/core/designsystem/TypographyPacksTest.kt`:

```kotlin
package com.urunkarpm.drawer.core.designsystem

import androidx.compose.ui.text.font.FontFamily
import com.urunkarpm.drawer.core.designsystem.theme.TexasTroupeFontFamily
import com.urunkarpm.drawer.core.designsystem.theme.headerFontFamilyFromName
import com.urunkarpm.drawer.core.designsystem.theme.labelScaleFromName
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyPacksTest {

    @Test
    fun mapsKnownHeaderFontFamiliesCorrectly() {
        assertEquals(FontFamily.SansSerif, headerFontFamilyFromName("SYSTEM"))
        assertEquals(FontFamily.Serif, headerFontFamilyFromName("SERIF"))
        assertEquals(FontFamily.Monospace, headerFontFamilyFromName("MONOSPACE"))
        assertEquals(FontFamily.Cursive, headerFontFamilyFromName("CURSIVE"))
        assertEquals(TexasTroupeFontFamily, headerFontFamilyFromName("TEXAS_TROUPE"))
        assertEquals(FontFamily.Default, headerFontFamilyFromName("UNKNOWN"))
    }

    @Test
    fun mapsKnownLabelScalesCorrectly() {
        assertEquals(0.85f, labelScaleFromName("SMALL"), 0.001f)
        assertEquals(1.0f, labelScaleFromName("NORMAL"), 0.001f)
        assertEquals(1.2f, labelScaleFromName("LARGE"), 0.001f)
        assertEquals(1.0f, labelScaleFromName("UNKNOWN"), 0.001f)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:designsystem:testDebugUnitTest --tests com.urunkarpm.drawer.core.designsystem.TypographyPacksTest`
Expected: FAIL (unresolved references)

- [ ] **Step 3: Implement `TypographyPacks.kt`**

Create `core/designsystem/src/main/kotlin/com/urunkarpm/drawer/core/designsystem/theme/TypographyPacks.kt`:

```kotlin
package com.urunkarpm.drawer.core.designsystem.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

val LocalHeaderFontFamily = staticCompositionLocalOf { FontFamily.Default }
val LocalAppLabelScale = compositionLocalOf { 1.0f }

fun headerFontFamilyFromName(name: String): FontFamily = when (name.uppercase()) {
    "SYSTEM" -> FontFamily.SansSerif
    "SERIF" -> FontFamily.Serif
    "MONOSPACE" -> FontFamily.Monospace
    "CURSIVE" -> FontFamily.Cursive
    "TEXAS_TROUPE" -> TexasTroupeFontFamily
    else -> FontFamily.Default
}

fun labelScaleFromName(name: String): Float = when (name.uppercase()) {
    "SMALL" -> 0.85f
    "NORMAL" -> 1.0f
    "LARGE" -> 1.2f
    else -> 1.0f
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:designsystem:testDebugUnitTest --tests com.urunkarpm.drawer.core.designsystem.TypographyPacksTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add core/designsystem/
git commit -m "feat(designsystem): add TypographyPacks tokens, mappers, and CompositionLocals"
```

---

### Task 3: Expose Typography in `HomeViewModel` and Provide at Root of `HomeScreen`

**Files:**
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModel.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`

**Interfaces:**
- Consumes:
  - `DrawerPreferencesDataSource.headerFontFamily`
  - `DrawerPreferencesDataSource.appLabelScale`
  - `headerFontFamilyFromName`, `labelScaleFromName`, `LocalHeaderFontFamily`, `LocalAppLabelScale`
- Produces:
  - Root `CompositionLocalProvider` delivering typography tokens to the entire home hierarchy.

- [ ] **Step 1: Expose properties in `HomeUiState` and `HomeViewModel`**

In `HomeUiState` (inside `HomeViewModel.kt`):
```kotlin
val headerFontFamily: String = "SYSTEM",
val appLabelScale: String = "NORMAL",
```
In `HomeViewModel.kt`, map these properties from `preferencesDataSource.headerFontFamily` and `preferencesDataSource.appLabelScale` in the `combine` block of `uiState`.

- [ ] **Step 2: Provide `LocalHeaderFontFamily` and `LocalAppLabelScale` in `HomeScreen.kt`**

In `HomeScreen.kt`:
```kotlin
val headerFont = remember(uiState.headerFontFamily) { headerFontFamilyFromName(uiState.headerFontFamily) }
val labelScale = remember(uiState.appLabelScale) { labelScaleFromName(uiState.appLabelScale) }

CompositionLocalProvider(
    LocalHeaderFontFamily provides headerFont,
    LocalAppLabelScale provides labelScale
) {
    // Existing HomeScreen layout content
}
```

- [ ] **Step 3: Run build to verify compilation**

Run: `./gradlew :feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add feature/home/
git commit -m "feat(home): inject LocalHeaderFontFamily and LocalAppLabelScale in HomeScreen"
```

---

### Task 4: Connect Header Components to `LocalHeaderFontFamily`

**Files:**
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/GlanceHeader.kt`
- Modify: `feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/GroupsAccordion.kt`
- Modify: `feature/notifications/src/main/kotlin/com/urunkarpm/drawer/feature/notifications/NotificationDrawer.kt`

**Interfaces:**
- Consumes: `LocalHeaderFontFamily.current`

- [ ] **Step 1: Update `GlanceHeader.kt`**

Replace:
```kotlin
Text(
    text = timeDigits,
    fontFamily = TexasTroupeFontFamily,
    ...
)
```
with:
```kotlin
Text(
    text = timeDigits,
    fontFamily = LocalHeaderFontFamily.current,
    ...
)
```
and update `tempText` similarly to use `LocalHeaderFontFamily.current`.

- [ ] **Step 2: Update `GroupsAccordion.kt`**

In `GroupCard`:
Replace `fontFamily = TexasTroupeFontFamily` with `fontFamily = LocalHeaderFontFamily.current`.

- [ ] **Step 3: Update `NotificationDrawer.kt`**

In `NotificationDrawer`:
Replace `fontFamily = TexasTroupeFontFamily` with `fontFamily = LocalHeaderFontFamily.current`.

- [ ] **Step 4: Run build to verify compilation**

Run: `./gradlew :feature:home:compileDebugKotlin :feature:groups:compileDebugKotlin :feature:notifications:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add feature/home/ feature/groups/ feature/notifications/
git commit -m "feat(ui): consume LocalHeaderFontFamily in GlanceHeader, GroupsAccordion, and NotificationDrawer"
```

---

### Task 5: Scale App Labels via `LocalAppLabelScale`

**Files:**
- Modify: `feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/component/GroupGridLayout.kt`
- Modify: `feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/component/GroupListLayout.kt`
- Modify: `feature/dock/src/main/kotlin/com/urunkarpm/drawer/feature/dock/Dock.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AllAppsDrawer.kt`

**Interfaces:**
- Consumes: `LocalAppLabelScale.current`

- [ ] **Step 1: Scale font sizes in `GroupGridLayout.kt` and `GroupListLayout.kt`**

Multiply the label `fontSize` by `LocalAppLabelScale.current`:
```kotlin
fontSize = (11.5f * LocalAppLabelScale.current).sp
```

- [ ] **Step 2: Scale font sizes in `Dock.kt` and `AllAppsDrawer.kt`**

Multiply label `fontSize` in `DockItem` and `AllAppsDrawer` grid items by `LocalAppLabelScale.current`.

- [ ] **Step 3: Run build to verify compilation**

Run: `./gradlew :feature:dock:compileDebugKotlin :feature:groups:compileDebugKotlin :feature:home:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add feature/dock/ feature/groups/ feature/home/
git commit -m "feat(ui): apply LocalAppLabelScale to app labels across dock, groups, and all-apps"
```

---

### Task 6: Settings UI for Typography & Font Packs in `feature:settings`

**Files:**
- Modify: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsViewModel.kt`
- Modify: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsScreen.kt`

**Interfaces:**
- Consumes: `DrawerPreferencesDataSource.setHeaderFontFamily`, `DrawerPreferencesDataSource.setAppLabelScale`
- Produces: An interactive Typography card in Settings with live preview.

- [ ] **Step 1: Update `SettingsViewModel.kt`**

1. Add properties to `ThemeSettings` and `SettingsUiState`:
```kotlin
val headerFontFamily: String = "SYSTEM",
val appLabelScale: String = "NORMAL",
```
2. In `init` block / state mapper, observe:
```kotlin
preferencesDataSource.headerFontFamily.collect { ... }
preferencesDataSource.appLabelScale.collect { ... }
```
3. Expose action methods:
```kotlin
fun setHeaderFontFamily(font: String) {
    viewModelScope.launch(ioDispatcher) {
        preferencesDataSource.setHeaderFontFamily(font)
    }
}

fun setAppLabelScale(scale: String) {
    viewModelScope.launch(ioDispatcher) {
        preferencesDataSource.setAppLabelScale(scale)
    }
}
```

- [ ] **Step 2: Add Typography Card in `SettingsScreen.kt`**

Under the Appearance section, add a dedicated `TypographyCard`:
- Header Font Style selector with 5 chips:
  - **System Sans** (`SYSTEM`)
  - **Editorial Serif** (`SERIF`)
  - **Monospace** (`MONOSPACE`)
  - **Cursive** (`CURSIVE`)
  - **Texas Troupe** (`TEXAS_TROUPE`)
  Each chip renders its label in its own font family for an immediate preview.
- Live sample clock: Displays `"12:34 • Work"` rendered in the selected `headerFontFamily`.
- App Label Size selector with 3 segmented chips:
  - **Small (85%)** (`SMALL`)
  - **Normal (100%)** (`NORMAL`)
  - **Large (120%)** (`LARGE`)
  With an interactive preview text displaying `"Sample App"` scaled dynamically.

- [ ] **Step 3: Run build to verify compilation**

Run: `./gradlew :feature:settings:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add feature/settings/
git commit -m "feat(settings): add Typography & Font Packs configuration card and live preview"
```

---

### Task 7: Full Test Suite & Release Verification

**Files:**
- All touched modules

- [ ] **Step 1: Run all unit tests**

Run: `./gradlew testDebugUnitTest`
Expected: All tests pass.

- [ ] **Step 2: Run full debug and release assembly**

Run: `./gradlew assembleDebug assembleRelease`
Expected: BUILD SUCCESSFUL, APK packaged cleanly.

- [ ] **Step 3: Final verification commit (if any leftover adjustments)**

```bash
git status
```
Verify working tree is clean.
