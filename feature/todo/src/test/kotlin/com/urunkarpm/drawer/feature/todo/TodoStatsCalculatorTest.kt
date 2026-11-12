package com.urunkarpm.drawer.feature.todo

import com.urunkarpm.drawer.core.model.TodoItem
import com.urunkarpm.drawer.core.model.TodoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoStatsCalculatorTest {

    private fun containsEmoji(text: String): Boolean {
        // Regex checking for common emoji and symbol ranges
        val emojiRegex = Regex("[\\p{So}\\p{Cs}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]")
        return emojiRegex.containsMatchIn(text)
    }

    @Test
    fun calculate_emptyTasks_returnsCleanSlateWith100ScoreAndNoEmoji() {
        val stats = TodoStatsCalculator.calculate(emptyList(), emptyList(), now = 1000000L)

        assertEquals(0, stats.totalAdded)
        assertEquals(0, stats.completedCount)
        assertEquals(0, stats.pendingOnTrackCount)
        assertEquals(0, stats.overdueOrStaleCount)
        assertEquals(100, stats.punctualScore)
        assertEquals("Clean Slate", stats.tierTitle)
        assertFalse("Tier title contains emoji", containsEmoji(stats.tierTitle))
        assertFalse("Prompt contains emoji", containsEmoji(stats.motivationalPrompt))
    }

    @Test
    fun calculate_allCompletedOnTime_returnsMasterOfTime() {
        val now = 1000000L
        val doneTasks = listOf(
            TodoItem(id = 1, title = "Task 1", status = TodoStatus.DONE, deadlineUtc = now + 10000, completedAt = now),
            TodoItem(id = 2, title = "Task 2", status = TodoStatus.DONE, deadlineUtc = now + 20000, completedAt = now)
        )
        val stats = TodoStatsCalculator.calculate(openList = emptyList(), doneList = doneTasks, now = now)

        assertEquals(2, stats.totalAdded)
        assertEquals(2, stats.completedCount)
        assertEquals(0, stats.pendingOnTrackCount)
        assertEquals(0, stats.overdueOrStaleCount)
        assertEquals(100, stats.punctualScore)
        assertEquals("Master of Time", stats.tierTitle)
        assertFalse(containsEmoji(stats.tierTitle))
        assertFalse(containsEmoji(stats.motivationalPrompt))
    }

    @Test
    fun calculate_mixedPendingAndCompleted_computesCorrectSlicesAndBoost() {
        val now = 1000000L
        val doneTasks = listOf(
            TodoItem(id = 1, title = "Task 1", status = TodoStatus.DONE, completedAt = now)
        )
        val openTasks = listOf(
            TodoItem(id = 2, title = "Task 2", status = TodoStatus.OPEN, createdAt = now, deadlineUtc = now + 100000L),
            TodoItem(id = 3, title = "Task 3", status = TodoStatus.OPEN, createdAt = now, deadlineUtc = now - 50000L) // overdue
        )

        val stats = TodoStatsCalculator.calculate(openList = openTasks, doneList = doneTasks, now = now)

        assertEquals(3, stats.totalAdded)
        assertEquals(1, stats.completedCount)
        assertEquals(1, stats.pendingOnTrackCount)
        assertEquals(1, stats.overdueOrStaleCount)
        assertTrue("Score should reflect partial completion and overdue penalty", stats.punctualScore in 0..80)
        assertFalse(containsEmoji(stats.tierTitle))
        assertFalse(containsEmoji(stats.motivationalPrompt))
        val boost = stats.nextMilestoneBoost
        assertNotNull(boost)
        assertTrue(boost!!.isNotEmpty())
        assertFalse(containsEmoji(boost))
    }
}
