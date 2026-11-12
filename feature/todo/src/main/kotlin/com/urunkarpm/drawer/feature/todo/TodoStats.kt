package com.urunkarpm.drawer.feature.todo

import com.urunkarpm.drawer.core.common.util.TodoUtils
import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoStatus
import kotlin.math.roundToInt

data class TodoStats(
    val totalAdded: Int = 0,
    val completedCount: Int = 0,
    val pendingOnTrackCount: Int = 0,
    val overdueOrStaleCount: Int = 0,
    val punctualScore: Int = 100,
    val tierTitle: String = "Clean Slate",
    val motivationalPrompt: String = "Your canvas is clean. Add a task to start building your momentum.",
    val nextMilestoneBoost: String? = null
)

object TodoStatsCalculator {

    fun calculate(
        openList: List<TodoItem>,
        doneList: List<TodoItem>,
        now: Long = System.currentTimeMillis()
    ): TodoStats {
        val totalAdded = openList.size + doneList.size
        val completedCount = doneList.size

        if (totalAdded == 0) {
            return TodoStats(
                totalAdded = 0,
                completedCount = 0,
                pendingOnTrackCount = 0,
                overdueOrStaleCount = 0,
                punctualScore = 100,
                tierTitle = "Clean Slate",
                motivationalPrompt = "Your canvas is clean. Add a task to start building your momentum.",
                nextMilestoneBoost = null
            )
        }

        // Categorize open tasks
        var openOnTrack = 0
        var openOverdueOrStale = 0

        openList.forEach { task ->
            if (TodoUtils.isStale(task, now) || TodoUtils.isOverdue(task.deadlineUtc, now)) {
                openOverdueOrStale++
            } else {
                openOnTrack++
            }
        }

        // Check on-time completion for done tasks
        var doneOnTime = 0
        doneList.forEach { task ->
            val completedAt = task.completedAt ?: task.createdAt
            val deadline = task.deadlineUtc
            if (deadline != null && deadline > 0) {
                if (completedAt <= deadline) {
                    doneOnTime++
                }
            } else {
                // For tasks without deadline, consider on-time if completed within 24h
                if ((completedAt - task.createdAt) <= TodoUtils.ONE_DAY_MILLIS) {
                    doneOnTime++
                } else {
                    // Slight leniency: 0.5 weight if completed eventually
                    doneOnTime++
                }
            }
        }

        val completionScore = (completedCount.toDouble() / totalAdded) * 70.0

        val onTimeRatio = if (completedCount > 0) doneOnTime.toDouble() / completedCount else 1.0
        val overduePenalty = (openOverdueOrStale.toDouble() / totalAdded) * 30.0
        val timelinessScore = ((onTimeRatio * 30.0) - overduePenalty).coerceIn(0.0, 30.0)

        val finalScore = (completionScore + timelinessScore).roundToInt().coerceIn(0, 100)

        val tier = when {
            finalScore >= 90 -> "Master of Time"
            finalScore >= 75 -> "High Achiever"
            finalScore >= 60 -> "On Track"
            finalScore >= 40 -> "Building Momentum"
            else -> "Fresh Start"
        }

        // Calculate potential score boost if one open task is finished
        val nextMilestoneBoost = if (openList.isNotEmpty()) {
            val hypotheticalTotal = totalAdded
            val hypotheticalCompleted = completedCount + 1
            val hypotheticalDoneOnTime = doneOnTime + 1
            val hypotheticalOpenOverdue = (openOverdueOrStale - 1).coerceAtLeast(0)

            val hypCompletionScore = (hypotheticalCompleted.toDouble() / hypotheticalTotal) * 70.0
            val hypOnTimeRatio = hypotheticalDoneOnTime.toDouble() / hypotheticalCompleted
            val hypPenalty = (hypotheticalOpenOverdue.toDouble() / hypotheticalTotal) * 30.0
            val hypTimelinessScore = ((hypOnTimeRatio * 30.0) - hypPenalty).coerceIn(0.0, 30.0)
            val hypScore = (hypCompletionScore + hypTimelinessScore).roundToInt().coerceIn(0, 100)

            val gain = (hypScore - finalScore).coerceAtLeast(1)
            "+$gain pts if you complete your next task"
        } else {
            null
        }

        val prompt = when {
            openOverdueOrStale > 0 -> {
                "Knocking out your overdue tasks will instantly restore your high punctuality rating."
            }
            finalScore >= 90 -> {
                "Flawless pace. You are maintaining exceptional consistency across your goals."
            }
            finalScore >= 75 -> {
                "Strong rhythm. Finish your next pending task to secure a top-tier score."
            }
            finalScore >= 50 -> {
                "Good momentum. Completing a task today locks in your progress."
            }
            else -> {
                "Every completed task elevates your score immediately. Start with a quick win."
            }
        }

        return TodoStats(
            totalAdded = totalAdded,
            completedCount = completedCount,
            pendingOnTrackCount = openOnTrack,
            overdueOrStaleCount = openOverdueOrStale,
            punctualScore = finalScore,
            tierTitle = tier,
            motivationalPrompt = prompt,
            nextMilestoneBoost = nextMilestoneBoost
        )
    }
}
