package com.urunkarpm.drawer.feature.settings.subscreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.urunkarpm.drawer.core.designsystem.component.DockSurfaceBackground
import com.urunkarpm.drawer.core.designsystem.theme.CinematicPalettes
import com.urunkarpm.drawer.core.designsystem.theme.VibrantAccentColors
import com.urunkarpm.drawer.core.designsystem.theme.headerFontFamilyFromName
import com.urunkarpm.drawer.core.designsystem.theme.labelScaleFromName
import com.urunkarpm.drawer.core.designsystem.theme.rememberDeviceScreenCurve
import com.urunkarpm.drawer.feature.settings.SettingsUiState
import com.urunkarpm.drawer.feature.settings.SettingsViewModel
import com.urunkarpm.drawer.feature.settings.component.SettingsDivider
import com.urunkarpm.drawer.feature.settings.component.SettingsSegmentedRow
import com.urunkarpm.drawer.feature.settings.component.SettingsSliderRow
import com.urunkarpm.drawer.feature.settings.component.SettingsSwitchRow

@Composable
fun ThemesSubScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSegmentedRow(
                    options = listOf(
                        "SYSTEM" to "System",
                        "LIGHT" to "Light",
                        "DARK" to "Dark",
                        "AMOLED" to "AMOLED"
                    ),
                    selected = uiState.themeMode,
                    onSelect = { viewModel.setThemeMode(it) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "App Drawer Theme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Background style for the full app drawer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSegmentedRow(
                    options = listOf(
                        "WHITE" to "White",
                        "DARK" to "Dark",
                        "SYSTEM" to "System"
                    ),
                    selected = if (uiState.drawerThemeMode == "LIGHT") "WHITE" else uiState.drawerThemeMode,
                    onSelect = { viewModel.setDrawerThemeMode(it) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "App Drawer Look",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Choose solid, crystalline liquid glass, or frosted satin diffusion",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSegmentedRow(
                    options = listOf(
                        "SOLID" to "Solid",
                        "LIQUID_GLASS" to "Liquid Glass",
                        "FROSTED_GLASS" to "Frosted Glass"
                    ),
                    selected = uiState.drawerLook,
                    onSelect = { viewModel.setDrawerLook(it) }
                )
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    title = "Dynamic Color (Material You)",
                    subtitle = "Derive accent colors from your wallpaper",
                    checked = uiState.dynamicColor,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "Vibrant Accent Color",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Bold tones for highlights, sliders, and buttons",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VibrantAccentColors.palettes.forEach { palette ->
                        val isSelected = uiState.accentColor.equals(palette.id, ignoreCase = true)
                        val isDynamic = palette.id.equals(VibrantAccentColors.ID_DYNAMIC, ignoreCase = true)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    viewModel.setAccentColor(palette.id)
                                    if (isDynamic) {
                                        viewModel.setDynamicColor(true)
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isDynamic) {
                                            Modifier.background(
                                                Brush.sweepGradient(
                                                    listOf(
                                                        Color(0xFF3B82F6),
                                                        Color(0xFF10B981),
                                                        Color(0xFFF59E0B),
                                                        Color(0xFFEF4444),
                                                        Color(0xFF8B5CF6),
                                                        Color(0xFF3B82F6)
                                                    )
                                                )
                                            )
                                        } else {
                                            Modifier.background(palette.color)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 3.dp else 1.5.dp,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = palette.name,
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    title = "Auto-Open Keyboard in Drawer",
                    subtitle = "Focus search bar and pop up keyboard immediately upon opening All Apps",
                    checked = uiState.autoOpenKeyboardInDrawer,
                    onCheckedChange = { viewModel.setAutoOpenKeyboardInDrawer(it) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Blur App List on Open",
                    subtitle = "Obscure apps with a blur when opening All Apps until tapped",
                    checked = uiState.blurAppListOnOpen,
                    onCheckedChange = { viewModel.setBlurAppListOnOpen(it) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Haptic Feedback",
                    subtitle = "Tactile response on gestures, slider notches, and long-press actions",
                    checked = uiState.hapticFeedbackEnabled,
                    onCheckedChange = { viewModel.setHapticFeedbackEnabled(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun WallpaperSubScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Cinematic Color Grading",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Curated film palettes applied to dock glass, specular rims, and category cards",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CinematicPalettes.allPalettes.forEach { palette ->
                        val isSelected = uiState.cinematicPalette.equals(palette.id, ignoreCase = true)
                        Surface(
                            onClick = { viewModel.setCinematicPalette(palette.id) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                            },
                            border = if (isSelected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy((-6).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(palette.primaryAccent)
                                            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(palette.secondaryAccent)
                                            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(palette.rimStart)
                                            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = palette.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = palette.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsSwitchRow(
                    title = "Wallpaper Blur",
                    subtitle = "Applies real-time hardware blur across home wallpaper behind drawers",
                    checked = uiState.wallpaperBlur,
                    onCheckedChange = { viewModel.setWallpaperBlur(it) }
                )

                if (uiState.wallpaperBlur) {
                    SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsSliderRow(
                        title = "Wallpaper Blur Radius",
                        subtitle = "Softness and diffusion radius",
                        value = uiState.wallpaperBlurRadius,
                        onValueChange = { viewModel.setWallpaperBlurRadius(it) },
                        valueRange = 5f..80f,
                        valueLabel = "${uiState.wallpaperBlurRadius.toInt()} px"
                    )
                }

                SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                SettingsSwitchRow(
                    title = "Dynamic Pantone Gradient",
                    subtitle = "Cycles an ambient moving color mesh across the background",
                    checked = uiState.dynamicWallpaperEnabled,
                    onCheckedChange = { viewModel.setDynamicWallpaperEnabled(it) }
                )

                if (uiState.dynamicWallpaperEnabled) {
                    SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsSliderRow(
                        title = "Cycle Speed",
                        subtitle = "Time in seconds for full gradient cycle",
                        value = uiState.wallpaperGradientSpeedSeconds.toFloat(),
                        onValueChange = { viewModel.setWallpaperGradientSpeedSeconds(it.toInt()) },
                        valueRange = 2f..20f,
                        valueLabel = "${uiState.wallpaperGradientSpeedSeconds} s"
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsSliderRow(
                        title = "Gradient Blur",
                        subtitle = "Diffusion percentage across color steps",
                        value = uiState.wallpaperGradientBlurPercent.toFloat(),
                        onValueChange = { viewModel.setWallpaperGradientBlurPercent(it.toInt()) },
                        valueRange = 10f..80f,
                        valueLabel = "${uiState.wallpaperGradientBlurPercent}%"
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Gradient Palette",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSegmentedRow(
                        options = listOf(
                            "FULL_SPECTRUM" to "Rainbow",
                            "WARM_SUNSET" to "Sunset",
                            "COOL_OCEAN" to "Ocean",
                            "EMERALD" to "Forest"
                        ),
                        selected = uiState.wallpaperGradientPalette,
                        onSelect = { viewModel.setWallpaperGradientPalette(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Gradient Motion Angle",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSegmentedRow(
                        options = listOf(
                            "DIAGONAL" to "Diagonal",
                            "VERTICAL" to "Vertical",
                            "HORIZONTAL" to "Horizontal"
                        ),
                        selected = uiState.wallpaperGradientAngle,
                        onSelect = { viewModel.setWallpaperGradientAngle(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SurfacesSubScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Surface Aesthetic Style",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Appearance for the dock and category drawers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSegmentedRow(
                    options = listOf(
                        "LIQUID_GLASS" to "Glass",
                        "BLUR" to "Blur",
                        "SOLID" to "Solid",
                        "TRANSPARENT" to "Clear"
                    ),
                    selected = uiState.surfaceThemeStyle,
                    onSelect = { viewModel.setSurfaceThemeStyle(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Live Preview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val previewIcons = listOf(
                                Icons.Default.Phone to Color(0xFF10B981),
                                Icons.Default.Email to Color(0xFF3B82F6),
                                Icons.Default.CameraAlt to Color(0xFFEC4899),
                                Icons.Default.Language to Color(0xFFF59E0B)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(54.dp)
                                    .clip(RoundedCornerShape(uiState.surfaceCornerRadius.dp))
                            ) {
                                DockSurfaceBackground(
                                    backgroundStyle = uiState.surfaceThemeStyle,
                                    shape = RoundedCornerShape(uiState.surfaceCornerRadius.dp),
                                    surfaceOpacity = uiState.surfaceOpacity,
                                    surfaceBlurRadius = uiState.surfaceBlurRadius,
                                    surfaceStrokeOpacity = uiState.surfaceStrokeOpacity,
                                    isDropTarget = false,
                                    isMesmerizingGlow = uiState.dockMesmerizingGlow,
                                    cinematicPaletteId = uiState.cinematicPalette,
                                    modifier = Modifier.matchParentSize()
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    previewIcons.forEach { (icon, color) ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.surfaceThemeStyle != "TRANSPARENT" && uiState.surfaceThemeStyle != "SOLID") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsSliderRow(
                        title = "Surface Opacity",
                        subtitle = "Glass translucency level",
                        value = uiState.surfaceOpacity,
                        onValueChange = { viewModel.setSurfaceOpacity(it) },
                        valueRange = 0.10f..1.0f,
                        valueLabel = "${(uiState.surfaceOpacity * 100).toInt()}%"
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SettingsSliderRow(
                        title = "Blur Depth",
                        subtitle = "Frosted background blur intensity",
                        value = uiState.surfaceBlurRadius,
                        onValueChange = { viewModel.setSurfaceBlurRadius(it) },
                        valueRange = 5f..80f,
                        valueLabel = "${uiState.surfaceBlurRadius.toInt()} px"
                    )

                    if (uiState.surfaceThemeStyle == "LIQUID_GLASS") {
                        SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SettingsSliderRow(
                            title = "Glass Highlight",
                            subtitle = "Top rim specular light reflection",
                            value = uiState.surfaceStrokeOpacity,
                            onValueChange = { viewModel.setSurfaceStrokeOpacity(it) },
                            valueRange = 0f..1.0f,
                            valueLabel = "${(uiState.surfaceStrokeOpacity * 100).toInt()}%"
                        )
                    }
                }
            }
        }

        if (uiState.surfaceThemeStyle != "TRANSPARENT") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsSliderRow(
                        title = "Corner Curvature",
                        subtitle = "Border radius for dock and drawer surfaces",
                        value = uiState.surfaceCornerRadius,
                        onValueChange = { viewModel.setSurfaceCornerRadius(it) },
                        valueRange = 0f..36f,
                        steps = 8,
                        valueLabel = "${uiState.surfaceCornerRadius.toInt()} dp"
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))

                    val detectedCurve = rememberDeviceScreenCurve(fallbackDp = uiState.surfaceCornerRadius)
                    val curveSubtitle = if (detectedCurve.isHardwareDetected) {
                        "Auto-detected hardware display curve: ${detectedCurve.rawDetectedDp.toInt()} dp"
                    } else {
                        "Hugs physical screen corners on supported devices"
                    }

                    SettingsSwitchRow(
                        title = "Match Device Screen Curve",
                        subtitle = curveSubtitle,
                        checked = uiState.matchDeviceScreenCurve,
                        onCheckedChange = { viewModel.setMatchDeviceScreenCurve(it) }
                    )

                    if (uiState.matchDeviceScreenCurve) {
                        SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SettingsSliderRow(
                            title = "Curve Fine-Tuning",
                            subtitle = "Nudge drawer border curve to align with cases or screen protectors",
                            value = uiState.deviceCurveOffsetDp,
                            onValueChange = { viewModel.setDeviceCurveOffsetDp(it) },
                            valueRange = -12f..12f,
                            steps = 12,
                            valueLabel = "${if (uiState.deviceCurveOffsetDp > 0) "+" else ""}${uiState.deviceCurveOffsetDp.toInt()} dp"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TypographyIconsSubScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onShowIconPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Header Font Family",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Distinctive typography for drawer headers and title text",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val headerFonts = listOf(
                    "GOOGLE_SANS" to "Google Sans",
                    "SERIF" to "Classic Serif",
                    "SANS_SERIF" to "Clean Sans",
                    "MONOSPACE" to "Tech Mono",
                    "CURSIVE" to "Signature"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    headerFonts.forEach { (fontKey, fontLabel) ->
                        val isSelected = uiState.headerFontFamily == fontKey
                        Surface(
                            onClick = { viewModel.setHeaderFontFamily(fontKey) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Text(
                                text = fontLabel,
                                fontFamily = headerFontFamilyFromName(fontKey),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "App Label Scale",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Text sizing for app names in grid and dock",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                SettingsSegmentedRow(
                    options = listOf(
                        "NORMAL" to "Normal",
                        "SUBTLE" to "Subtle",
                        "MINIMAL" to "Minimal"
                    ),
                    selected = uiState.appLabelScale,
                    onSelect = { viewModel.setAppLabelScale(it) }
                )
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Icon Packs & Adaptive Shapes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Custom icon packs and geometric masks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onShowIconPicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.activeIconPack != null) "Manage Icon Packs (Active)" else "Choose Icon Pack"
                    )
                }

                SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "Adaptive Icon Shape",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSegmentedRow(
                    options = listOf(
                        "CIRCLE" to "Circle",
                        "SQUIRCLE" to "Squircle",
                        "ROUNDED_SQUARE" to "Square",
                        "TEARDROP" to "Teardrop"
                    ),
                    selected = uiState.adaptiveIconShape,
                    onSelect = { viewModel.setAdaptiveIconShape(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
