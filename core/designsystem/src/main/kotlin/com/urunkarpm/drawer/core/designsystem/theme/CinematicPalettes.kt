package com.urunkarpm.drawer.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Curated cinematic color grading palettes representing signature film-stocks,
 * directors' palettes, and aesthetic color science.
 *
 * Each palette defines coherent harmonies for accents, glass surfaces,
 * specular rim lights, and atmospheric glow.
 */
data class CinematicPalette(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val surfaceColor: Color,
    val rimStart: Color,
    val rimEnd: Color,
    val glowColor: Color,
    val steps: List<Triple<Color, Color, Color>>,
    val isOledBlack: Boolean = false
)

object CinematicPalettes {
    const val ID_DEFAULT = "DEFAULT"
    const val ID_TEAL_AMBER = "TEAL_AMBER"
    const val ID_NORDIC_FROST = "NORDIC_FROST"
    const val ID_CYBERPUNK = "CYBERPUNK"
    const val ID_KODAK_PORTRA = "KODAK_PORTRA"
    const val ID_MARIANA_ABYSS = "MARIANA_ABYSS"

    val Default = CinematicPalette(
        id = ID_DEFAULT,
        name = "Original / Dynamic",
        subtitle = "Material You & Pantone",
        description = "System dynamic tonal scheme with Pantone chromatic peach and very peri highlights.",
        primaryAccent = Color(0xFF6366F1),
        secondaryAccent = Color(0xFFFFBE98),
        surfaceColor = Color(0xFF141218),
        rimStart = Color(0xFFFFBE98),
        rimEnd = Color(0xFF6667AB),
        glowColor = Color(0xFFFFBE98),
        steps = emptyList()
    )

    val TealAmber = CinematicPalette(
        id = ID_TEAL_AMBER,
        name = "Cinema Teal & Amber",
        subtitle = "Hollywood Split-Complementary",
        description = "Hollywood's quintessential grade. Radiant golden amber highlights against deep oceanic teal shadows create striking three-dimensional depth.",
        primaryAccent = Color(0xFFF59E0B),
        secondaryAccent = Color(0xFF0D9488),
        surfaceColor = Color(0xFF0F172A),
        rimStart = Color(0xFFF59E0B),
        rimEnd = Color(0xFF0D9488),
        glowColor = Color(0xFFFBBF24),
        steps = listOf(
            Triple(Color(0xFF0F172A), Color(0xFF0D9488), Color(0xFFF59E0B)),
            Triple(Color(0xFF134E4A), Color(0xFF0F172A), Color(0xFFD97706)),
            Triple(Color(0xFF0D9488), Color(0xFFF59E0B), Color(0xFF042F2E)),
            Triple(Color(0xFF1E293B), Color(0xFF14B8A6), Color(0xFFB45309))
        )
    )

    val NordicFrost = CinematicPalette(
        id = ID_NORDIC_FROST,
        name = "Nordic Frost & Glacier",
        subtitle = "Minimalist Scandi",
        description = "Scandinavian minimalism inspired by glacial twilights. Crisp glacial cyan and arctic periwinkle over deep atmospheric slate.",
        primaryAccent = Color(0xFF38BDF8),
        secondaryAccent = Color(0xFF818CF8),
        surfaceColor = Color(0xFF1E293B),
        rimStart = Color(0xFFF8FAFC),
        rimEnd = Color(0xFF38BDF8),
        glowColor = Color(0xFF7DD3FC),
        steps = listOf(
            Triple(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF38BDF8)),
            Triple(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF818CF8)),
            Triple(Color(0xFF0C4A6E), Color(0xFF1E293B), Color(0xFFE2E8F0)),
            Triple(Color(0xFF1E293B), Color(0xFF0284C7), Color(0xFF94A3B8))
        )
    )

    val TokyoCyberpunk = CinematicPalette(
        id = ID_CYBERPUNK,
        name = "Tokyo Cyberpunk",
        subtitle = "OLED Neo-Noir",
        description = "Midnight in Neo-Tokyo. Pitch black OLED canvas pierced by high-voltage neon cyan, ultraviolet, and hot magenta laser.",
        primaryAccent = Color(0xFF00F0FF),
        secondaryAccent = Color(0xFFFF007F),
        surfaceColor = Color(0xFF050508),
        rimStart = Color(0xFF00F0FF),
        rimEnd = Color(0xFFFF007F),
        glowColor = Color(0xFF00F0FF),
        steps = listOf(
            Triple(Color(0xFF050508), Color(0xFF18002E), Color(0xFF00F0FF)),
            Triple(Color(0xFF0A0017), Color(0xFFFF007F), Color(0xFF00F0FF)),
            Triple(Color(0xFF050508), Color(0xFF8B5CF6), Color(0xFFFF007F)),
            Triple(Color(0xFF001122), Color(0xFF00F0FF), Color(0xFFD946EF))
        ),
        isOledBlack = true
    )

    val KodakPortra = CinematicPalette(
        id = ID_KODAK_PORTRA,
        name = "Kodak Portra 400",
        subtitle = "Warm Analog Earth",
        description = "Legendary 35mm portrait film emulation. Soft highlight roll-off, terracotta amber, vintage olive greens, and deep espresso shadows.",
        primaryAccent = Color(0xFFEA580C),
        secondaryAccent = Color(0xFF84CC16),
        surfaceColor = Color(0xFF1C1917),
        rimStart = Color(0xFFFEF3C7),
        rimEnd = Color(0xFFEA580C),
        glowColor = Color(0xFFF59E0B),
        steps = listOf(
            Triple(Color(0xFF1C1917), Color(0xFF292524), Color(0xFFEA580C)),
            Triple(Color(0xFF292524), Color(0xFF44403C), Color(0xFF84CC16)),
            Triple(Color(0xFF1C1917), Color(0xFF78350F), Color(0xFFFEF3C7)),
            Triple(Color(0xFF292524), Color(0xFFB45309), Color(0xFF65A30D))
        )
    )

    val MarianaAbyss = CinematicPalette(
        id = ID_MARIANA_ABYSS,
        name = "Mariana Abyss",
        subtitle = "Bioluminescent Deep Ocean",
        description = "Deep oceanic trench realm. Midnight abyssal water illuminated by phosphor seafoam green and radiant electric turquoise.",
        primaryAccent = Color(0xFF10B981),
        secondaryAccent = Color(0xFF2DD4BF),
        surfaceColor = Color(0xFF0B132B),
        rimStart = Color(0xFF2DD4BF),
        rimEnd = Color(0xFF10B981),
        glowColor = Color(0xFF2DD4BF),
        steps = listOf(
            Triple(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF10B981)),
            Triple(Color(0xFF051923), Color(0xFF0B132B), Color(0xFF2DD4BF)),
            Triple(Color(0xFF003554), Color(0xFF0B132B), Color(0xFF67E8F9)),
            Triple(Color(0xFF0B132B), Color(0xFF0466C8), Color(0xFF34D399))
        )
    )

    val allPalettes: List<CinematicPalette> = listOf(
        Default,
        TealAmber,
        NordicFrost,
        TokyoCyberpunk,
        KodakPortra,
        MarianaAbyss
    )

    fun find(id: String?): CinematicPalette {
        if (id == null) return Default
        return allPalettes.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: Default
    }
}
