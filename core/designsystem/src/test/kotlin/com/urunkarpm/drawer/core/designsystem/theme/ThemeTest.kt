package com.urunkarpm.drawer.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {

    @Test
    fun toMonochrome_pureWhiteAndBlack_remainWhiteAndBlack() {
        val white = Color.White.toMonochrome()
        assertEquals(1f, white.red, 0.001f)
        assertEquals(1f, white.green, 0.001f)
        assertEquals(1f, white.blue, 0.001f)

        val black = Color.Black.toMonochrome()
        assertEquals(0f, black.red, 0.001f)
        assertEquals(0f, black.green, 0.001f)
        assertEquals(0f, black.blue, 0.001f)
    }

    @Test
    fun toMonochrome_vibrantColors_desaturateToEqualRGB() {
        val red = Color.Red.toMonochrome()
        assertEquals(red.red, red.green, 0.001f)
        assertEquals(red.green, red.blue, 0.001f)
        assertEquals(0.2126f, red.red, 0.01f)

        val green = Color.Green.toMonochrome()
        assertEquals(green.red, green.green, 0.001f)
        assertEquals(green.green, green.blue, 0.001f)
        assertEquals(0.7152f, green.red, 0.01f)

        val blue = Color.Blue.toMonochrome()
        assertEquals(blue.red, blue.green, 0.001f)
        assertEquals(blue.green, blue.blue, 0.001f)
        assertEquals(0.0722f, blue.red, 0.01f)
    }

    @Test
    fun toMonochrome_preservesAlpha() {
        val halfAlphaColor = Color(1f, 0.5f, 0.2f, 0.42f).toMonochrome()
        assertEquals(0.42f, halfAlphaColor.alpha, 0.001f)
        assertEquals(halfAlphaColor.red, halfAlphaColor.green, 0.001f)
        assertEquals(halfAlphaColor.green, halfAlphaColor.blue, 0.001f)
    }

    @Test
    fun generateMonochromeColorScheme_darkTheme_returnsMonochromeGrayscaleTokens() {
        val scheme = generateMonochromeColorScheme(darkTheme = true, isAmoled = false)
        assertTrue(scheme.primary.red > 0.8f)
        assertEquals(scheme.primary.red, scheme.primary.green, 0.01f)
        assertEquals(scheme.primary.green, scheme.primary.blue, 0.01f)

        assertTrue(scheme.onPrimary.red < 0.2f)

        assertEquals(scheme.outline.red, scheme.outline.green, 0.01f)
        assertEquals(scheme.outline.green, scheme.outline.blue, 0.01f)
    }

    @Test
    fun generateMonochromeColorScheme_amoledTheme_usesTrueBlackBackground() {
        val scheme = generateMonochromeColorScheme(darkTheme = true, isAmoled = true)
        assertEquals(AmoledBlack, scheme.background)
        assertEquals(AmoledBlack, scheme.surface)
    }

    @Test
    fun generateMonochromeColorScheme_lightTheme_returnsDarkPrimaryAndLightSurfaces() {
        val scheme = generateMonochromeColorScheme(darkTheme = false, isAmoled = false)
        assertTrue(scheme.primary.red < 0.2f)
        assertEquals(Color.White, scheme.onPrimary)
        assertTrue(scheme.surface.red > 0.95f)
    }

    @Test
    fun monochromeWallpaperOverlay_hasDarkNeutralGreyTonesAndSemiTransparency() {
        assertEquals(0.702f, MonochromeWallpaperOverlay.alpha, 0.01f)
        assertEquals(MonochromeWallpaperOverlay.red, MonochromeWallpaperOverlay.green, 0.001f)
        assertEquals(MonochromeWallpaperOverlay.green, MonochromeWallpaperOverlay.blue, 0.001f)
        assertTrue(MonochromeWallpaperOverlay.red in 0.10f..0.20f)
    }
}
