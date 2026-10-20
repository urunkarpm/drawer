# Design Document: On-Device AI App Categorization for Drawer

- **Status:** Approved
- **Date:** 2026-10-20
- **Target Platform:** Android (API 24+)
- **Architecture:** Multi-module Clean Architecture (`:core:needle`, `:core:data`, `:feature:groups`, `:feature:settings`)

---

## 1. Executive Summary & Goals

Drawer users currently organize apps into workflow-oriented accordion drawers (*Deep Work*, *Finance*, *Social*, *Media*, *Utilities*). Previously, auto-population relied on basic hardcoded substring matching in `AppGroupRepositoryImpl.kt` (`pkg.contains("pay")` etc.), which fails for regional apps, new utilities, and custom-named user drawers.

With the local **Needle 3 foundation model** (`:core:needle`) streaming/downloaded on the device, Drawer can perform **100% offline, private, zero-telemetry app categorization**.

### Key Objectives
1. **One-Tap "Organize with AI":** A dedicated AI action button in the Groups accordion toolbar that analyzes unorganized and installed apps, mapping them to existing or suggested drawers.
2. **Review & Approval Bottom Sheet:** A transparent, non-destructive UI showing the proposed changes grouped by drawer (e.g. *Finance: +3 apps*, *Work: +2 apps*) with check/uncheck controls before committing.
3. **Multi-Tier Robust Fallback:** If the Needle model is still downloading or not yet ready, the system immediately falls back to Android OS `ApplicationInfo.category` metadata + expanded semantic heuristics so organization *always works* without blocking the user.
4. **New App Auto-Categorization (Settings):** An optional preference allowing Drawer to automatically suggest or assign newly installed apps to the most appropriate drawer.

---

## 2. Architecture & Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    GroupsAccordion UI                       │
│  [✨ Organize with AI] button                                │
└──────────────────────────────┬──────────────────────────────┘
                               │ User taps
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     GroupsViewModel                         │
│  • Emits isCategorizing = true                              │
│  • Calls AppGroupRepository.suggestCategorization()         │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 AppGroupRepositoryImpl                      │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ Check NeedleEngine.modelStatus:                     │   │
│   │ • Ready: Use Needle AI Classifier                   │   │
│   │ • Not Ready: Use Android OS Category + Heuristics   │   │
│   └─────────────────────────────────────────────────────┘   │
└──────────────────────────────┬──────────────────────────────┘
                               │ Returns Proposed Diff
                               ▼
┌─────────────────────────────────────────────────────────────┐
│          AiCategorizationPreviewBottomSheet                 │
│  • Displays proposed apps per drawer with checkboxes        │
│  • "Apply" button commits to Room in a single transaction   │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Public API & Interfaces

### 3.1 `:core:needle` / `:core:data` Categorizer Contract

```kotlin
data class CategorizationSuggestion(
    val group: AppGroup,
    val suggestedApps: List<AppInfo>
)

interface AppCategorizer {
    suspend fun categorizeApps(
        apps: List<AppInfo>,
        existingGroups: List<AppGroup>
    ): List<CategorizationSuggestion>
}
```

### 3.2 Classification Logic

1. **Category Mapping:**
   - Standard target domains:
     - **Finance:** Banking, UPI, Crypto, Wallets, Investing, Expense tracking.
     - **Social & Communication:** Messaging, Email, Social networks, Phone, Contacts.
     - **Deep Work / Productivity:** Office, Docs, Notes, Project management, AI assistants, Calendar, Cloud storage.
     - **Media & Entertainment:** Music, Streaming, Podcasts, Gallery, Video players.
     - **Utilities & System:** Settings, File managers, System tools, Terminal, Calculators.
     - **Games:** Android `CATEGORY_GAME` or identified mobile games.
     - **Health & Fitness:** Exercise trackers, Wearables, Health monitoring.
     - **Shopping & Food:** E-commerce, Food delivery, Grocery.
2. **Group Matching:**
   - Matches classified category to user-created drawers using fuzzy name matching (e.g. user group "Money" matches "Finance", "Chat" matches "Social").
   - If no existing group matches, apps can be placed in a proposed new group or general fallback drawer.

---

## 4. UI & Interaction Design

### 4.1 Groups Accordion Toolbar (`GroupsAccordion.kt`)
- Next to the "Add Group" (`+`) icon, add a new **`AutoAwesome`** (AI sparkle) icon button.
- When clicked:
  - If model is downloading, shows progress or runs immediate metadata categorization.
  - Launches the `AiCategorizationPreviewBottomSheet`.

### 4.2 Preview Bottom Sheet (`AiCategorizationPreviewBottomSheet.kt`)
- Bottom sheet with:
  - **Title:** "Organize with AI" + Badge (`⚡ On-Device AI` or `📋 Smart Metadata`).
  - **Summary:** e.g. "Suggested 14 apps across 4 drawers".
  - **Group Cards:** Each group displays its icon, name, and the list of suggested apps. Each app row has a checkbox (checked by default).
  - **Bottom Bar:**
    - "Select All" / "Deselect All" button.
    - "Apply (X apps)" high-emphasis button.
    - "Cancel" button.

### 4.3 Settings (`:feature:settings`)
- New preference in **Drawers & Folders** section:
  - Title: *"Auto-suggest category for new apps"*
  - Subtitle: *"Suggest or assign newly installed apps to drawers using on-device intelligence"*
  - Key: `auto_categorize_new_apps` (Boolean, default `true`).

---

## 5. Error Handling & Edge Cases

1. **Model Not Ready / Network Down:**
   - The engine never fails or crashes: it seamlessly falls back to Tier-2 (OS `ApplicationInfo.category` + rich keyword dictionary).
2. **Apps Already Assigned:**
   - By default, categorization targets **unassigned apps** to avoid disrupting existing custom user layouts.
   - A toggle *"Include already assigned apps"* allows full reorganizations if desired.
3. **Thread Safety & Performance:**
   - Inference and DB queries execute on `Dispatchers.Default` and `Dispatchers.IO`. UI thread is never blocked.
4. **Data Persistence:**
   - When "Apply" is tapped, all assignments are committed inside a Room `@Transaction` in `AppGroupDao`.
