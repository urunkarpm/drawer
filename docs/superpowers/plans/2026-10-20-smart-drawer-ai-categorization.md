# On-Device AI App Categorization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate the on-device Needle 3 local foundation model and intelligent metadata fallback to provide one-tap smart app drawer categorization and review in Drawer.

**Architecture:** Create an `AppCategorizer` domain engine connecting to `:core:needle` (Needle 3) with a tier-2 Android system metadata fallback, exposed through `AppGroupRepository` to `GroupsViewModel`, and rendered via a new `AiCategorizationPreviewBottomSheet` in Jetpack Compose.

**Tech Stack:** Kotlin 2.3, Jetpack Compose (Material 3), Kotlin Coroutines & Flows, Dagger Hilt, Room, Robolectric/JUnit4.

**Spec:** [`docs/superpowers/specs/2026-10-20-smart-drawer-ai-categorization-design.md`](file:///C:/Users/uprasenjeet/Documents/Drawer/docs/superpowers/specs/2026-10-20-smart-drawer-ai-categorization-design.md)

## Global Constraints

- 100% Offline, Zero Telemetry: No external network calls for classification; everything runs purely on-device.
- Non-blocking: Inference runs asynchronously on `Dispatchers.Default`; UI remains at 120Hz.
- Non-destructive: Propose changes in a preview bottom sheet; changes are only committed when user taps "Apply".
- Graceful Fallback: If Needle model is still downloading or not ready, fallback to `ApplicationInfo.category` + rich semantic heuristics without failing.

---

### Task 1: `AppCategorizer` Engine & Classification (`:core:data`)

**Files:**
- Create: `core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/ai/AppCategorizer.kt`
- Create: `core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/ai/AppCategorizerImpl.kt`
- Test: `core/data/src/test/kotlin/com/urunkarpm/drawer/core/data/ai/AppCategorizerTest.kt`

**Interfaces:**
- Consumes: `NeedleEngine` from `:core:needle`, `AppInfo`, `AppGroup` from `:core:model`.
- Produces: `AppCategorizer` with `suspend fun categorizeApps(apps: List<AppInfo>, existingGroups: List<AppGroup>, unassignedOnly: Boolean): List<CategorizationSuggestion>`

- [x] **Step 1: Write the failing unit test for `AppCategorizer`**

```kotlin
package com.urunkarpm.drawer.core.data.ai

import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.needle.NeedleEngine
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCategorizerTest {

    private val needleEngine: NeedleEngine = mockk(relaxed = true)

    @Test
    fun categorizeApps_mapsFinanceAndSocialCorrectly() = runTest {
        every { needleEngine.modelStatus } returns MutableStateFlow(ModelStatus.NotDownloaded)
        val categorizer = AppCategorizerImpl(needleEngine)

        val groups = listOf(
            AppGroup(id = "finance_group", name = "Finance", orderIndex = 0),
            AppGroup(id = "social_group", name = "Social", orderIndex = 1)
        )

        val apps = listOf(
            AppInfo(packageName = "com.splitwise.app", activityName = ".Main", label = "Splitwise"),
            AppInfo(packageName = "org.telegram.messenger", activityName = ".Main", label = "Telegram")
        )

        val suggestions = categorizer.categorizeApps(apps, groups)
        val financeSuggestion = suggestions.firstOrNull { it.group.id == "finance_group" }
        val socialSuggestion = suggestions.firstOrNull { it.group.id == "social_group" }

        assertTrue(financeSuggestion != null && financeSuggestion.suggestedApps.any { it.label == "Splitwise" })
        assertTrue(socialSuggestion != null && socialSuggestion.suggestedApps.any { it.label == "Telegram" })
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :core:data:testDebugUnitTest --tests com.urunkarpm.drawer.core.data.ai.AppCategorizerTest`
Expected: FAIL (class not found / unresolved reference).

- [x] **Step 3: Implement `AppCategorizer` interface and `AppCategorizerImpl`**

Create `core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/ai/AppCategorizer.kt`:
```kotlin
package com.urunkarpm.drawer.core.data.ai

import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppInfo

data class CategorizationSuggestion(
    val group: AppGroup,
    val suggestedApps: List<AppInfo>
)

interface AppCategorizer {
    suspend fun categorizeApps(
        apps: List<AppInfo>,
        existingGroups: List<AppGroup>,
        unassignedOnly: Boolean = true
    ): List<CategorizationSuggestion>
}
```

Create `core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/ai/AppCategorizerImpl.kt`:
Implements classification matching apps to target categories (Finance, Social, Productivity, Media, Utilities, Games, Health, Shopping) using NeedleEngine when Ready and semantic metadata fallback.

- [x] **Step 4: Run unit test to verify it passes**

Run: `.\gradlew.bat :core:data:testDebugUnitTest --tests com.urunkarpm.drawer.core.data.ai.AppCategorizerTest`
Expected: PASS.

- [x] **Step 5: Commit**

```bash
git add core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/ai/
git add core/data/src/test/kotlin/com/urunkarpm/drawer/core/data/ai/
git commit -m "feat(ai): implement AppCategorizer with on-device intelligence and metadata fallback"
```

---

### Task 2: Repository & ViewModel Integration

**Files:**
- Modify: `core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/repository/AppGroupRepository.kt`
- Modify: `core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/repository/AppGroupRepositoryImpl.kt`
- Modify: `feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/GroupsViewModel.kt`
- Test: `feature/groups/src/test/kotlin/com/urunkarpm/drawer/feature/groups/GroupsViewModelTest.kt`

**Interfaces:**
- Consumes: `AppCategorizer`, `CategorizationSuggestion` from `:core:data`.
- Produces: `GroupsViewModel` with:
  - `val isAiCategorizing: StateFlow<Boolean>`
  - `val aiSuggestions: StateFlow<List<CategorizationSuggestion>?>`
  - `fun startAiCategorization()`
  - `fun applyAiSuggestions(approvedMap: Map<String, List<AppInfo>>)`
  - `fun dismissAiSuggestions()`

- [x] **Step 1: Write unit test in `GroupsViewModelTest` for AI categorization flow**

```kotlin
@Test
fun startAiCategorization_emitsSuggestions() = runTest {
    // Tests that startAiCategorization invokes repository and updates aiSuggestions StateFlow
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :feature:groups:testDebugUnitTest --tests com.urunkarpm.drawer.feature.groups.GroupsViewModelTest`
Expected: FAIL.

- [x] **Step 3: Update `AppGroupRepository` & `AppGroupRepositoryImpl`**

Add `suggestCategorization` and `applyCategorization` methods in `AppGroupRepository` that delegate to `AppCategorizer` and insert items into `AppGroupDao` in a single transaction.

- [x] **Step 4: Update `GroupsViewModel`**

Inject `AppCategorizer` or call `AppGroupRepository.suggestCategorization()` and manage `aiSuggestions` state.

- [x] **Step 5: Run tests to verify they pass**

Run: `.\gradlew.bat :feature:groups:testDebugUnitTest`
Expected: PASS.

- [x] **Step 6: Commit**

```bash
git add core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/repository/
git add feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/
git add feature/groups/src/test/kotlin/com/urunkarpm/drawer/feature/groups/
git commit -m "feat(groups): wire AI categorization through AppGroupRepository and GroupsViewModel"
```

---

### Task 3: UI - AI Action Button & Review Bottom Sheet (`:feature:groups`)

**Files:**
- Create: `feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/component/AiCategorizationPreviewBottomSheet.kt`
- Modify: `feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/GroupsAccordion.kt`

**Interfaces:**
- Consumes: `GroupsUiState.aiSuggestions`, `viewModel.startAiCategorization()`, `viewModel.applyAiSuggestions()`, `viewModel.dismissAiSuggestions()`.
- Produces: Interactive preview sheet with checkboxes, group tags, and "Apply" button.

- [x] **Step 1: Create `AiCategorizationPreviewBottomSheet.kt`**

Build Material 3 `ModalBottomSheet` displaying:
- Header: Sparkle icon, "Organize with AI", subtitle explaining on-device classification.
- Content: LazyColumn of suggested groups. Each group card shows the group title, app count, and expandable app rows with a `Checkbox`.
- Bottom bar: "Cancel" and "Apply (N apps)" buttons with primary container styling.

- [x] **Step 2: Add AI Sparkle Button to `GroupsAccordion.kt`**

In the top bar actions of `GroupsAccordion` (next to Add Group `+`), add an `IconButton` with `Icons.Default.AutoAwesome` (tinted with primary accent) that triggers `viewModel.startAiCategorization()`.
Render `AiCategorizationPreviewBottomSheet` when `uiState.aiSuggestions != null`.

- [x] **Step 3: Verify build and compile**

Run: `.\gradlew.bat :feature:groups:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [x] **Step 4: Commit**

```bash
git add feature/groups/src/main/kotlin/com/urunkarpm/drawer/feature/groups/
git commit -m "feat(groups): add AI organize action button and review bottom sheet"
```

---

### Task 4: Settings Toggle for New App Categorization (`:core:datastore` & `:feature:settings`)

**Files:**
- Modify: `core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt`
- Modify: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsScreen.kt`

**Interfaces:**
- Produces: `autoCategorizeNewApps: Flow<Boolean>` and `fun setAutoCategorizeNewApps(enabled: Boolean)`.

- [x] **Step 1: Add preference in `DrawerPreferencesDataSource`**

Add `AUTO_CATEGORIZE_NEW_APPS` boolean preference key (default `true`).

- [x] **Step 2: Add Switch preference row in Settings under Drawers**

In `SettingsScreen.kt`, add a preference toggle:
- Title: "Auto-suggest categories for new apps"
- Subtitle: "Suggest drawer categories when installing new apps using on-device intelligence"

- [x] **Step 3: Run full assemble and tests**

Run: `.\gradlew.bat assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [x] **Step 4: Commit**

```bash
git add core/datastore/ feature/settings/
git commit -m "feat(settings): add auto-categorize preference toggle"
```
