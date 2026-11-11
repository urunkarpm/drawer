package com.urunkarpm.drawer.core.common.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Haversine {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the great-circle distance between two points in meters using the Haversine formula.
     */
    fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(rLat1) * cos(rLat2) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Checks if a point (userLat, userLon) is within the geofence circle.
     * @param circleRadiusM Radius of the circle in meters.
     */
    fun isInsideCircle(
        userLat: Double,
        userLon: Double,
        placeLat: Double,
        placeLon: Double,
        circleRadiusM: Float
    ): Boolean {
        return distanceMeters(userLat, userLon, placeLat, placeLon) <= circleRadiusM
    }
}
