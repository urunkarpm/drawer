package com.urunkarpm.drawer.core.designsystem.component

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Authentic Pantone Validated Colors covering the entire visual spectrum.
 */
object PantoneColors {
    // Warm / Reds / Corals
    val VivaMagenta = Color(0xFFBE3455)       // PANTONE 18-1750 (Color of the Year 2023)
    val LivingCoral = Color(0xFFFF6F61)       // PANTONE 16-1546 (Color of the Year 2019)
    val TangerineTango = Color(0xFFDD4124)    // PANTONE 17-1463 (Color of the Year 2012)
    val ChiliPepper = Color(0xFF9B1B30)       // PANTONE 19-1557 (Color of the Year 2007)
    val LavaFalls = Color(0xFFB4262A)         // PANTONE 18-1552

    // Peaches / Golds / Yellows
    val PeachFuzz = Color(0xFFFFBE98)         // PANTONE 13-1023 (Color of the Year 2024)
    val ApricotCrush = Color(0xFFFBB288)      // PANTONE 13-1025
    val Mimosa = Color(0xFFEFC050)            // PANTONE 14-0848 (Color of the Year 2009)
    val SolarPower = Color(0xFFF8CC46)        // PANTONE 13-0759
    val SunsetGold = Color(0xFFE09F3E)        // PANTONE 15-1054
    val CyberYellow = Color(0xFFFFD300)       // PANTONE 14-0760

    // Greens / Teals
    val Greenery = Color(0xFF88B04B)          // PANTONE 15-0343 (Color of the Year 2017)
    val Emerald = Color(0xFF009473)           // PANTONE 17-5641 (Color of the Year 2013)
    val MintLeaf = Color(0xFF00A877)          // PANTONE 15-5718
    val ElectricGreen = Color(0xFF84BD00)     // PANTONE 15-0545
    val DeepForest = Color(0xFF1D4335)        // PANTONE 19-5414
    val BiscayGreen = Color(0xFF00A699)       // PANTONE 15-5516

    // Blues / Cyans
    val CelestialBlue = Color(0xFF2E79B9)     // PANTONE 18-4530
    val ClassicBlue = Color(0xFF0F4C81)       // PANTONE 19-4052 (Color of the Year 2020)
    val Serenity = Color(0xFF92A8D1)          // PANTONE 15-3919 (Color of the Year 2016)
    val DeepCobalt = Color(0xFF1A3B68)        // PANTONE 19-4035
    val BlueIris = Color(0xFF5A5B9F)          // PANTONE 18-3943 (Color of the Year 2008)

    // Purples / Violets / Pinks
    val VeryPeri = Color(0xFF6667AB)          // PANTONE 17-3938 (Color of the Year 2022)
    val UltraViolet = Color(0xFF5F4B8B)       // PANTONE 18-3838 (Color of the Year 2018)
    val RadiantOrchid = Color(0xFFB163A3)     // PANTONE 18-3224 (Color of the Year 2014)
    val PrismViolet = Color(0xFF45284D)       // PANTONE 19-3540
    val DigitalLavender = Color(0xFFD4C9DF)   // PANTONE 14-3812
    val RoseQuartz = Color(0xFFF7CAC9)        // PANTONE 13-1520 (Color of the Year 2016)
    val FrenchRoast = Color(0xFF4A3B32)       // PANTONE 19-1012
}

/**
 * Curated Pantone harmonic color palettes spanning the full spectrum.
 */
data class PantonePalette(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<Triple<Color, Color, Color>>
)

object PantonePalettes {
    val FullSpectrum = PantonePalette(
        id = "FULL_SPECTRUM",
        name = "Full Spectrum Flow",
        description = "Harmonic continuous sweep across red, orange, gold, emerald, cobalt, violet, and magenta.",
        steps = listOf(
            Triple(PantoneColors.VivaMagenta, PantoneColors.TangerineTango, PantoneColors.PeachFuzz),
            Triple(PantoneColors.TangerineTango, PantoneColors.SolarPower, PantoneColors.Emerald),
            Triple(PantoneColors.Emerald, PantoneColors.MintLeaf, PantoneColors.CelestialBlue),
            Triple(PantoneColors.CelestialBlue, PantoneColors.ClassicBlue, PantoneColors.VeryPeri),
            Triple(PantoneColors.VeryPeri, PantoneColors.RadiantOrchid, PantoneColors.UltraViolet),
            Triple(PantoneColors.UltraViolet, PantoneColors.PrismViolet, PantoneColors.VivaMagenta),
            Triple(PantoneColors.LivingCoral, PantoneColors.PeachFuzz, PantoneColors.ClassicBlue)
        )
    )

