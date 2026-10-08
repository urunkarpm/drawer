# Typography & Font Packs Design Specification

- **Date:** 2026-10-08
- **Status:** Approved
- **Scope:** Architectural (Cross-module: `core:datastore`, `core:designsystem`, `feature:settings`, `feature:home`, `feature:groups`, `feature:notifications`, `feature:dock`)

---

## 1. Overview & Goals
Introduce comprehensive typography personalization to **Drawer**:
- **Header & Clock Font Styles**: Give users the choice of font styles for the home screen clock, weather status, category headers, and notification panel title without degrading app label readability.
- **App Label Sizing**: Allow users to adjust the font size of app labels (Small, Normal, Large) across the home screen, dock, and app drawer.
- **Zero Bloat**: Utilize native Android font families (`SansSerif`, `Serif`, `Monospace`, `Cursive`) alongside the existing bundled `Texas Troupe` font, incurring 0 KB additional APK size overhead.

---

## 2. Architecture & Data Flow

```
[ DrawerPreferencesDataSource ]
             │
             ├── headerFontFamily: Flow<String>  ("SYSTEM", "SERIF", "MONOSPACE", "CURSIVE", "TEXAS_TROUPE")
             └── appLabelScale: Flow<String>     ("SMALL", "NORMAL", "LARGE")
             │
             ▼
      [ HomeScreen ]
             │
             ▼ Provides CompositionLocals
    ┌──────────────────────────────────────────────┐
    │ CompositionLocalProvider(                   │
    │   LocalHeaderFontFamily provides font,       │
    │   LocalAppLabelScale provides scale          │
    │ ) { ... }                                    │
    └──────────────────────────────────────────────┘
             │
    ┌────────┴────────────────────────┬────────────────────────┐
    ▼                                 ▼                        ▼
[ GlanceHeader ]             [ GroupsAccordion ]      [ NotificationDrawer ]
(Clock digits & weather)     (Drawer header titles)   ("Notifications" title)
    │                                 │
    ▼                                 ▼
[ App Labels ]                [ Dock Items ]
(Grid & List labels           (Dock app labels)
 scaled by labelScale)
```

---

## 3. Data Models (`core:datastore`)

### Keys in `PreferencesKeys`:
- `HEADER_FONT_FAMILY = stringPreferencesKey("header_font_family")`
  - Allowed values: `"SYSTEM"`, `"SERIF"`, `"MONOSPACE"`, `"CURSIVE"`, `"TEXAS_TROUPE"`
  - Default: `"SYSTEM"`
- `APP_LABEL_SCALE = stringPreferencesKey("app_label_scale")`
  - Allowed values: `"SMALL"`, `"NORMAL"`, `"LARGE"`
  - Default: `"NORMAL"`

### Repository / DataSource API:
- `val headerFontFamily: Flow<String>`
- `suspend fun setHeaderFontFamily(font: String)`
- `val appLabelScale: Flow<String>`
- `suspend fun setAppLabelScale(scale: String)`

---

## 4. Design System Tokens & Mappers (`core:designsystem`)

### Mappers:
- `fun headerFontFamilyFromName(name: String): FontFamily`
  - `"SYSTEM"` -> `FontFamily.SansSerif`
  - `"SERIF"` -> `FontFamily.Serif`
  - `"MONOSPACE"` -> `FontFamily.Monospace`
  - `"CURSIVE"` -> `FontFamily.Cursive`
  - `"TEXAS_TROUPE"` -> `TexasTroupeFontFamily`
  - Fallback -> `FontFamily.Default`
- `fun labelScaleFromName(name: String): Float`
  - `"SMALL"` -> `0.85f` (approx 10.sp for standard 12.sp text)
  - `"NORMAL"` -> `1.0f` (standard 12.sp)
  - `"LARGE"` -> `1.2f` (approx 14.5.sp)
  - Fallback -> `1.0f`

### CompositionLocals:
```kotlin
val LocalHeaderFontFamily = staticCompositionLocalOf { FontFamily.Default }
val LocalAppLabelScale = compositionLocalOf { 1.0f }
```

---

## 5. UI Integration

### 1. `HomeScreen.kt` Root Injection
Observe `headerFontFamily` and `appLabelScale` from `HomeViewModel` (backed by `DrawerPreferencesDataSource`), resolve into `FontFamily` and `Float`, and wrap the home content in:
```kotlin
CompositionLocalProvider(
    LocalHeaderFontFamily provides resolvedHeaderFont,
    LocalAppLabelScale provides resolvedLabelScale
) {
    ...
}
```

### 2. Header Components
- **`GlanceHeader.kt`**: Replace hardcoded `TexasTroupeFontFamily` on clock digits and temperature with `LocalHeaderFontFamily.current`.
- **`GroupsAccordion.kt`**: Replace hardcoded `TexasTroupeFontFamily` on group title `Text` with `LocalHeaderFontFamily.current`.
- **`NotificationDrawer.kt`**: Replace hardcoded `TexasTroupeFontFamily` on `"Notifications"` title with `LocalHeaderFontFamily.current`.

### 3. App Labels Scaling
- App grid / list layouts in `feature:groups`, `feature:dock`, and `feature:home`:
  Multiply label font sizes by `LocalAppLabelScale.current` (e.g. `(12 * LocalAppLabelScale.current).sp`).

### 4. Settings UI (`feature:settings`)
Add a new **Typography & Fonts** preference section in `SettingsScreen.kt`:
- **Header Font Family Selection**:
  - Row / grid of selectable chips or radio cards for each font style:
    - *System Sans* (rendered in SansSerif)
    - *Editorial Serif* (rendered in Serif)
    - *Monospace* (rendered in Monospace)
    - *Cursive* (rendered in Cursive)
    - *Texas Troupe* (rendered in TexasTroupe)
  - Interactive preview showing a sample clock / header: `"12:34 • Work"`.
- **App Label Size Selection**:
  - Segmented control / 3-option chip group: *Small (85%)*, *Normal (100%)*, *Large (120%)*.
  - Live sample app label displaying the scaling effect immediately.

---

## 6. Error Handling & Edge Cases
- **Unknown preference string**: Gracefully defaults to `FontFamily.Default` and `1.0f`.
- **Very long app labels with Large scale**: Labels maintain `maxLines = 1` and `TextOverflow.Ellipsis` to prevent clipping or layout breakage in grids and dock.
- **Dynamic re-composition**: Changes in Settings instantly reflect across the entire launcher without requiring app restart.

---

## 7. Testing & Verification
- Unit test for `headerFontFamilyFromName` and `labelScaleFromName` mapping.
- Preferences persistence round-trip test in `DrawerPreferencesDataSource`.
- Visual verification: Verify each font family renders properly in `GlanceHeader`, `GroupsAccordion`, and `NotificationDrawer`.
- Run `./gradlew assembleDebug` to guarantee clean build.
