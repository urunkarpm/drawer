package com.urunkarpm.drawer.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DockCapacityUtilsTest {

    @Test
    fun defaultIconSize_yieldsFiveApps() {
        assertEquals(5, DockCapacityUtils.calculateCapacity(56f))
        assertEquals(5, DockCapacityUtils.calculateCapacity(64f))
        assertEquals(5, DockCapacityUtils.calculateCapacity(72f))
    }

    @Test
    fun smallerIconSizes_allowMoreApps() {
        assertEquals(6, DockCapacityUtils.calculateCapacity(48f))
        assertEquals(7, DockCapacityUtils.calculateCapacity(44f))
        assertEquals(8, DockCapacityUtils.calculateCapacity(40f))
    }

    @Test
    fun foldableOrTablet_expandsCapacity() {
        assertEquals(8, DockCapacityUtils.calculateCapacity(56f, isFoldableOrTablet = true, expandedMaxApps = 8))
        assertEquals(11, DockCapacityUtils.calculateCapacity(40f, isFoldableOrTablet = true, expandedMaxApps = 8))
    }
}
