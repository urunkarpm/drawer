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
