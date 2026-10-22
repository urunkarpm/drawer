package com.urunkarpm.drawer.core.data.util

import android.content.Context
import android.content.res.Configuration

/**
 * Utility functions to detect whether the user is running on a foldable device or a tablet,
 * and dynamically determine whether the device is currently in unfolded (open) or folded (closed) posture.
 */
object DevicePostureUtils {
    // Standard Android system feature for hinge angle sensor present on all modern foldables (Pixel Fold, Galaxy Fold/Flip, etc.)
    const val FEATURE_HINGE_SENSOR = "android.hardware.sensor.hinge_angle"

    fun isFoldable(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(FEATURE_HINGE_SENSOR)
    }

    fun isTablet(context: Context, configuration: Configuration? = null): Boolean {
        val config = configuration ?: context.resources.configuration
        return config.smallestScreenWidthDp >= 600
    }

    fun isFoldableOrTablet(context: Context, configuration: Configuration? = null): Boolean {
        val config = configuration ?: context.resources.configuration
        return isFoldable(context) || isTablet(context, config) || config.screenWidthDp >= 600
    }

    fun isOpenFoldableOrTablet(context: Context, configuration: Configuration): Boolean {
        // Tablets are always open/wide
        if (isTablet(context, configuration)) return true
        // For foldables or wide displays, open mode is active when screen width >= 600dp
        return configuration.screenWidthDp >= 600
    }
}
