package com.urunkarpm.drawer.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class StealthColorsTest {

    @Test
    fun titleColor_returnsExpectedColorsForContrastLevels() {
        assertEquals(StealthColors.titleLow, StealthColors.titleColor("LOW"))
        assertEquals(StealthColors.titleLower, StealthColors.titleColor("LOWER"))
        assertEquals(StealthColors.titleLowest, StealthColors.titleColor("LOWEST"))
        assertEquals(StealthColors.titleLow, StealthColors.titleColor("UNKNOWN"))
    }

    @Test
    fun bodyColor_returnsExpectedColorsForContrastLevels() {
        assertEquals(StealthColors.bodyLow, StealthColors.bodyColor("LOW"))
        assertEquals(StealthColors.bodyLower, StealthColors.bodyColor("LOWER"))
        assertEquals(StealthColors.bodyLowest, StealthColors.bodyColor("LOWEST"))
        assertEquals(StealthColors.bodyLow, StealthColors.bodyColor("UNKNOWN"))
    }

    @Test
    fun iconAlpha_scalesWithContrastLevels() {
        assertEquals(0.50f, StealthColors.iconAlpha("LOW"), 0.001f)
        assertEquals(0.38f, StealthColors.iconAlpha("LOWER"), 0.001f)
        assertEquals(0.28f, StealthColors.iconAlpha("LOWEST"), 0.001f)
        assertEquals(0.50f, StealthColors.iconAlpha("UNKNOWN"), 0.001f)
    }

    @Test
    fun cardBackground_isDeepStealthBlack() {
        assertEquals(Color(0xFF0A0A0A), StealthColors.cardBackground)
    }
}
