package com.urunkarpm.drawer.core.designsystem.haptics

import org.junit.Assert.assertNotNull
import org.junit.Test

class CrispHapticsTest {

    @Test
    fun noOpHapticsExecutesWithoutException() {
        val haptics: CrispHaptics = NoOpCrispHaptics
        haptics.click()
        haptics.tick()
        haptics.heavyClick()
        haptics.confirm()
        haptics.reject()
        haptics.dragStart()
        haptics.dragEnd()
        assertNotNull(haptics)
    }

    @Test
    fun defaultCrispHapticsWithNullContextViewExecutesSafely() {
        val haptics = DefaultCrispHaptics(context = null, view = null, enabled = true)
        haptics.click()
        haptics.tick()
        haptics.heavyClick()
        haptics.confirm()
        haptics.reject()
        haptics.dragStart()
        haptics.dragEnd()
        assertNotNull(haptics)
    }

    @Test
    fun defaultCrispHapticsDisabledExecutesSafely() {
        val haptics = DefaultCrispHaptics(context = null, view = null, enabled = false)
        haptics.click()
        haptics.tick()
        haptics.heavyClick()
        haptics.confirm()
        haptics.reject()
        haptics.dragStart()
        haptics.dragEnd()
        assertNotNull(haptics)
    }
}
