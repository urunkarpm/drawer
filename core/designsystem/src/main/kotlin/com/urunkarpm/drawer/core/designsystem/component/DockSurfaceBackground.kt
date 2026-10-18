package com.urunkarpm.drawer.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun DockSurfaceBackground(
    backgroundStyle: String,
    shape: Shape,
    surfaceOpacity: Float,
    surfaceBlurRadius: Float,
    surfaceStrokeOpacity: Float,
    isDropTarget: Boolean = false,
    isMesmerizingGlow: Boolean = true,
    modifier: Modifier = Modifier
) {
    val opacity = surfaceOpacity.coerceIn(0.10f, 1.0f)
    val strokeAlpha = surfaceStrokeOpacity.coerceIn(0.0f, 1.0f)

    when {
        isDropTarget -> {
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.50f),
                shadowElevation = 8.dp,
                border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary),
                modifier = modifier
            ) {}
        }

        backgroundStyle == "TRANSPARENT" -> {
            // Completely clear floating dock with zero chrome
            Box(modifier = modifier)
        }

        backgroundStyle == "SOLID" -> {
            // 100% opaque, physical Material 3 shelf with elevation and crisp border
            Surface(
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 10.dp,
                tonalElevation = 6.dp,
                border = BorderStroke(
                    1.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
                ),
                modifier = modifier
            ) {}
        }

        backgroundStyle == "BLUR" -> {
            // Frosted glass / Acrylic: milky diffusion wash + soft translucent frosted border
            val frostedGradient = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = (0.28f * opacity).coerceIn(0f, 1f)),
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = (0.75f * opacity).coerceIn(0f, 1f)),
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = (0.85f * opacity).coerceIn(0f, 1f))
                )
            )
            val frostedBorder = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    colors = if (isMesmerizingGlow) listOf(
                        Color(0x99FFBE98), // Pantone Peach Fuzz ambient rim
                        Color.White.copy(alpha = (0.60f * opacity).coerceIn(0f, 1f)),
                        Color(0x806667AB)  // Pantone Very Peri ambient base
                    ) else listOf(
                        Color.White.copy(alpha = (0.50f * opacity).coerceIn(0f, 1f)),
                        Color.White.copy(alpha = (0.15f * opacity).coerceIn(0f, 1f)),
                        Color.White.copy(alpha = (0.35f * opacity).coerceIn(0f, 1f))
                    )
                )
            )

            Box(
                modifier = modifier
                    .clip(shape)
                    .background(frostedGradient)
                    .border(frostedBorder, shape)
            ) {
                // Subtle frosted inner diffuse overlay with chromatic refraction
                if (isMesmerizingGlow) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0x18FFBE98),
                                        Color.Transparent,
                                        Color(0x186667AB)
                                    )
                                )
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.05f))
                    )
                }
            }
        }

        else -> {
            // "LIQUID_GLASS": Specular multi-stop glass gradient, top rim shine, gleaming border
            val glassGradient = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = (0.30f * opacity).coerceIn(0f, 1f)),
                    MaterialTheme.colorScheme.surface.copy(alpha = (0.60f * opacity).coerceIn(0f, 1f)),
                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = (0.85f * opacity).coerceIn(0f, 1f))
                )
            )
            val glassBorder = BorderStroke(
                1.5.dp,
                Brush.verticalGradient(
                    colors = if (isMesmerizingGlow) listOf(
                        Color.White.copy(alpha = (0.95f * strokeAlpha).coerceIn(0f, 1f)),
                        Color(0xB3FFBE98), // Pantone Peach Fuzz prismatic refraction
                        Color(0x996667AB)  // Pantone Very Peri luminous glow
                    ) else listOf(
                        Color.White.copy(alpha = (0.90f * strokeAlpha).coerceIn(0f, 1f)),
                        Color.White.copy(alpha = (0.25f * strokeAlpha).coerceIn(0f, 1f)),
                        MaterialTheme.colorScheme.primary.copy(alpha = (0.45f * strokeAlpha).coerceIn(0f, 1f))
                    )
                )
            )

            Box(
                modifier = modifier
                    .clip(shape)
                    .background(glassGradient)
                    .border(glassBorder, shape)
            ) {
                // Specular chromatic top light sweep line across the top rim of the liquid glass
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = if (isMesmerizingGlow) listOf(
                                    Color.Transparent,
                                    Color(0xB3FFBE98),
                                    Color.White.copy(alpha = (0.95f * strokeAlpha).coerceIn(0f, 1f)),
                                    Color(0xB36667AB),
                                    Color.Transparent
                                ) else listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = (0.75f * strokeAlpha).coerceIn(0f, 1f)),
                                    Color.Transparent
                                )
                            )
                        )
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}
