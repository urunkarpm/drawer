package com.urunkarpm.drawer.core.designsystem.theme

import androidx.compose.ui.graphics.Color

data class VibrantAccentColor(
    val id: String,
    val name: String,
    val colorHex: String,
    val color: Color
)

object VibrantAccentColors {
    const val ID_DYNAMIC = "DYNAMIC"

    val palettes = listOf(
        VibrantAccentColor(ID_DYNAMIC, "Wallpaper Dynamic", "#00000000", Color.Transparent),
        VibrantAccentColor("#3B82F6", "Electric Blue", "#3B82F6", Color(0xFF3B82F6)),
        VibrantAccentColor("#06B6D4", "Neon Cyan", "#06B6D4", Color(0xFF06B6D4)),
        VibrantAccentColor("#10B981", "Emerald Green", "#10B981", Color(0xFF10B981)),
        VibrantAccentColor("#84CC16", "Solar Lime", "#84CC16", Color(0xFF84CC16)),
        VibrantAccentColor("#F59E0B", "Amber Gold", "#F59E0B", Color(0xFFF59E0B)),
        VibrantAccentColor("#F97316", "Sunset Coral", "#F97316", Color(0xFFF97316)),
        VibrantAccentColor("#EF4444", "Crimson Red", "#EF4444", Color(0xFFEF4444)),
        VibrantAccentColor("#EC4899", "Hot Pink", "#EC4899", Color(0xFFEC4899)),
        VibrantAccentColor("#8B5CF6", "Vivid Violet", "#8B5CF6", Color(0xFF8B5CF6)),
        VibrantAccentColor("#6366F1", "Indigo Pop", "#6366F1", Color(0xFF6366F1))
    )

    fun find(id: String): VibrantAccentColor? = palettes.find { it.id.equals(id, ignoreCase = true) }
}