    val PantoneCoty = PantonePalette(
        id = "PANTONE_COTY",
        name = "Colors of the Year",
        description = "Iconic Pantone Color of the Year selections from Peach Fuzz to Viva Magenta and Classic Blue.",
        steps = listOf(
            Triple(PantoneColors.PeachFuzz, PantoneColors.VivaMagenta, PantoneColors.VeryPeri),
            Triple(PantoneColors.VivaMagenta, PantoneColors.ClassicBlue, PantoneColors.LivingCoral),
            Triple(PantoneColors.ClassicBlue, PantoneColors.UltraViolet, PantoneColors.Greenery),
            Triple(PantoneColors.UltraViolet, PantoneColors.RoseQuartz, PantoneColors.Serenity),
            Triple(PantoneColors.RoseQuartz, PantoneColors.RadiantOrchid, PantoneColors.Emerald),
            Triple(PantoneColors.RadiantOrchid, PantoneColors.TangerineTango, PantoneColors.PeachFuzz)
        )
    )

    val AuroraBorealis = PantonePalette(
        id = "AURORA_BOREALIS",
        name = "Aurora Borealis",
        description = "Electric green, emerald, and deep celestial blues reminiscent of northern lights.",
        steps = listOf(
            Triple(PantoneColors.ElectricGreen, PantoneColors.Emerald, PantoneColors.CelestialBlue),
            Triple(PantoneColors.Emerald, PantoneColors.MintLeaf, PantoneColors.DeepForest),
            Triple(PantoneColors.MintLeaf, PantoneColors.CelestialBlue, PantoneColors.VeryPeri),
            Triple(PantoneColors.CelestialBlue, PantoneColors.DeepCobalt, PantoneColors.PrismViolet),
            Triple(PantoneColors.DeepCobalt, PantoneColors.ElectricGreen, PantoneColors.Emerald)
        )
    )

    val SunsetChroma = PantonePalette(
        id = "SUNSET_CHROMA",
        name = "Sunset Chroma",
        description = "Velvet sunset glow: Sunset Gold, Tangerine Tango, Viva Magenta, and Chili Pepper.",
        steps = listOf(
            Triple(PantoneColors.SunsetGold, PantoneColors.TangerineTango, PantoneColors.VivaMagenta),
            Triple(PantoneColors.TangerineTango, PantoneColors.ChiliPepper, PantoneColors.LavaFalls),
            Triple(PantoneColors.VivaMagenta, PantoneColors.PrismViolet, PantoneColors.SunsetGold),
            Triple(PantoneColors.ChiliPepper, PantoneColors.LivingCoral, PantoneColors.Mimosa)
        )
    )

    val DeepTwilight = PantonePalette(
        id = "DEEP_TWILIGHT",
        name = "Deep Twilight",
        description = "Moody, executive night tones with deep cobalt, prism violet, and blue iris.",
        steps = listOf(
            Triple(PantoneColors.DeepCobalt, PantoneColors.BlueIris, PantoneColors.PrismViolet),
            Triple(PantoneColors.BlueIris, PantoneColors.ClassicBlue, PantoneColors.FrenchRoast),
            Triple(PantoneColors.PrismViolet, PantoneColors.UltraViolet, PantoneColors.DeepCobalt),
            Triple(PantoneColors.ClassicBlue, PantoneColors.PrismViolet, PantoneColors.BlueIris)
        )
    )

    val PastelDream = PantonePalette(
        id = "PASTEL_DREAM",
        name = "Pastel Dream",
        description = "Soft, atmospheric tones with Rose Quartz, Serenity, Digital Lavender, and Peach Fuzz.",
        steps = listOf(
            Triple(PantoneColors.PeachFuzz, PantoneColors.RoseQuartz, PantoneColors.Serenity),
            Triple(PantoneColors.RoseQuartz, PantoneColors.DigitalLavender, PantoneColors.ApricotCrush),
            Triple(PantoneColors.Serenity, PantoneColors.DigitalLavender, PantoneColors.PeachFuzz),
            Triple(PantoneColors.DigitalLavender, PantoneColors.ApricotCrush, PantoneColors.RoseQuartz)
        )
    )

