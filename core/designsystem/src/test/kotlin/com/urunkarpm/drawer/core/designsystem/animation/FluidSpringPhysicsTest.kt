package com.urunkarpm.drawer.core.designsystem.animation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FluidSpringPhysicsTest {

    @Test
    fun rubberBand_atZero_returnsZero() {
        val result = FluidSpringPhysics.rubberBand(0f, 360f)
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun rubberBand_negativeDistance_returnsZero() {
        val result = FluidSpringPhysics.rubberBand(-100f, 360f)
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun rubberBand_asymptotesTowardsCoefficient() {
        val c = 360f
        val small = FluidSpringPhysics.rubberBand(10f, c) // ~9.72
        val medium = FluidSpringPhysics.rubberBand(100f, c) // ~78.26
        val large = FluidSpringPhysics.rubberBand(1000f, c) // ~264.7
        val massive = FluidSpringPhysics.rubberBand(100000f, c) // ~358.7

        assertTrue(small < medium)
        assertTrue(medium < large)
        assertTrue(large < massive)
        assertTrue(massive < c)
    }

    @Test
    fun signedRubberBand_handlesPositiveAndNegative() {
        val c = 300f
        val pos = FluidSpringPhysics.signedRubberBand(100f, c)
        val neg = FluidSpringPhysics.signedRubberBand(-100f, c)

        assertEquals(pos, -neg, 0.001f)
        assertEquals((100f * 300f) / (100f + 300f), pos, 0.001f)
    }

    @Test
    fun boundedRubberBand_withinBounds_returnsIdentity() {
        val result = FluidSpringPhysics.boundedRubberBand(50f, 0f, 100f)
        assertEquals(50f, result, 0.001f)
    }

    @Test
    fun boundedRubberBand_outsideBounds_rubberBands() {
        val lower = FluidSpringPhysics.boundedRubberBand(-50f, 0f, 100f, 300f)
        assertTrue(lower < 0f)
        assertTrue(lower > -50f) // Resisted

        val upper = FluidSpringPhysics.boundedRubberBand(150f, 0f, 100f, 300f)
        assertTrue(upper > 100f)
        assertTrue(upper < 150f) // Resisted
    }

    @Test
    fun springConstants_matchFluidSettlingParameters() {
        assertEquals(320, FluidSpringPhysics.FLUID_SETTLING_DURATION_MS)
        assertEquals(380f, FluidSpringPhysics.FLUID_STIFFNESS, 0.01f)
        assertEquals(0.82f, FluidSpringPhysics.FLUID_DAMPING_RATIO, 0.01f)
    }
}
