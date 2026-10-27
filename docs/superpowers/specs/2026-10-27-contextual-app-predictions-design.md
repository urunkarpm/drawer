# Design Document: Contextual App Prediction Engine for Drawer

- **Status:** Approved
- **Date:** 2026-10-27
- **Target Platform:** Android (API 26+)
- **Architecture:** Clean Architecture (`:core:model`, `:core:datastore`, `:core:data`, `:feature:home`, `:feature:settings`)

---

## 1. Executive Summary & Motivation

Drawer currently renders a "Suggested Apps" row (`FrequentAppsRow` in `AllAppsDrawer.kt`) sitting directly above the bottom search bar. The existing algorithm in `HomeScreen.kt` relies on a rudimentary frecency heuristic:
$$\text{Score} = (\sqrt{\text{count}} \times 10 + 2) \times 0.5^{(\Delta \text{hours} / 48)} \times \text{time\_of\_day\_boost}$$

### Flaws in the Current Approach
1. **Single Timestamp Hazard**: Only one timestamp (`lastLaunch`) is tracked per app. Opening an app once late at night disrupts its habitual daytime affinity calculation.
2. **Calendar Blindness**: No distinction between Weekday (Mon–Fri work routine) and Weekend (Sat–Sun downtime).
3. **Monotonic Accumulation**: Raw `launchCount` grows indefinitely; an app launched 300 times months ago still outranks an active new app used daily.
4. **Dock Redundancy**: Apps already visible in the Floating Glass Dock frequently waste slots in the 6-app drawer recommendation row.
5. **No Sequence Awareness**: Chained app flows (e.g. Camera $\to$ Photos, Authenticator $\to$ Banking) are ignored.

### Core Objectives
1. **4-Pillar Contextual Frecency**: Model habitual routines via time-of-day slots, weekday/weekend splits, and a 7-day rolling half-life decay.
2. **Zero Extra Permissions & Zero Battery Cost**: 100% on-device, computed synchronously in $<2\text{ms}$ upon opening the drawer. No background location tracking or sensor polling.
3. **Smart Ecosystem Synergy**: Leverage Drawer's existing active notification state (`notificationsUiState`) and automatically deduplicate against the Floating Glass Dock.
4. **User Agency & Stability**: Freeze predictions while the drawer is open to prevent jumping icons; allow users to long-press to "Hide from suggestions"; provide an opt-out toggle in Settings.

---

## 2. Architecture & Data Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                        App Launch Trigger                              │
│       User taps an app in Dock, Drawer, or Home Accordion             │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                  DrawerPreferencesDataSource                           │
│  • Applies 7-day half-life decay to existing AppUsageProfile score     │
│  • Increments current time-slot counter (Weekday/Weekend × 4 slots)    │
│  • Stores updated AppUsageProfile map in DataStore                     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    │ (App closed / Drawer opened)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           AppPredictor                                 │
│  Pure Kotlin Domain Service (`:core:data:prediction`)                  │
│                                                                        │
│  Inputs:                                                               │
│   - installedApps: List<AppInfo>                                       │
│   - usageProfiles: Map<String, AppUsageProfile>                        │
│   - dockedPackages: Set<String> (Dock Deduplication)                   │
│   - blacklistedPackages: Set<String> ("Don't suggest")                 │
│   - activeNotificationPackages: Set<String> (Unread Alert Boost)       │
│   - lastExitedPackage: String? (App Transition Boost)                  │
│   - currentTimeMs: Long (Time slot & Day-of-week evaluation)           │
│                                                                        │
│  Output: List<AppInfo> (Top 6 ranked suggestions)                      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       HomeScreen / HomeViewModel                       │
│  • Captures snapshot when isAllAppsOpen transitions false -> true      │
│  • Freezes suggestions for current drawer session                      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   AllAppsDrawer -> FrequentAppsRow                     │
│  • Renders 6 stable icons                                              │
│  • Long-press menu item: "Hide from suggestions"                       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Data Model & Storage

### 3.1 `AppUsageProfile` (`:core:model`)
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

### 3.2 DataStore Keys (`:core:datastore`)
- `APP_USAGE_PROFILES`: String preference containing JSON serialized `Map<String, AppUsageProfile>`.
- `SUGGESTION_BLACKLIST`: String preference containing JSON serialized `Set<String>` (packages hidden by user).
- `SHOW_DRAWER_SUGGESTIONS`: Boolean preference (default: `true`).

### 3.3 Migration from Legacy Format
When `APP_USAGE_PROFILES` is empty but legacy `APP_LAUNCH_COUNTS` (`Map<String, Int>`) and `APP_LAST_LAUNCHED_TIMES` (`Map<String, Long>`) exist:
1. Initialize `totalScore` using $\min(\text{legacyCount}, 50) \times 0.5^{(\text{ageInDays} / 7.0)}$.
2. Set `lastLaunchTime = legacyTime`.
3. Distribute base count evenly across slots.
4. Persist newly formed `AppUsageProfile` map and retain legacy keys for rollback safety.

---

## 4. Scoring Algorithm Specification

For each installed, launchable application $A$:

