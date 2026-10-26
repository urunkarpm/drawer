package com.urunkarpm.drawer.core.model

/**
 * Utility functions to dynamically compute dock capacity based on dock icon size
 * and device posture (foldable/tablet).
 */
object DockCapacityUtils {

    /**
     * Dynamically calculates maximum dock capacity based on icon size and posture.
     * When icon size is smaller, allows user to add more apps to the dock.
     *
     * Scalable phone breakpoints:
     * - iconSize <= 42dp: up to 8 apps
     * - iconSize <= 46dp: up to 7 apps
     * - iconSize <= 50dp: up to 6 apps
     * - iconSize > 50dp (default 56dp, 64dp, 72dp): 5 apps
     *
     * On foldables and tablets, capacity scales even further with posture.
     */
    fun calculateCapacity(
        iconSizeDp: Float,
        isFoldableOrTablet: Boolean = false,
        expandedMaxApps: Int = 8
    ): Int {
        val phoneCapacity = when {
            iconSizeDp <= 42f -> 8
            iconSizeDp <= 46f -> 7
            iconSizeDp <= 50f -> 6
            else -> 5
        }
        return if (isFoldableOrTablet) {
            val boost = (phoneCapacity - 5).coerceAtLeast(0)
            maxOf(expandedMaxApps + boost, phoneCapacity)
        } else {
            phoneCapacity
        }
    }
}
