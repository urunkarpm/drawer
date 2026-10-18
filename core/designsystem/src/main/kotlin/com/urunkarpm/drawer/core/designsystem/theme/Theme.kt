package com.urunkarpm.drawer.core.designsystem.theme

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
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
    primary = Purple80,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = PurpleGrey80,
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Pink80,
    onTertiary = Color(0xFF492532),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainer = Color(0xFF1D1B20),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
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
 * Extracts dominant primary wallpaper color from the system WallpaperManager.
 */
fun extractWallpaperPrimaryColor(context: Context): Color? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        val wm = runCatching { WallpaperManager.getInstance(context) }.getOrNull() ?: return null
        val colors = runCatching { wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
        if (colors?.primaryColor != null) {
            return Color(colors.primaryColor.toArgb())
        }
        val drawable = runCatching { wm.drawable }.getOrNull()
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (bitmap != null && !bitmap.isRecycled && bitmap.width > 0 && bitmap.height > 0) {
                return Color(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
            }
        }
    }
    return null
}

// ponytail: HSV-based Material You palette derivation generates harmonized tonal color schemes directly from the primary wallpaper color without requiring heavy material-color-utilities library; ceiling is simplified 3-tone harmony; upgrade path is full HCT / Cam16 palette generator.
private fun generateColorSchemeFromSeed(
    seedColor: Color,
    darkTheme: Boolean,
    isAmoled: Boolean
): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seedColor.toArgb(), hsv)
    val hue = hsv[0]
    val sat = hsv[1].coerceIn(0.20f, 0.85f)

    fun hsvColor(h: Float, s: Float, v: Float): Color {
        return Color(android.graphics.Color.HSVToColor(floatArrayOf((h % 360f + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))
    }

    return if (darkTheme) {
        if (isAmoled) {
            darkColorScheme(
                primary = hsvColor(hue, (sat * 0.75f).coerceIn(0.25f, 0.70f), 0.90f),
                onPrimary = hsvColor(hue, 0.85f, 0.15f),
                primaryContainer = hsvColor(hue, (sat * 0.85f).coerceIn(0.35f, 0.85f), 0.22f),
                onPrimaryContainer = hsvColor(hue, 0.30f, 0.92f),
                secondary = hsvColor(hue + 15f, (sat * 0.45f).coerceIn(0.18f, 0.50f), 0.80f),
                onSecondary = Color(0xFF1E1E1E),
                secondaryContainer = hsvColor(hue + 15f, (sat * 0.40f).coerceIn(0.20f, 0.50f), 0.22f),
                onSecondaryContainer = hsvColor(hue + 15f, 0.25f, 0.90f),
                tertiary = hsvColor(hue + 60f, (sat * 0.55f).coerceIn(0.20f, 0.60f), 0.85f),
                onTertiary = Color(0xFF2B1700),
                tertiaryContainer = hsvColor(hue + 60f, (sat * 0.50f).coerceIn(0.25f, 0.60f), 0.22f),
                onTertiaryContainer = hsvColor(hue + 60f, 0.30f, 0.92f),
                background = AmoledBlack,
                onBackground = Color(0xFFFFFFFF),
                surface = AmoledBlack,
                onSurface = Color(0xFFFFFFFF),
                surfaceVariant = hsvColor(hue, 0.12f, 0.16f),
                onSurfaceVariant = Color(0xFFEDEDED),
                surfaceContainerLowest = Color(0xFF000000),
                surfaceContainerLow = Color(0xFF0D0D0D),
                surfaceContainer = AmoledBlack,
                surfaceContainerHigh = Color(0xFF121212),
                surfaceContainerHighest = Color(0xFF1C1C1E),
                outline = hsvColor(hue, 0.20f, 0.60f),
                outlineVariant = hsvColor(hue, 0.20f, 0.28f)
            )
        } else {
            darkColorScheme(
                primary = hsvColor(hue, (sat * 0.70f).coerceIn(0.25f, 0.70f), 0.88f),
                onPrimary = hsvColor(hue, 0.85f, 0.18f),
                primaryContainer = hsvColor(hue, (sat * 0.80f).coerceIn(0.30f, 0.80f), 0.32f),
                onPrimaryContainer = hsvColor(hue, 0.30f, 0.92f),
                secondary = hsvColor(hue + 15f, (sat * 0.45f).coerceIn(0.18f, 0.50f), 0.78f),
                onSecondary = Color(0xFF2A2A2A),
                secondaryContainer = hsvColor(hue + 15f, (sat * 0.40f).coerceIn(0.20f, 0.50f), 0.28f),
                onSecondaryContainer = hsvColor(hue + 15f, 0.25f, 0.90f),
                tertiary = hsvColor(hue + 60f, (sat * 0.55f).coerceIn(0.20f, 0.60f), 0.82f),
                onTertiary = Color(0xFF331E00),
                tertiaryContainer = hsvColor(hue + 60f, (sat * 0.50f).coerceIn(0.25f, 0.60f), 0.28f),
                onTertiaryContainer = hsvColor(hue + 60f, 0.30f, 0.92f),
                background = Color(0xFF141218),
                onBackground = Color(0xFFE6E1E5),
                surface = Color(0xFF141218),
                onSurface = Color(0xFFE6E1E5),
                surfaceVariant = hsvColor(hue, 0.15f, 0.28f),
                onSurfaceVariant = Color(0xFFCAC4D0),
                surfaceContainerLowest = Color(0xFF0F0D13),
                surfaceContainerLow = Color(0xFF1D1B20),
                surfaceContainer = Color(0xFF211F26),
                surfaceContainerHigh = Color(0xFF2B2930),
                surfaceContainerHighest = Color(0xFF36343B),
                outline = hsvColor(hue, 0.20f, 0.60f),
                outlineVariant = hsvColor(hue, 0.20f, 0.35f)
            )
        }
    } else {
        lightColorScheme(
            primary = hsvColor(hue, sat.coerceAtLeast(0.60f), 0.50f),
            onPrimary = Color.White,
            primaryContainer = hsvColor(hue, (sat * 0.40f).coerceIn(0.12f, 0.40f), 0.92f),
            onPrimaryContainer = hsvColor(hue, 0.85f, 0.22f),
            secondary = hsvColor(hue + 15f, (sat * 0.45f).coerceIn(0.20f, 0.50f), 0.45f),
            onSecondary = Color.White,
            secondaryContainer = hsvColor(hue + 15f, (sat * 0.35f).coerceIn(0.10f, 0.35f), 0.93f),
            onSecondaryContainer = hsvColor(hue + 15f, 0.75f, 0.20f),
            tertiary = hsvColor(hue + 60f, (sat * 0.50f).coerceIn(0.25f, 0.55f), 0.45f),
            onTertiary = Color.White,
            tertiaryContainer = hsvColor(hue + 60f, (sat * 0.35f).coerceIn(0.10f, 0.35f), 0.93f),
            onTertiaryContainer = hsvColor(hue + 60f, 0.75f, 0.20f),
            background = Color(0xFFFFFBFE),
            onBackground = Color(0xFF1C1B1F),
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = hsvColor(hue, 0.15f, 0.90f),
            onSurfaceVariant = Color(0xFF49454F),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF7F2FA),
            surfaceContainer = Color(0xFFF3EDF7),
            surfaceContainerHigh = Color(0xFFECE6F0),
            surfaceContainerHighest = Color(0xFFE6E0E9),
            outline = hsvColor(hue, 0.20f, 0.55f),
            outlineVariant = hsvColor(hue, 0.15f, 0.80f)
        )
    }
}