### 4.1 Hard Exclusion Filters
An app receives a score of $0$ if:
- $A.\text{packageName} \in \text{dockedPackages}$ *(Eliminates dock duplication)*.
- $A.\text{packageName} \in \text{blacklistedPackages}$ *(User override)*.
- $A.\text{packageName} == \text{context.packageName}$ *(Drawer itself)*.
- $A.\text{packageName}$ is an ignored system package (e.g., `com.google.android.webview`, `com.google.android.gms`).

### 4.2 Score Calculation
For all eligible apps:

$$\text{FinalScore}(A) = \text{BaseFrequency}(A) \times \text{SlotAffinity}(A) \times \text{NotificationBoost}(A) \times \text{SequenceBoost}(A)$$

#### 1. Damped Base Frequency
$$\text{BaseFrequency}(A) = \sqrt{\text{DecayedScore}(A)} \times 10.0$$
where $\text{DecayedScore}(A) = \text{profile.totalScore} \times 0.5^{(\Delta \text{days} / 7.0)}$.

#### 2. Temporal Slot Affinity Multiplier
Let $s \in \{0, 1, 2, 3\}$ be the current time-slot:
- Morning ($06:00 - 12:00$): Slot 0
- Afternoon ($12:00 - 17:00$): Slot 1
- Evening ($17:00 - 22:00$): Slot 2
- Night ($22:00 - 06:00$): Slot 3

Let $d \in \{\text{Weekday}, \text{Weekend}\}$ be determined by `Calendar.DAY_OF_WEEK`.
$$\text{SlotCount} = \text{profile.slots}[d][s]$$
$$\text{TotalSlotLaunches} = \sum_{i=0}^3 \text{profile.slots}[d][i]$$

$$\text{SlotAffinity}(A) = \begin{cases} 
1.0 + \left(\frac{\text{SlotCount}}{\text{TotalSlotLaunches}} \times 0.5\right) & \text{if } \text{TotalSlotLaunches} > 0 \\
1.0 & \text{otherwise}
\end{cases}$$
*(Yields a multiplier between $1.0\times$ and $1.50\times$.)*

#### 3. Active Notification Boost
$$\text{NotificationBoost}(A) = \begin{cases}
1.30 & \text{if } A.\text{packageName} \in \text{activeNotificationPackages} \\
1.00 & \text{otherwise}
\end{cases}$$

#### 4. Chained Transition Boost
If an app was exited within the last 60 seconds ($T_{\text{now}} - T_{\text{exit}} \le 60\,000\text{ms}$):
$$\text{SequenceBoost}(A) = \begin{cases}
1.25 & \text{if } (A_{\text{last}}, A) \text{ is a paired transition} \\
1.00 & \text{otherwise}
\end{cases}$$

### 4.3 Cold-Start Fallback
If the number of apps with $\text{FinalScore} > 0$ is less than 6:
1. Take all eligible apps with score $> 0$.
2. Fill remaining slots from candidate apps that match standard priority keywords (`phone`, `message`, `sms`, `browser`, `chrome`, `camera`, `photos`, `vending`), ordered by install recency.
3. Crucially, the fallback candidate pool also **excludes** docked packages and blacklisted apps.

---

## 5. UI & Interaction Design

### 5.1 Stable Frozen Viewport (`AllAppsDrawer.kt`)
- `suggestedApps` is evaluated only upon drawer opening (`LaunchedEffect(visible)` or `remember(visible)`).
- When scrolling, scrubbing the alphabet index bar, or typing search queries, the recommendation row remains completely static to avoid touch misfires.

### 5.2 Contextual Menu ("Hide from suggestions")
- When long-pressing an app inside `FrequentAppsRow`:
  - Show the existing `AppActionBottomSheet` or context popup.
  - Add action: **"Hide from suggestions"** with a discreet eye-slash / block icon.
  - Selecting it immediately appends the app to `SUGGESTION_BLACKLIST` and triggers an instant recalculation of the row.

### 5.3 Settings Toggle (`SettingsScreen.kt`)
- Located under **Home Screen & Drawer Settings**:
  - **"Show Suggested Apps"** (Switch: On/Off).
  - Subtitle: *"Surface relevant apps based on your routine and context."*
  - When disabled, `FrequentAppsRow` collapses with zero vertical padding.

---

## 6. Verification & Testing Plan

1. **Unit Tests (`AppPredictorTest`)**:
   - `predictTopApps_excludesDockedApps`: Verifies docked apps never appear in suggestions.
   - `predictTopApps_boostsActiveNotifications`: Verifies apps with active notifications score higher than identical unnotified apps.
   - `predictTopApps_favorsWeekdayMorningHabits`: Verifies work apps rank first on Tuesday morning.
   - `predictTopApps_respectsBlacklist`: Verifies hidden packages are filtered out.
   - `predictTopApps_fallsBackGracefully`: Verifies clean cold-start essentials without duplicating dock items.
2. **Storage Migration Tests (`DrawerPreferencesDataSourceTest`)**:
   - Verifies legacy `APP_LAUNCH_COUNTS` migrate seamlessly to `AppUsageProfile` without data loss.
3. **Performance Benchmark**:
   - Verify execution time of `predictTopApps` with 150 installed apps is under 2ms on standard Android hardware.
