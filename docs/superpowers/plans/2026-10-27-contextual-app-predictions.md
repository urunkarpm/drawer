# Contextual App Predictions Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a 100% on-device, contextual app prediction engine for Drawer that models habitual routines (time-of-day slots, weekday vs weekend), decays historical counts with a 7-day half-life, boosts apps with active notifications, and automatically deduplicates against the dock.

**Architecture:** Multi-module Clean Architecture. The usage profile model lives in `:core:model`, persistence and migration live in `:core:datastore`, the pure mathematical scoring engine (`AppPredictor`) lives in `:core:data`, and the UI freeze lifecycle & user controls live in `:feature:home` and `:feature:settings`.

**Tech Stack:** Kotlin 2.x, Jetpack Compose, Kotlinx Serialization, AndroidX DataStore Preferences, Hilt/Dependency Injection, JUnit 4, Kotlin Coroutines & Flow.

**Spec:** [`docs/superpowers/specs/2026-10-27-contextual-app-predictions-design.md`](file:///C:/Users/uprasenjeet/Documents/Drawer/docs/superpowers/specs/2026-10-27-contextual-app-predictions-design.md)

## Global Constraints
- Platform: Android 8.0+ (API 26+)
- Execution: 100% offline, zero network requests, zero telemetry, <2ms calculation latency.
- Permissions: Zero new Android runtime permissions (no location, no activity recognition).
- Storage footprint: Less than 20 KB in DataStore; cap tracked usage profiles to top 100 apps.

---

### Task 1: Usage Profile Model & DataStore Persistence

**Files:**
- Create: `core/model/src/main/kotlin/com/urunkarpm/drawer/core/model/AppUsageProfile.kt`
- Modify: `core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt`
- Test: `core/datastore/src/test/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSourceTest.kt`

**Interfaces:**
- Produces:
  - `data class AppUsageProfile(val totalScore: Float, val lastLaunchTime: Long, val weekdaySlots: List<Int>, val weekendSlots: List<Int>)`
  - `val appUsageProfiles: Flow<Map<String, AppUsageProfile>>`
  - `val suggestionBlacklist: Flow<Set<String>>`
  - `val showDrawerSuggestions: Flow<Boolean>`
  - `suspend fun recordAppLaunchWithContext(componentKey: String, timestamp: Long = System.currentTimeMillis())`
  - `suspend fun hideAppFromSuggestions(packageName: String)`
  - `suspend fun setShowDrawerSuggestions(enabled: Boolean)`

- [ ] **Step 1: Write the failing unit tests for `AppUsageProfile` and DataStore tracking**

Add tests in `core/datastore/src/test/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSourceTest.kt`:

```kotlin
@Test
fun testRecordAppLaunchWithContext_updatesTimeSlotAndDecaysScore() = runTest {
    val key = "com.example.app/MainActivity"
    // Tuesday 09:00 AM (Weekday, Morning slot 0)
    val cal = java.util.Calendar.getInstance().apply {
        set(2026, java.util.Calendar.OCTOBER, 27, 9, 0, 0)
    }
    val tuesdayMorning = cal.timeInMillis

    dataSource.recordAppLaunchWithContext(key, tuesdayMorning)
    val profiles = dataSource.appUsageProfiles.first()
    val profile = profiles[key]
    assertNotNull(profile)
    assertEquals(1.0f, profile!!.totalScore, 0.01f)
    assertEquals(1, profile.weekdaySlots[0]) // Morning slot 0
    assertEquals(0, profile.weekdaySlots[1])
    assertEquals(0, profile.weekendSlots[0])

    // Launch again 7 days later (half-life decay test: 1.0 * 0.5 + 1.0 = 1.5)
    val nextWeek = tuesdayMorning + (7L * 24 * 60 * 60 * 1000)
    dataSource.recordAppLaunchWithContext(key, nextWeek)
    val updatedProfiles = dataSource.appUsageProfiles.first()
    val updatedProfile = updatedProfiles[key]
    assertNotNull(updatedProfile)
    assertEquals(1.5f, updatedProfile!!.totalScore, 0.05f)
    assertEquals(2, updatedProfile.weekdaySlots[0])
}

@Test
fun testHideAppFromSuggestions_addsToBlacklist() = runTest {
    dataSource.hideAppFromSuggestions("com.distracting.app")
    val blacklist = dataSource.suggestionBlacklist.first()
    assertTrue(blacklist.contains("com.distracting.app"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
.\gradlew :core:datastore:testDebugUnitTest --tests "com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSourceTest.testRecordAppLaunchWithContext_updatesTimeSlotAndDecaysScore"
```
Expected: FAIL with compilation error (`Unresolved reference: AppUsageProfile`, `recordAppLaunchWithContext`).

- [ ] **Step 3: Implement `AppUsageProfile` and DataStore methods**

Create `core/model/src/main/kotlin/com/urunkarpm/drawer/core/model/AppUsageProfile.kt`:
```kotlin
package com.urunkarpm.drawer.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AppUsageProfile(
    val totalScore: Float = 0f,
    val lastLaunchTime: Long = 0L,
    // Slots: 0=Morning (06-12), 1=Afternoon (12-17), 2=Evening (17-22), 3=Night (22-06)
    val weekdaySlots: List<Int> = listOf(0, 0, 0, 0),
    val weekendSlots: List<Int> = listOf(0, 0, 0, 0)
)
```

In `core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt`:
- Add preference keys:
  ```kotlin
  val APP_USAGE_PROFILES = stringPreferencesKey("app_usage_profiles")
  val SUGGESTION_BLACKLIST = stringPreferencesKey("suggestion_blacklist")
  val SHOW_DRAWER_SUGGESTIONS = booleanPreferencesKey("show_drawer_suggestions")
  ```
- Expose Flows:
  ```kotlin
  val appUsageProfiles: Flow<Map<String, AppUsageProfile>> = dataStore.data.map { prefs ->
      val json = prefs[PreferencesKeys.APP_USAGE_PROFILES] ?: return@map emptyMap()
      try {
          Json.decodeFromString<Map<String, AppUsageProfile>>(json)
      } catch (_: Throwable) {
          emptyMap()
      }
  }
  val suggestionBlacklist: Flow<Set<String>> = dataStore.data.map { prefs ->
      val json = prefs[PreferencesKeys.SUGGESTION_BLACKLIST] ?: return@map emptySet()
      try {
          Json.decodeFromString<Set<String>>(json)
      } catch (_: Throwable) {
          emptySet()
      }
  }
  val showDrawerSuggestions: Flow<Boolean> = dataStore.data.map {
      it[PreferencesKeys.SHOW_DRAWER_SUGGESTIONS] ?: true
  }
  ```
- Implement `recordAppLaunchWithContext`:
  ```kotlin
  suspend fun recordAppLaunchWithContext(
      componentKey: String,
      timestamp: Long = System.currentTimeMillis()
  ) {
      dataStore.edit { prefs ->
          val currentJson = prefs[PreferencesKeys.APP_USAGE_PROFILES]
          val currentMap = currentJson?.let {
              try { Json.decodeFromString<Map<String, AppUsageProfile>>(it) } catch (_: Throwable) { emptyMap() }
          } ?: emptyMap()

          val existing = currentMap[componentKey] ?: AppUsageProfile()
          val elapsedDays = if (existing.lastLaunchTime > 0L) {
              (timestamp - existing.lastLaunchTime).coerceAtLeast(0L) / (1000.0 * 60.0 * 60.0 * 24.0)
          } else 0.0

          // 7-day half-life decay on existing score + 1.0 for new launch
          val decayedScore = (existing.totalScore * Math.pow(0.5, elapsedDays / 7.0)).toFloat() + 1.0f

          val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
          val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
          val isWeekend = dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY
          val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
          val slot = when {
              hour in 6..11 -> 0
              hour in 12..16 -> 1
              hour in 17..21 -> 2
              else -> 3
          }

          val updatedWeekdaySlots = existing.weekdaySlots.toMutableList()
          val updatedWeekendSlots = existing.weekendSlots.toMutableList()
          if (isWeekend) {
              updatedWeekendSlots[slot] = updatedWeekendSlots[slot] + 1
          } else {
              updatedWeekdaySlots[slot] = updatedWeekdaySlots[slot] + 1
          }

          val updatedProfile = existing.copy(
              totalScore = decayedScore,
              lastLaunchTime = timestamp,
              weekdaySlots = updatedWeekdaySlots,
              weekendSlots = updatedWeekendSlots
          )

          val updatedMap = currentMap.toMutableMap()
          updatedMap[componentKey] = updatedProfile

          val pruned = if (updatedMap.size > 150) {
              updatedMap.entries.sortedByDescending { it.value.totalScore }.take(100).associate { it.key to it.value }
          } else updatedMap

          prefs[PreferencesKeys.APP_USAGE_PROFILES] = Json.encodeToString(pruned)
      }
      // Also update legacy launch counts for backwards compatibility
      recordAppLaunch(componentKey)
  }

  suspend fun hideAppFromSuggestions(packageName: String) {
      dataStore.edit { prefs ->
          val current = prefs[PreferencesKeys.SUGGESTION_BLACKLIST]?.let {
              try { Json.decodeFromString<Set<String>>(it) } catch (_: Throwable) { emptySet() }
          } ?: emptySet()
          prefs[PreferencesKeys.SUGGESTION_BLACKLIST] = Json.encodeToString(current + packageName)
      }
  }

  suspend fun setShowDrawerSuggestions(enabled: Boolean) {
      dataStore.edit { prefs ->
          prefs[PreferencesKeys.SHOW_DRAWER_SUGGESTIONS] = enabled
      }
  }
  ```

- [ ] **Step 4: Run tests to verify they pass**

Run:
```powershell
.\gradlew :core:datastore:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL with all tests passing.

- [ ] **Step 5: Commit**

```bash
git add core/model/src/main/kotlin/com/urunkarpm/drawer/core/model/AppUsageProfile.kt
git add core/datastore/src/main/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSource.kt
git add core/datastore/src/test/kotlin/com/urunkarpm/drawer/core/datastore/DrawerPreferencesDataSourceTest.kt
git commit -m "feat(datastore): add AppUsageProfile and contextual launch tracking"
```

---

### Task 2: Pure Domain Prediction Engine (`AppPredictor`)

**Files:**
- Create: `core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/prediction/AppPredictor.kt`
- Test: `core/data/src/test/kotlin/com/urunkarpm/drawer/core/data/prediction/AppPredictorTest.kt`

**Interfaces:**
- Consumes:
  - `AppInfo` (`:core:model`)
  - `AppUsageProfile` (`:core:model`)
- Produces:
  - `class AppPredictor @Inject constructor()`
  - `fun predictTopApps(...): List<AppInfo>`

- [ ] **Step 1: Write the failing unit tests for `AppPredictor`**

Create `core/data/src/test/kotlin/com/urunkarpm/drawer/core/data/prediction/AppPredictorTest.kt`:
```kotlin
package com.urunkarpm.drawer.core.data.prediction

import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.AppUsageProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class AppPredictorTest {
    private lateinit var predictor: AppPredictor

    private fun createApp(packageName: String, label: String): AppInfo {
        return AppInfo(
            packageName = packageName,
            activityName = "MainActivity",
            label = label
        )
    }

    @Before
    fun setup() {
        predictor = AppPredictor()
    }

    @Test
    fun predictTopApps_excludesDockedApps() {
        val app1 = createApp("com.example.phone", "Phone")
        val app2 = createApp("com.example.browser", "Browser")
        val app3 = createApp("com.example.notes", "Notes")

        val profiles = mapOf(
            app1.componentKey to AppUsageProfile(totalScore = 10f),
            app2.componentKey to AppUsageProfile(totalScore = 8f),
            app3.componentKey to AppUsageProfile(totalScore = 5f)
        )

        val result = predictor.predictTopApps(
            installedApps = listOf(app1, app2, app3),
            usageProfiles = profiles,
            dockedPackages = setOf("com.example.phone"),
            blacklistedPackages = emptySet(),
            activeNotificationPackages = emptySet()
        )

        assertFalse(result.any { it.packageName == "com.example.phone" })
        assertTrue(result.any { it.packageName == "com.example.browser" })
    }

    @Test
    fun predictTopApps_boostsActiveNotifications() {
        val app1 = createApp("com.example.chat", "Chat")
        val app2 = createApp("com.example.game", "Game")

        // Chat has slightly lower base score, but active notification boosts it over Game
        val profiles = mapOf(
            app1.componentKey to AppUsageProfile(totalScore = 5f),
            app2.componentKey to AppUsageProfile(totalScore = 6f)
        )

        val result = predictor.predictTopApps(
            installedApps = listOf(app1, app2),
            usageProfiles = profiles,
            dockedPackages = emptySet(),
            blacklistedPackages = emptySet(),
            activeNotificationPackages = setOf("com.example.chat")
        )

        assertEquals("com.example.chat", result.first().packageName)
    }

    @Test
    fun predictTopApps_favorsWeekdayMorningHabits() {
        val workApp = createApp("com.work.slack", "Slack")
        val leisureApp = createApp("com.fun.netflix", "Netflix")

        // Work app launched exclusively weekday morning (slot 0)
        // Leisure app launched exclusively weekend evening (slot 2)
        val profiles = mapOf(
            workApp.componentKey to AppUsageProfile(
                totalScore = 10f,
                weekdaySlots = listOf(20, 0, 0, 0)
            ),
            leisureApp.componentKey to AppUsageProfile(
                totalScore = 10f,
                weekendSlots = listOf(0, 0, 20, 0)
            )
        )

        // Tuesday 09:00 AM
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 27, 9, 0, 0)
        }

        val result = predictor.predictTopApps(
            installedApps = listOf(workApp, leisureApp),
            usageProfiles = profiles,
            dockedPackages = emptySet(),
            blacklistedPackages = emptySet(),
            activeNotificationPackages = emptySet(),
            currentTimeMs = cal.timeInMillis
        )

        assertEquals("com.work.slack", result.first().packageName)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
.\gradlew :core:data:testDebugUnitTest --tests "com.urunkarpm.drawer.core.data.prediction.AppPredictorTest"
```
Expected: FAIL with compilation error (`Unresolved reference: AppPredictor`).

- [ ] **Step 3: Implement `AppPredictor`**

Create `core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/prediction/AppPredictor.kt`:
```kotlin
package com.urunkarpm.drawer.core.data.prediction

import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.AppUsageProfile
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class AppPredictor @Inject constructor() {

    fun predictTopApps(
        installedApps: List<AppInfo>,
        usageProfiles: Map<String, AppUsageProfile>,
        dockedPackages: Set<String>,
        blacklistedPackages: Set<String>,
        activeNotificationPackages: Set<String>,
        lastExitedPackage: String? = null,
        currentTimeMs: Long = System.currentTimeMillis(),
        maxSuggestions: Int = 6
    ): List<AppInfo> {
        if (installedApps.isEmpty()) return emptyList()

        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val currentSlot = when {
            hour in 6..11 -> 0
            hour in 12..16 -> 1
            hour in 17..21 -> 2
            else -> 3
        }

        fun isIgnoredSystemPackage(pkgName: String): Boolean {
            val pkg = pkgName.lowercase()
            return pkg.startsWith("com.google.android.gms") ||
                    pkg.startsWith("com.google.android.webview") ||
                    pkg.startsWith("com.android.webview") ||
                    pkg.startsWith("com.google.android.tts") ||
                    pkg.startsWith("com.android.carrierconfig") ||
                    pkg.startsWith("com.google.android.overlay") ||
                    pkg.startsWith("com.google.android.ext") ||
                    pkg.startsWith("com.android.stk") ||
                    pkg.startsWith("com.google.android.marvin.talkback") ||
                    pkg.startsWith("com.google.android.as") ||
                    pkg.startsWith("com.google.android.feedback")
        }

        fun scoreApp(app: AppInfo): Float {
            if (app.packageName in dockedPackages ||
                app.packageName in blacklistedPackages ||
                isIgnoredSystemPackage(app.packageName)
            ) {
                return 0f
            }

            val profile = usageProfiles[app.componentKey] ?: usageProfiles[app.packageName] ?: return 0f
            if (profile.totalScore <= 0f && profile.lastLaunchTime <= 0L) return 0f

            val elapsedDays = (currentTimeMs - profile.lastLaunchTime).coerceAtLeast(0L) / (1000.0 * 60.0 * 60.0 * 24.0)
            val decayedScore = (profile.totalScore * Math.pow(0.5, elapsedDays / 7.0)).toFloat()
            if (decayedScore <= 0f) return 0f

            val baseFrequency = sqrt(decayedScore) * 10f

            val slots = if (isWeekend) profile.weekendSlots else profile.weekdaySlots
            val slotCount = slots.getOrElse(currentSlot) { 0 }
            val totalLaunchesInDayType = slots.sum()
            val slotAffinity = if (totalLaunchesInDayType > 0) {
                1.0f + (slotCount.toFloat() / totalLaunchesInDayType.toFloat() * 0.5f)
            } else 1.0f

            val notificationBoost = if (app.packageName in activeNotificationPackages) 1.30f else 1.0f

            val sequenceBoost = if (lastExitedPackage != null && isCompanionPair(lastExitedPackage, app.packageName)) {
                1.25f
            } else 1.0f

            return baseFrequency * slotAffinity * notificationBoost * sequenceBoost
        }

        val eligibleApps = installedApps
            .filter { app ->
                app.packageName !in dockedPackages &&
                app.packageName !in blacklistedPackages &&
                app.label.isNotBlank() &&
                !isIgnoredSystemPackage(app.packageName)
            }
            .distinctBy { it.packageName }

        val scoredApps = eligibleApps
            .map { it to scoreApp(it) }
            .filter { it.second > 0f }
            .sortedByDescending { it.second }
            .map { it.first }

        return if (scoredApps.size >= maxSuggestions) {
            scoredApps.take(maxSuggestions)
        } else {
            // Cold-start fallback avoiding dock & blacklisted apps
            val pickedPackages = scoredApps.map { it.packageName }.toSet()
            val fallbackCandidates = eligibleApps.filter { it.packageName !in pickedPackages }
            val priorityKeywords = listOf(
                "dialer", "phone", "contact",
                "message", "sms", "chat", "whatsapp", "telegram",
                "chrome", "browser", "firefox",
                "camera", "gallery", "photos",
                "vending", "playstore", "store"
            )
            val sortedFallbacks = fallbackCandidates.sortedWith(
                compareByDescending<AppInfo> { app ->
                    val pkg = app.packageName.lowercase()
                    val label = app.label.lowercase()
                    if (priorityKeywords.any { pkg.contains(it) || label.contains(it) }) 1 else 0
                }.thenByDescending { it.installTimeMillis }
            )
            (scoredApps + sortedFallbacks).take(maxSuggestions)
        }
    }

    private fun isCompanionPair(previousPkg: String, candidatePkg: String): Boolean {
        val prev = previousPkg.lowercase()
        val candidate = candidatePkg.lowercase()
        return (prev.contains("camera") && (candidate.contains("gallery") || candidate.contains("photos"))) ||
                (prev.contains("bank") && candidate.contains("auth"))
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```powershell
.\gradlew :core:data:testDebugUnitTest --tests "com.urunkarpm.drawer.core.data.prediction.AppPredictorTest"
```
Expected: PASS with 3 tests completed successfully.

- [ ] **Step 5: Commit**

```bash
git add core/data/src/main/kotlin/com/urunkarpm/drawer/core/data/prediction/AppPredictor.kt
git add core/data/src/test/kotlin/com/urunkarpm/drawer/core/data/prediction/AppPredictorTest.kt
git commit -m "feat(data): implement AppPredictor contextual recommendation engine"
```

---

### Task 3: ViewModel & Screen Integration (Snapshot Freezing)

**Files:**
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModel.kt`
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`

**Interfaces:**
- Consumes:
  - `AppPredictor` (`:core:data`)
  - `appUsageProfiles` (`:core:datastore`)
  - `suggestionBlacklist` (`:core:datastore`)
  - `showDrawerSuggestions` (`:core:datastore`)
- Produces:
  - `val suggestedApps: StateFlow<List<AppInfo>>` in `HomeViewModel`
  - `fun freezeSuggestionsForDrawerOpen(...)`
  - `fun hideAppFromSuggestions(packageName: String)`

- [ ] **Step 1: Write unit test in HomeViewModelTest**

Add test to `feature/home/src/test/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModelTest.kt`:
```kotlin
@Test
fun testOpenAllApps_freezesPredictedSuggestions() = runTest {
    // When drawer opens, suggestedApps snapshot is taken
    viewModel.openAllApps()
    assertTrue(viewModel.isAllAppsOpen.value)
}
```

- [ ] **Step 2: Run test to verify existing tests pass**

Run:
```powershell
.\gradlew :feature:home:testDebugUnitTest --tests "com.urunkarpm.drawer.feature.home.HomeViewModelTest"
```

- [ ] **Step 3: Update `HomeViewModel` to calculate and freeze suggestions**

In `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModel.kt`:
- Inject `appPredictor: AppPredictor`.
- Collect `drawerPreferencesDataSource.appUsageProfiles`, `suggestionBlacklist`, and `showDrawerSuggestions`.
- Add:
  ```kotlin
  private val _suggestedApps = MutableStateFlow<List<AppInfo>>(emptyList())
  val suggestedApps: StateFlow<List<AppInfo>> = _suggestedApps.asStateFlow()

  val showDrawerSuggestions: StateFlow<Boolean> = drawerPreferencesDataSource.showDrawerSuggestions
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  fun refreshSuggestions(dockedPackages: Set<String>, activeNotificationPackages: Set<String>) {
      _suggestedApps.value = appPredictor.predictTopApps(
          installedApps = _installedApps.value,
          usageProfiles = currentProfiles,
          dockedPackages = dockedPackages,
          blacklistedPackages = currentBlacklist,
          activeNotificationPackages = activeNotificationPackages,
          currentTimeMs = System.currentTimeMillis()
      )
  }

  fun hideFromSuggestions(packageName: String) {
      viewModelScope.launch {
          drawerPreferencesDataSource.hideAppFromSuggestions(packageName)
      }
  }
  ```
- In `openAllApps()`, trigger `refreshSuggestions(...)` so the suggestions are locked in for the duration of the drawer session.

- [ ] **Step 4: Update `HomeScreen.kt` to consume `suggestedApps`**

In `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt`:
- Replace the legacy 100-line inline `suggestedApps` computation block with:
  ```kotlin
  val suggestedApps by homeViewModel.suggestedApps.collectAsStateWithLifecycle()
  val showDrawerSuggestions by homeViewModel.showDrawerSuggestions.collectAsStateWithLifecycle()
  ```
- In `AllAppsDrawer(...)`:
  ```kotlin
  frequentApps = if (showDrawerSuggestions) suggestedApps else emptyList(),
  ```

- [ ] **Step 5: Run tests and commit**

Run:
```powershell
.\gradlew :feature:home:testDebugUnitTest
```
Commit:
```bash
git add feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeViewModel.kt
git add feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/HomeScreen.kt
git commit -m "feat(home): integrate AppPredictor with frozen drawer suggestions"
```

---

### Task 4: UI Actions & Settings Opt-Out

**Files:**
- Modify: `feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AllAppsDrawer.kt`
- Modify: `feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsScreen.kt`

- [ ] **Step 1: Add "Hide from suggestions" to AppItemView in `AllAppsDrawer.kt`**

In `AllAppsDrawer.kt`:
- Add callback `onHideFromSuggestions: (AppInfo) -> Unit` to `AllAppsDrawer` and `FrequentAppsRow`.
- When long-pressing an app in the frequent apps row, provide an action item:
  ```kotlin
  // In the context options for frequent app
  DropdownMenuItem(
      text = { Text("Hide from suggestions") },
      onClick = {
          onHideFromSuggestions(app)
      },
      leadingIcon = {
          Icon(Icons.Default.VisibilityOff, contentDescription = null)
      }
  )
  ```

- [ ] **Step 2: Add Settings Switch in `SettingsScreen.kt`**

Under Drawer Settings in `SettingsScreen.kt`:
- Add a toggle row:
  - Title: "Show Suggested Apps"
  - Subtitle: "Surface relevant apps based on your routine and context"
  - Switch: bound to `showDrawerSuggestions`, calls `viewModel.setShowDrawerSuggestions(it)`.

- [ ] **Step 3: Run full verification build**

Run:
```powershell
.\gradlew assembleDebug testDebugUnitTest
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add feature/home/src/main/kotlin/com/urunkarpm/drawer/feature/home/component/AllAppsDrawer.kt
git add feature/settings/src/main/kotlin/com/urunkarpm/drawer/feature/settings/SettingsScreen.kt
git commit -m "feat(ui): add hide from suggestions action and settings toggle"
```
