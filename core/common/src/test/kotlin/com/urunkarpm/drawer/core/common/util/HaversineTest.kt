package com.urunkarpm.drawer.core.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HaversineTest {

    @Test
    fun distanceMeters_samePoint_isZero() {
        val dist = Haversine.distanceMeters(37.7749, -122.4194, 37.7749, -122.4194)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun distanceMeters_knownLocations_calculatedAccurately() {
        // Paris (48.8566, 2.3522) to London (51.5074, -0.1278) ~ 343 km = 343,000 m
        val dist = Haversine.distanceMeters(48.8566, 2.3522, 51.5074, -0.1278)
        assertEquals(343556.0, dist, 2000.0) // within 2km tolerance for earth flattening
    }

    @Test
    fun isInsideCircle_withinRadius_returnsTrue() {
        // 0.0005 deg latitude ~ 55.6 meters
        val centerLat = 40.7128
        val centerLon = -74.0060
        val nearbyLat = 40.7132
        val nearbyLon = -74.0060

        val inside = Haversine.isInsideCircle(nearbyLat, nearbyLon, centerLat, centerLon, 100f)
        assertTrue(inside)
    }

    @Test
    fun isInsideCircle_outsideRadius_returnsFalse() {
        val centerLat = 40.7128
        val centerLon = -74.0060
        val farLat = 40.7200 // ~ 800m away
        val farLon = -74.0060

        val inside = Haversine.isInsideCircle(farLat, farLon, centerLat, centerLon, 200f)
        assertFalse(inside)
    }
}
