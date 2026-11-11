package com.urunkarpm.drawer.core.data.util

import android.content.Context
import android.content.res.Configuration

object DevicePostureUtils {
    const val FEATURE_HINGE_SENSOR = "android.hardware.sensor.hinge_angle"

    fun isFoldableOrTablet(context: Context, configuration: Configuration? = null): Boolean {
        val config = configuration ?: context.resources.configuration
        return context.packageManager.hasSystemFeature(FEATURE_HINGE_SENSOR) ||
            config.smallestScreenWidthDp >= 600 ||
            config.screenWidthDp >= 600
    }

    fun isOpenFoldableOrTablet(context: Context? = null, configuration: Configuration): Boolean =
        configuration.smallestScreenWidthDp >= 600 || configuration.screenWidthDp >= 600
}
