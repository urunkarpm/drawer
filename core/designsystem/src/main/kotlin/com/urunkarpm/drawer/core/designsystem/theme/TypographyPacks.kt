package com.urunkarpm.drawer.core.designsystem.theme

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

val LocalHeaderFontFamily: ProvidableCompositionLocal<FontFamily> =
    staticCompositionLocalOf { FontFamily.Default }

val LocalAppLabelScale: ProvidableCompositionLocal<Float> =
    compositionLocalOf { 1.0f }

fun headerFontFamilyFromName(name: String): FontFamily = when (name) {
    "SYSTEM" -> FontFamily.SansSerif
    "SERIF" -> FontFamily.Serif
    "MONOSPACE" -> FontFamily.Monospace
    "CURSIVE" -> FontFamily.Cursive
    "TEXAS_TROUPE" -> TexasTroupeFontFamily
    else -> FontFamily.Default
}

fun labelScaleFromName(name: String): Float = when (name) {
    "SMALL" -> 0.85f
    "NORMAL" -> 1.0f
    "LARGE" -> 1.2f
    else -> 1.0f
}
