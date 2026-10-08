package com.urunkarpm.drawer.core.designsystem

import androidx.compose.ui.text.font.FontFamily
import com.urunkarpm.drawer.core.designsystem.theme.LocalAppLabelScale
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily
import com.urunkarpm.drawer.core.designsystem.theme.TexasTroupeFontFamily
import com.urunkarpm.drawer.core.designsystem.theme.headerFontFamilyFromName
import com.urunkarpm.drawer.core.designsystem.theme.labelScaleFromName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TypographyPacksTest {

    @Test
    fun headerFontFamilyFromName_mapsAllExpectedTokens() {
        assertEquals(FontFamily.SansSerif, headerFontFamilyFromName("SYSTEM"))
        assertEquals(FontFamily.Serif, headerFontFamilyFromName("SERIF"))
        assertEquals(FontFamily.Monospace, headerFontFamilyFromName("MONOSPACE"))
        assertEquals(FontFamily.Cursive, headerFontFamilyFromName("CURSIVE"))
        assertEquals(TexasTroupeFontFamily, headerFontFamilyFromName("TEXAS_TROUPE"))
    }

    @Test
    fun headerFontFamilyFromName_fallsBackToDefault() {
        assertEquals(FontFamily.Default, headerFontFamilyFromName("UNKNOWN"))
        assertEquals(FontFamily.Default, headerFontFamilyFromName(""))
    }

    @Test
    fun labelScaleFromName_mapsAllExpectedTokens() {
        assertEquals(0.85f, labelScaleFromName("SMALL"), 0.001f)
        assertEquals(1.0f, labelScaleFromName("NORMAL"), 0.001f)
        assertEquals(1.2f, labelScaleFromName("LARGE"), 0.001f)
    }

    @Test
    fun labelScaleFromName_fallsBackToNormal() {
        assertEquals(1.0f, labelScaleFromName("UNKNOWN"), 0.001f)
        assertEquals(1.0f, labelScaleFromName(""), 0.001f)
    }

    @Test
    fun compositionLocalsAreDefined() {
        assertNotNull(LocalHeaderFontFamily)
        assertNotNull(LocalAppLabelScale)
        assertEquals(FontFamily.Serif, LocalHeaderFontFamily.provides(FontFamily.Serif).value)
        assertEquals(1.2f, LocalAppLabelScale.provides(1.2f).value, 0.001f)
    }
}
