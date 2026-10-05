package com.urunkarpm.drawer.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

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

// Explicit light scheme — ensures onSurface/onBackground are legible even when
// the system doesn't support dynamic colors (API < 31).
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

@Composable
fun DrawerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    if (darkTheme) {
        if (isAmoled) {
            // AMOLED: true black background + elevated tinted container so text is always visible
            colorScheme = colorScheme.copy(
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
                // Readable tinted primary container on pure-black background
                primaryContainer = Color(0xFF2C1F4D),
                onPrimaryContainer = Color(0xFFEADDFF),
                outline = Color(0xFF8E8E93),
                outlineVariant = Color(0xFF3A3A3C)
            )
        } else {
            // Standard dark (including dynamic dark): enforce legible text colours
            colorScheme = colorScheme.copy(
                onBackground = Color(0xFFE6E1E5),
                onSurface = Color(0xFFE6E1E5),
                onSurfaceVariant = Color(0xFFCAC4D0)
            )
        }
    } else {
        // Light (including dynamic light): enforce legible dark-on-light text colours
        colorScheme = colorScheme.copy(
            background = Color(0xFFFFFBFE),
            onBackground = Color(0xFF1C1B1F),
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            onSurfaceVariant = Color(0xFF49454F)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