@Composable
fun DrawerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var wallpaperColor by remember { mutableStateOf(if (dynamicColor) extractWallpaperPrimaryColor(context) else null) }

    DisposableEffect(context, dynamicColor) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wm = runCatching { WallpaperManager.getInstance(context) }.getOrNull()
            val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
                if ((which and WallpaperManager.FLAG_SYSTEM) != 0) {
                    wallpaperColor = if (colors?.primaryColor != null) {
                        Color(colors.primaryColor.toArgb())
                    } else {
                        extractWallpaperPrimaryColor(context)
                    }
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

    val colorScheme = when {
        dynamicColor && wallpaperColor != null -> {
            generateColorSchemeFromSeed(wallpaperColor!!, darkTheme, isAmoled)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && isAmoled) {
                base.copy(
                    background = AmoledBlack,
                    onBackground = Color(0xFFFFFFFF),
                    surface = AmoledBlack,
                    onSurface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFF1E1E1E),
                    onSurfaceVariant = Color(0xFFEDEDED),
                    surfaceContainer = AmoledBlack,
                    surfaceContainerLow = Color(0xFF0D0D0D),
                    surfaceContainerHigh = Color(0xFF121212),
                    surfaceContainerHighest = Color(0xFF1C1C1E),
                    primaryContainer = base.primary.copy(alpha = 0.25f),
                    onPrimaryContainer = Color(0xFFEADDFF),
                    outline = Color(0xFF8E8E93),
                    outlineVariant = Color(0xFF3A3A3C)
                )
            } else {
                base
            }
        }
        darkTheme -> {
            if (isAmoled) {
                DarkColorScheme.copy(
                    background = AmoledBlack,
                    onBackground = Color(0xFFFFFFFF),
                    surface = AmoledBlack,
                    onSurface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFF1E1E1E),
                    onSurfaceVariant = Color(0xFFEDEDED),
                    surfaceContainer = AmoledBlack,
                    surfaceContainerLow = Color(0xFF0D0D0D),
                    surfaceContainerHigh = Color(0xFF121212),
                    surfaceContainerHighest = Color(0xFF1C1C1E)
                )
            } else {
                DarkColorScheme
            }
        }
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
