package com.urunkarpm.drawer.core.designsystem.theme

import android.annotation.SuppressLint
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF06B6D4),
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Color(0xFF8B5CF6),
    onTertiary = Color(0xFF2E1065),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2E2C34),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFF0D0B10),
    surfaceContainerLow = Color(0xFF18161D),
    surfaceContainer = Color(0xFF1E1B23),
    surfaceContainerHigh = Color(0xFF27242C),
    surfaceContainerHighest = Color(0xFF322F38),
    outline = Color(0xFF6B7280),
    outlineVariant = Color(0xFF374151)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF155E75),
    tertiary = Color(0xFF7C3AED),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0)
)

/**
 * Extracts vibrant dominant color from the system WallpaperManager.
 * Prioritizes high saturation and vivid tones over muted background tones.
 */
@SuppressLint("MissingPermission")
fun extractWallpaperPrimaryColor(context: Context): Color? {
    val wm = runCatching { WallpaperManager.getInstance(context) }.getOrNull() ?: return null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        val sysColors = runCatching { wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
        val lockColors = runCatching { wm.getWallpaperColors(WallpaperManager.FLAG_LOCK) }.getOrNull()
        val colors = sysColors ?: lockColors

        if (colors != null) {
            val candidates = listOfNotNull(
                colors.primaryColor?.let { Color(it.toArgb()) },
                colors.secondaryColor?.let { Color(it.toArgb()) },
                colors.tertiaryColor?.let { Color(it.toArgb()) }
            )

            // Score candidates for vibrancy (boost saturation, penalize near-black and near-white)
            val mostVibrant = candidates.maxByOrNull { color ->
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                val sat = hsv[1]
                val value = hsv[2]
                if (value < 0.12f || (value > 0.92f && sat < 0.12f)) {
                    -1f
                } else {
                    sat * 2.5f + value
                }
            }

            if (mostVibrant != null) {
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(mostVibrant.toArgb(), hsv)
                if (hsv[1] > 0.15f) {
                    return mostVibrant
                }
            }

            if (colors.primaryColor != null) {
                return Color(colors.primaryColor.toArgb())
            }
        }
    }

    // Fallback: Sample from WallpaperManager drawable if accessible
    try {
        val drawable = wm.drawable
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val sampled = sampleVibrantColorFromBitmap(drawable.bitmap)
            if (sampled != null) return sampled
        }
    } catch (_: Exception) {}

    return null
}

private fun sampleVibrantColorFromBitmap(bitmap: Bitmap): Color? {
    return runCatching {
        val scaled = Bitmap.createScaledBitmap(bitmap, 36, 36, false)
        val pixels = IntArray(36 * 36)
        scaled.getPixels(pixels, 0, 36, 0, 0, 36, 36)
        if (scaled != bitmap) {
            scaled.recycle()
        }

        var bestColor: Color? = null
        var bestScore = -1f
        val hsv = FloatArray(3)

        for (p in pixels) {
            val a = (p shr 24) and 0xff
            if (a < 128) continue
            android.graphics.Color.colorToHSV(p, hsv)
            val sat = hsv[1]
            val value = hsv[2]

            if (value > 0.15f && (value < 0.92f || sat > 0.25f)) {
                val score = sat * 3f + (1f - kotlin.math.abs(value - 0.7f))
                if (score > bestScore) {
                    bestScore = score
                    bestColor = Color(p)
                }
            }
        }
        bestColor
    }.getOrNull()
}

/**
 * HSV-based Material You palette derivation generating vibrant, non-muted color schemes.
 * Ensures saturation is preserved and amplified for crisp, punchy UI highlights.
 */