    val allPalettes: List<PantonePalette> = listOf(
        FullSpectrum,
        PantoneCoty,
        AuroraBorealis,
        SunsetChroma,
        DeepTwilight,
        PastelDream
    )

    fun findPalette(id: String): PantonePalette {
        val found = allPalettes.find { it.id.equals(id, ignoreCase = true) }
        if (found != null) return found
        val cinematic = com.urunkarpm.drawer.core.designsystem.theme.CinematicPalettes.find(id)
        if (cinematic.steps.isNotEmpty()) {
            return PantonePalette(
                id = cinematic.id,
                name = cinematic.name,
                description = cinematic.description,
                steps = cinematic.steps
            )
        }
        return FullSpectrum
    }
}

/**
 * Dynamic Pantone Gradient Wallpaper with 40% blur and aesthetic multi-step transitions.
 * Changes colors continuously every [speedSeconds] (default 3 seconds).
 */
@Composable
fun DynamicPantoneWallpaper(
    modifier: Modifier = Modifier,
    speedSeconds: Int = 3,
    blurPercent: Int = 40,
    paletteId: String = "FULL_SPECTRUM",
    angleStyle: String = "DIAGONAL"
) {
    val palette = remember(paletteId) { PantonePalettes.findPalette(paletteId) }
    val steps = palette.steps
    val safeSpeed = speedSeconds.coerceIn(1, 60)
    val animDurationMs = safeSpeed * 1000

    var currentStepIndex by remember(paletteId) { mutableIntStateOf(0) }

    // ponytail: Periodic coroutine ticker advances color index; ceiling: Infinite Repeatable tween per channel is tied to fixed bounds; upgrade path: Continuous perlin noise or GLSL shader on GPU.
    LaunchedEffect(paletteId, safeSpeed) {
        while (true) {
            delay(animDurationMs.toLong())
            currentStepIndex = (currentStepIndex + 1) % steps.size
        }
    }

    val currentTriple = steps[currentStepIndex]

    val color1 by animateColorAsState(
        targetValue = currentTriple.first,
        animationSpec = tween(durationMillis = animDurationMs, easing = FastOutSlowInEasing),
        label = "pantoneColor1"
    )
    val color2 by animateColorAsState(
        targetValue = currentTriple.second,
        animationSpec = tween(durationMillis = animDurationMs, easing = FastOutSlowInEasing),
        label = "pantoneColor2"
    )
    val color3 by animateColorAsState(
        targetValue = currentTriple.third,
        animationSpec = tween(durationMillis = animDurationMs, easing = FastOutSlowInEasing),
        label = "pantoneColor3"
    )

    // Compute blur radius: default 40% maps to ~48dp blur on API 31+
    val blurRadiusDp = (blurPercent.coerceIn(0, 100) * 1.2f).dp

    val baseModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPercent > 0) {
        modifier.blur(radius = blurRadiusDp)
    } else {
        modifier
    }

    // Frosted atmospheric softening scrim (40% blur optical diffusion)
    // Provides silkiness and anti-banding even on displays with lower bit-depth
    val scrimAlpha = (blurPercent.coerceIn(0, 100) / 100f * 0.22f).coerceIn(0f, 0.40f)

    // ponytail: Draw directly via drawBehind deferring animated color state reads to draw phase, bypassing recomposition churn and maintaining steady 120 FPS
    Box(
        modifier = baseModifier
            .fillMaxSize()
            .drawBehind {
                val c1 = color1
                val c2 = color2
                val c3 = color3
                val brush = when (angleStyle.uppercase()) {
                    "VERTICAL" -> Brush.verticalGradient(listOf(c1, c2, c3))
                    "HORIZONTAL" -> Brush.horizontalGradient(listOf(c1, c2, c3))
                    "RADIAL" -> Brush.radialGradient(
                        colors = listOf(c1, c2, c3),
                        center = Offset.Unspecified,
                        radius = size.maxDimension
                    )
                    else -> Brush.linearGradient(
                        colors = listOf(c1, c2, c3),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                }
                drawRect(brush)
                if (scrimAlpha > 0f) {
                    drawRect(Color.Black.copy(alpha = scrimAlpha))
                }
            }
    )
}
