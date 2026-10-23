package com.urunkarpm.drawer.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ponytail: hardcoded color object avoids CompositionLocal overhead;
// ceiling: no per-system-theme variant (same tokens for light + dark); single source of truth.
// upgrade path: StealthColors(dark=..., light=...) factory if per-theme variants are ever needed.
object StealthColors {
    val cardBackground = Color(0xFF0A0A0A)

    // Contrast level presets — LOW is the least aggressive (still readable off-axis),
    // LOWEST is nearly invisible from the side.
    val titleLow     = Color(0xFF666666)
    val bodyLow      = Color(0xFF555555)

    val titleLower   = Color(0xFF4A4A4A)
    val bodyLower    = Color(0xFF3A3A3A)

    val titleLowest  = Color(0xFF333333)
    val bodyLowest   = Color(0xFF2A2A2A)

    const val iconAlpha = 0.50f

    /** Returns the title color for the given contrast level key ("LOW", "LOWER", "LOWEST"). */
    fun titleColor(contrastLevel: String): Color = when (contrastLevel) {
        "LOWER"  -> titleLower
        "LOWEST" -> titleLowest
        else     -> titleLow  // "LOW" + unknown keys
    }

    /** Returns the body color for the given contrast level key ("LOW", "LOWER", "LOWEST"). */
    fun bodyColor(contrastLevel: String): Color = when (contrastLevel) {
        "LOWER"  -> bodyLower
        "LOWEST" -> bodyLowest
        else     -> bodyLow
    }

    /** Returns the icon alpha for the given contrast level key ("LOW", "LOWER", "LOWEST"). */
    fun iconAlpha(contrastLevel: String): Float = when (contrastLevel) {
        "LOWEST" -> 0.28f
        "LOWER"  -> 0.38f
        else     -> iconAlpha
    }
}