fun generateColorSchemeFromSeed(
    seedColor: Color,
    darkTheme: Boolean,
    isAmoled: Boolean
): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seedColor.toArgb(), hsv)
    val hue = hsv[0]
    // Boost saturation to keep accent colors vibrant and never muted
    val sat = hsv[1].coerceIn(0.65f, 1.0f)

    fun hsvColor(h: Float, s: Float, v: Float): Color {
        return Color(android.graphics.Color.HSVToColor(floatArrayOf((h % 360f + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))
    }

    return if (darkTheme) {
        if (isAmoled) {
            darkColorScheme(
                primary = hsvColor(hue, sat, 0.94f),
                onPrimary = Color(0xFF0F172A),
                primaryContainer = hsvColor(hue, sat.coerceIn(0.60f, 0.90f), 0.32f),
                onPrimaryContainer = hsvColor(hue, 0.15f, 0.98f),
                secondary = hsvColor(hue + 18f, (sat * 0.9f).coerceIn(0.55f, 0.90f), 0.88f),
                onSecondary = Color(0xFF0F172A),
                secondaryContainer = hsvColor(hue + 18f, 0.60f, 0.26f),
                onSecondaryContainer = hsvColor(hue + 18f, 0.20f, 0.95f),
                tertiary = hsvColor(hue + 50f, sat.coerceIn(0.60f, 1.0f), 0.92f),
                onTertiary = Color(0xFF1E1035),
                tertiaryContainer = hsvColor(hue + 50f, 0.60f, 0.26f),
                onTertiaryContainer = hsvColor(hue + 50f, 0.20f, 0.95f),
                background = AmoledBlack,
                onBackground = Color(0xFFFFFFFF),
                surface = AmoledBlack,
                onSurface = Color(0xFFFFFFFF),
                surfaceVariant = hsvColor(hue, 0.20f, 0.18f),
                onSurfaceVariant = Color(0xFFEDEDED),
                surfaceContainerLowest = Color(0xFF000000),
                surfaceContainerLow = Color(0xFF0D0D0D),
                surfaceContainer = AmoledBlack,
                surfaceContainerHigh = Color(0xFF121212),
                surfaceContainerHighest = Color(0xFF1C1C1E),
                outline = hsvColor(hue, 0.35f, 0.70f),
                outlineVariant = hsvColor(hue, 0.28f, 0.32f)
            )
        } else {
            darkColorScheme(
                primary = hsvColor(hue, sat, 0.92f),
                onPrimary = Color(0xFF0F172A),
                primaryContainer = hsvColor(hue, sat.coerceIn(0.60f, 0.90f), 0.35f),
                onPrimaryContainer = hsvColor(hue, 0.15f, 0.98f),
                secondary = hsvColor(hue + 18f, (sat * 0.9f).coerceIn(0.55f, 0.90f), 0.88f),
                onSecondary = Color(0xFF0F172A),
                secondaryContainer = hsvColor(hue + 18f, 0.60f, 0.30f),
                onSecondaryContainer = hsvColor(hue + 18f, 0.20f, 0.95f),
                tertiary = hsvColor(hue + 50f, sat.coerceIn(0.60f, 1.0f), 0.92f),
                onTertiary = Color(0xFF1E1035),
                tertiaryContainer = hsvColor(hue + 50f, 0.60f, 0.30f),
                onTertiaryContainer = hsvColor(hue + 50f, 0.20f, 0.95f),
                background = Color(0xFF141218),
                onBackground = Color(0xFFE6E1E5),
                surface = Color(0xFF141218),
                onSurface = Color(0xFFE6E1E5),
                surfaceVariant = hsvColor(hue, 0.22f, 0.26f),
                onSurfaceVariant = Color(0xFFCAC4D0),
                surfaceContainerLowest = Color(0xFF0D0B10),
                surfaceContainerLow = Color(0xFF18161D),
                surfaceContainer = Color(0xFF1E1B23),
                surfaceContainerHigh = Color(0xFF27242C),
                surfaceContainerHighest = Color(0xFF322F38),
                outline = hsvColor(hue, 0.30f, 0.65f),
                outlineVariant = hsvColor(hue, 0.25f, 0.35f)
            )
        }
    } else {
        lightColorScheme(
            primary = hsvColor(hue, sat, 0.58f),
            onPrimary = Color.White,
            primaryContainer = hsvColor(hue, 0.25f, 0.94f),
            onPrimaryContainer = hsvColor(hue, 0.95f, 0.28f),
            secondary = hsvColor(hue + 18f, sat.coerceIn(0.60f, 0.90f), 0.52f),
            onSecondary = Color.White,
            secondaryContainer = hsvColor(hue + 18f, 0.22f, 0.94f),
            onSecondaryContainer = hsvColor(hue + 18f, 0.90f, 0.24f),
            tertiary = hsvColor(hue + 50f, sat.coerceIn(0.60f, 0.95f), 0.52f),
            onTertiary = Color.White,
            tertiaryContainer = hsvColor(hue + 50f, 0.22f, 0.94f),
            onTertiaryContainer = hsvColor(hue + 50f, 0.90f, 0.24f),
            background = Color(0xFFFFFBFE),
            onBackground = Color(0xFF1C1B1F),
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = hsvColor(hue, 0.15f, 0.92f),
            onSurfaceVariant = Color(0xFF49454F),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF7F2FA),
            surfaceContainer = Color(0xFFF3EDF7),
            surfaceContainerHigh = Color(0xFFECE6F0),
            surfaceContainerHighest = Color(0xFFE6E0E9),
            outline = hsvColor(hue, 0.25f, 0.60f),
            outlineVariant = hsvColor(hue, 0.18f, 0.82f)
        )
    }
}

@Composable
fun DrawerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    dynamicColor: Boolean = true,
    accentColor: String = "DYNAMIC",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var wallpaperColor by remember { mutableStateOf(if (dynamicColor) extractWallpaperPrimaryColor(context) else null) }

    DisposableEffect(context, dynamicColor) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wm = runCatching { WallpaperManager.getInstance(context) }.getOrNull()
            val listener = WallpaperManager.OnColorsChangedListener { _, which ->
                if ((which and (WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)) != 0) {
                    wallpaperColor = extractWallpaperPrimaryColor(context)
                }
            }
            try {
                wm?.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
            } catch (_: Exception) {}
            onDispose {
                try {
                    wm?.removeOnColorsChangedListener(listener)
                } catch (_: Exception) {}
            }
        } else {
            onDispose {}
        }
    }

    val selectedCustomColor: Color? = if (!accentColor.equals("DYNAMIC", ignoreCase = true)) {
        runCatching { Color(android.graphics.Color.parseColor(accentColor)) }.getOrNull()
    } else {
        null
    }

    val colorScheme = when {
        selectedCustomColor != null -> {
            generateColorSchemeFromSeed(selectedCustomColor, darkTheme, isAmoled)
        }
        dynamicColor && wallpaperColor != null -> {
            generateColorSchemeFromSeed(wallpaperColor!!, darkTheme, isAmoled)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            generateColorSchemeFromSeed(base.primary, darkTheme, isAmoled)
        }
        darkTheme -> {
            generateColorSchemeFromSeed(Color(0xFF3B82F6), darkTheme = true, isAmoled = isAmoled)
        }
        else -> {
            generateColorSchemeFromSeed(Color(0xFF2563EB), darkTheme = false, isAmoled = false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
