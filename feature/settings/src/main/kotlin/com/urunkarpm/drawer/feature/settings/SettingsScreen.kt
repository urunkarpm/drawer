package com.urunkarpm.drawer.feature.settings

import androidx.compose.runtime.CompositionLocalProvider

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.urunkarpm.drawer.core.designsystem.theme.CinematicPalettes
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.data.util.DevicePostureUtils
import com.urunkarpm.drawer.core.designsystem.theme.DrawerTheme
import com.urunkarpm.drawer.core.designsystem.theme.GoogleSansFontFamily
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily
import com.urunkarpm.drawer.core.designsystem.theme.headerFontFamilyFromName
import com.urunkarpm.drawer.core.designsystem.theme.labelScaleFromName
import com.urunkarpm.drawer.feature.iconpacks.IconPackViewModel
import com.urunkarpm.drawer.feature.iconpacks.component.IconPackPickerSheet
import com.urunkarpm.drawer.feature.settings.component.SettingsDrawerCard
import com.urunkarpm.drawer.feature.settings.component.SettingsGlanceHeader
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DRAWER_APPEARANCE = "APPEARANCE"
private const val DRAWER_HOME = "HOME"
private const val DRAWER_PRIVACY = "PRIVACY"

private val ALL_DRAWER_KEYS = listOf(
    DRAWER_APPEARANCE,
    DRAWER_HOME,
    DRAWER_PRIVACY
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    iconPackViewModel: IconPackViewModel = hiltViewModel(),
    onOpenWalkthrough: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val iconPackUiState by iconPackViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val horizontalPadding = if (screenWidth < 360) 12.dp else if (screenWidth >= 840) 24.dp else 16.dp

    var showCityDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Expandable Drawers State
    var expandedDrawers by remember { mutableStateOf(setOf<String>()) }
    val toggleDrawer: (String) -> Unit = { drawerKey ->
        expandedDrawers = if (drawerKey in expandedDrawers) {
            expandedDrawers - drawerKey
        } else {
            expandedDrawers + drawerKey
        }
    }

    val cornerRadius = uiState.surfaceCornerRadius.dp

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // SAF Document Launchers for JSON Backup and Restore
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonContent = viewModel.getBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.bufferedWriter().use { writer ->
                                writer.write(jsonContent)
                            }
                        }
                    }
                    viewModel.notifyBackupExported()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonContent = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.bufferedReader().use { reader ->
                                reader.readText()
                            }
                        }
                    }
                    if (jsonContent != null) {
                        viewModel.restoreBackup(jsonContent)
                    } else {
                        snackbarHostState.showSnackbar("Failed to read selected file")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Restore failed: ${e.localizedMessage}")
                }
            }
        }
    }

    @Composable
    fun RenderDrawer(key: String, isCompact: Boolean = false) {
        when (key) {
            DRAWER_APPEARANCE -> {
                val themeLabel = when (uiState.themeMode) {
                    "SYSTEM" -> "System Theme"
                    "LIGHT" -> "Light Theme"
                    "DARK" -> "Dark Theme"
                    "AMOLED" -> "AMOLED Pure Black"
                    else -> uiState.themeMode
                }
                val selectedPalette = CinematicPalettes.find(uiState.cinematicPalette)
                val appearanceSummary = buildString {
                    append(themeLabel)
                    if (selectedPalette.id != CinematicPalettes.ID_DEFAULT) {
                        append(" • ")
                        append(selectedPalette.name)
                    } else {
                        append(" • ")
                        append(uiState.surfaceThemeStyle.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() })
                    }
                    if (uiState.dynamicWallpaperEnabled) {
                        append(" • Pantone (${uiState.wallpaperGradientSpeedSeconds}s)")
                    } else if (uiState.wallpaperBlur) {
                        append(" • Blur (${uiState.wallpaperBlurRadius.toInt()}px)")
                    }
                }

                SettingsDrawerCard(
                    title = "Surface & Appearance",
                    subtitle = appearanceSummary,
                    icon = Icons.Outlined.ColorLens,
                    iconTint = MaterialTheme.colorScheme.primary,
                    isExpanded = DRAWER_APPEARANCE in expandedDrawers,
                    onToggle = { toggleDrawer(DRAWER_APPEARANCE) },
                    isCompact = isCompact,
                    cornerRadius = cornerRadius
                ) {
                    // ── 1. Themes & Colors ──
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
                        text = "Clean, solid theme for the full app drawer",
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

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Dynamic Color (Material You)",
                        subtitle = "Derive accent colors from your wallpaper",
                        checked = uiState.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Auto-Open Keyboard in Drawer",
                        subtitle = "Focus search bar and pop up keyboard immediately upon opening All Apps",
                        checked = uiState.autoOpenKeyboardInDrawer,
                        onCheckedChange = { viewModel.setAutoOpenKeyboardInDrawer(it) }
                    )


                    SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ── Cinematic Color Grading Palettes ──
                    Text(
                        text = "Cinematic Color Grading",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Curated Hollywood & fine-art palettes applied to dock glass, specular rims, and category cards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                    // 3-tone color swatch pills
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
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = palette.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = palette.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ── 2. Surface & Glassmorphism ──
                    Text(
                        text = "Surface Aesthetic Style",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Unified aesthetic appearance for the dock and drawers",
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

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val previewShape = CircleShape
                                val previewIcons = listOf(
                                    Icons.Default.Phone to Color(0xFF10B981),
                                    Icons.Default.Email to Color(0xFF3B82F6),
                                    Icons.Default.CameraAlt to Color(0xFFEC4899),
                                    Icons.Default.Language to Color(0xFFF59E0B)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(60.dp)
                                        .clip(RoundedCornerShape(uiState.surfaceCornerRadius.dp))
                                ) {
                                    com.urunkarpm.drawer.core.designsystem.component.DockSurfaceBackground(
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
                                                    .size(36.dp)
                                                    .clip(previewShape)
                                                    .background(color.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = color,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.surfaceThemeStyle != "TRANSPARENT" && uiState.surfaceThemeStyle != "SOLID") {
                        Spacer(modifier = Modifier.height(10.dp))
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

                    if (uiState.surfaceThemeStyle != "TRANSPARENT") {
                        SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SettingsSliderRow(
                            title = "Corner Curvature",
                            subtitle = "Border radius for dock and drawer surfaces",
                            value = uiState.surfaceCornerRadius,
                            onValueChange = { viewModel.setSurfaceCornerRadius(it) },
                            valueRange = 0f..36f,
                            steps = 8,
                            valueLabel = "${uiState.surfaceCornerRadius.toInt()} dp"
                        )
                    }

                    SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ── 3. Wallpaper Blur & Gradient ──
                    SettingsSwitchRow(
                        title = "Wallpaper Blur",
                        subtitle = "Add frosted blur to home background wallpaper",
                        checked = uiState.wallpaperBlur,
                        onCheckedChange = { viewModel.setWallpaperBlur(it) }
                    )

                    if (uiState.wallpaperBlur) {
                        SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SettingsSliderRow(
                            title = "Wallpaper Blur Radius",
                            subtitle = "Blur intensity across wallpaper",
                            value = uiState.wallpaperBlurRadius,
                            onValueChange = { viewModel.setWallpaperBlurRadius(it) },
                            valueRange = 5f..100f,
                            steps = 18,
                            valueLabel = "${uiState.wallpaperBlurRadius.toInt()} px"
                        )
                    }

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Dynamic Pantone Wallpaper",
                        subtitle = "Smooth morphing Pantone validated gradient with 40% blur",
                        checked = uiState.dynamicWallpaperEnabled,
                        onCheckedChange = { viewModel.setDynamicWallpaperEnabled(it) }
                    )

                    if (uiState.dynamicWallpaperEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Gradient Shift Speed",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Frequency of smooth gradient color morphing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSegmentedRow(
                            options = listOf(
                                2 to "2s",
                                3 to "3s",
                                5 to "5s",
                                8 to "8s",
                                12 to "12s"
                            ),
                            selected = uiState.wallpaperGradientSpeedSeconds,
                            onSelect = { viewModel.setWallpaperGradientSpeedSeconds(it) }
                        )

                        SettingsDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Text(
                            text = "Pantone Harmonic Palette",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Harmonized authentic Pantone tones across the spectrum",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "FULL_SPECTRUM" to "Full Spectrum",
                                "PANTONE_COTY" to "Color of Year",
                                "AURORA_BOREALIS" to "Aurora",
                                "SUNSET_CHROMA" to "Sunset",
                                "DEEP_TWILIGHT" to "Twilight",
                                "PASTEL_DREAM" to "Pastel"
                            ).forEach { (paletteKey, label) ->
                                SettingsChoiceChip(
                                    label = label,
                                    selected = uiState.wallpaperGradientPalette == paletteKey,
                                    onClick = { viewModel.setWallpaperGradientPalette(paletteKey) }
                                )
                            }
                        }

                        SettingsDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Text(
                            text = "Gradient Flow Direction",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSegmentedRow(
                            options = listOf(
                                "DIAGONAL" to "Diagonal",
                                "VERTICAL" to "Vertical",
                                "RADIAL" to "Radial"
                            ),
                            selected = uiState.wallpaperGradientAngle,
                            onSelect = { viewModel.setWallpaperGradientAngle(it) }
                        )

                        SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))

                        SettingsSliderRow(
                            title = "Gradient Blur Intensity",
                            subtitle = "Frosted glass diffusion applied over gradient",
                            value = uiState.wallpaperGradientBlurPercent.toFloat(),
                            onValueChange = { viewModel.setWallpaperGradientBlurPercent(it.toInt()) },
                            valueRange = 0f..100f,
                            steps = 19,
                            valueLabel = "${uiState.wallpaperGradientBlurPercent}%"
                        )
                    }

                    SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ── 4. Typography & Font Packs ──
                    Text(
                        text = "Header & Clock Font",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose the display font for the clock, weather, and group headers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    var fontDropdownExpanded by remember { mutableStateOf(false) }
                    val currentFontKey = uiState.headerFontFamily
                    val currentFontFamily = headerFontFamilyFromName(currentFontKey)

                    val fontOptions = remember {
                        listOf(
                            "GOOGLE_SANS" to "Google Sans",
                            "SPOOKYY" to "Spookyy",
                            "DS_DIGITAL" to "DS-Digital",
                            "EVIL_EMPIRE" to "Evil Empire",
                            "HYPIK" to "Hypik",
                            "HELVETICA" to "Helvetica",
                            "ROBOTO" to "Roboto",
                            "TIMES_NEW_ROMAN" to "Times New Roman",
                            "ARIAL" to "Arial",
                            "TEXAS_TROUPE" to "Texas Troupe",
                            "SYSTEM" to "System Sans",
                            "MONOSPACE" to "Monospace",
                            "SERIF" to "Serif",
                            "CURSIVE" to "Cursive"
                        )
                    }
                    val currentFontDisplayName = fontOptions.find { it.first == currentFontKey }?.second
                        ?: currentFontKey.lowercase().replaceFirstChar { it.uppercase() }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { fontDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentFontDisplayName,
                                        // ponytail: font selector row label strictly uses Google Sans
                                        fontFamily = GoogleSansFontFamily,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Sample: 12:45 • Social • Notes",
                                        fontFamily = GoogleSansFontFamily,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    imageVector = if (fontDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = "Select font",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = fontDropdownExpanded,
                            onDismissRequest = { fontDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(max = 420.dp)
                        ) {
                            fontOptions.forEach { (key, label) ->
                                val font = headerFontFamilyFromName(key)
                                val isSelected = currentFontKey == key
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                                                Text(
                                                    text = label,
                                                    fontFamily = font,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "12:30 • Workspace • Apps",
                                                    fontFamily = font,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
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
                                    },
                                    onClick = {
                                        viewModel.setHeaderFontFamily(key)
                                        fontDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "12:45 PM • Social • Utilities",
                                fontFamily = headerFontFamilyFromName(uiState.headerFontFamily),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "App Label Size",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Scale font size for app titles across home, groups, and dock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSegmentedRow(
                        options = listOf(
                            "SMALL" to "Small",
                            "NORMAL" to "Default",
                            "LARGE" to "Large"
                        ),
                        selected = uiState.appLabelScale,
                        onSelect = { viewModel.setAppLabelScale(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ── 5. Icons & Shapes ──
                    val packLabel = iconPackUiState.installedPacks.firstOrNull { it.packageName == uiState.activeIconPack }?.name
                        ?: if (uiState.activeIconPack == null) "System Default" else uiState.activeIconPack
                    val shapeName = uiState.adaptiveIconShape.lowercase().replaceFirstChar { it.uppercase() }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showIconPicker = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Customize Icons & Shapes",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Pack: $packLabel • Shape: $shapeName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            DRAWER_HOME -> {
                val layoutMode = if (uiState.twoDrawersSideBySide) "Dual Column" else "Single Column"
                val alignmentLabel = if (uiState.categoryAlignment == "BOTTOM") "Bottom" else "Top"
                val homeSummary = "Alignment: $alignmentLabel • $layoutMode • ${uiState.dockExpandedMaxApps} Max Dock Apps"

                SettingsDrawerCard(
                    title = "Home & Ergonomics",
                    subtitle = homeSummary,
                    icon = Icons.Default.ViewCarousel,
                    iconTint = MaterialTheme.colorScheme.primary,
                    isExpanded = DRAWER_HOME in expandedDrawers,
                    onToggle = { toggleDrawer(DRAWER_HOME) },
                    isCompact = isCompact,
                    cornerRadius = cornerRadius
                ) {
                    // ── 1. Category Drawers & Thumb Zone ──
                    Text(
                        text = "Category Drawers & Thumb Zone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // ponytail: Category alignment toggle allows user to pick between Bottom thumb-zone alignment and Top stack; ceiling: Bottom or Top; upgrade path: custom vertical offset slider.
                    Text(
                        text = "Category Alignment",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Choose whether category drawers stack up from the bottom (natural thumb zone) or down from the glance header",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSegmentedRow(
                        options = listOf(
                            "BOTTOM" to "Bottom (Down)",
                            "TOP" to "Top (Up)"
                        ),
                        selected = uiState.categoryAlignment,
                        onSelect = { viewModel.setCategoryAlignment(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Two Drawers Side by Side",
                        subtitle = "Show category drawers in dual columns side by side on home page",
                        checked = uiState.twoDrawersSideBySide,
                        onCheckedChange = { viewModel.setTwoDrawersSideBySide(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Multi-Group Apps",
                        subtitle = "Allow a single app to be assigned to multiple categories",
                        checked = uiState.multiGroupApps,
                        onCheckedChange = { viewModel.setMultiGroupApps(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Lock Layout",
                        subtitle = "Prevent accidental dragging and category modifications",
                        checked = uiState.lockLayout,
                        onCheckedChange = { viewModel.setLockLayout(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Hide Drawer Icons",
                        subtitle = "Hide the folder and category icon badges on drawer headers",
                        checked = uiState.hideDrawerIcons,
                        onCheckedChange = { viewModel.setHideDrawerIcons(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Auto-Suggest Categories for New Apps",
                        subtitle = "Suggest drawer categories when installing new apps using on-device intelligence",
                        checked = uiState.autoCategorizeNewApps,
                        onCheckedChange = { viewModel.setAutoCategorizeNewApps(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Drawer Title Animation",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Animate drawer category title across the card when opening or closing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsSegmentedRow(
                            options = listOf(
                                "ACCENT_POP" to "Accent",
                                "FLUID_SPRING" to "Elastic",
                                "KINETIC_DECEL" to "Sleek",
                                "SNAPPY" to "Snappy",
                                "OFF" to "Off"
                            ),
                            selected = uiState.drawerTitleAnimation,
                            onSelect = { viewModel.setDrawerTitleAnimation(it) }
                        )
                    }

                    SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ── 2. Dock Bar ──
                    Text(
                        text = "Dock Bar & Navigation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    SettingsSliderRow(
                        title = "Dock Icon Size",
                        subtitle = "Scaled proportionally across all dock slots",
                        value = uiState.dockIconSize,
                        onValueChange = { viewModel.setDockIconSize(it) },
                        valueRange = 40f..72f,
                        steps = 7,
                        valueLabel = "${uiState.dockIconSize.toInt()} dp"
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingsSwitchRow(
                        title = "Show App Labels",
                        subtitle = "Display app names beneath dock icons",
                        checked = uiState.dockShowLabels,
                        onCheckedChange = { viewModel.setDockShowLabels(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingsSwitchRow(
                        title = "Free Drag & Auto-Arrange",
                        subtitle = "Freely slide apps left or right with instant auto-rearrangement",
                        checked = uiState.autoArrangeApps,
                        onCheckedChange = { viewModel.setAutoArrangeApps(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingsSwitchRow(
                        title = "Open Drawer From Dock Only",
                        subtitle = "Only open the app drawer when swiping up on the dock, disabling home screen swipe up",
                        checked = uiState.openDrawerFromDockOnly,
                        onCheckedChange = { viewModel.setOpenDrawerFromDockOnly(it) }
                    )

                    val isFoldableOrTablet = remember(context) {
                        DevicePostureUtils.isFoldableOrTablet(context)
                    }
                    if (isFoldableOrTablet) {
                        SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SettingsSliderRow(
                            title = "Foldable / Tablet Dock Capacity",
                            subtitle = "Number of app slots in unfolded posture",
                            value = uiState.dockExpandedMaxApps.toFloat(),
                            onValueChange = { viewModel.setDockExpandedMaxApps(it.toInt()) },
                            valueRange = 6f..10f,
                            steps = 3,
                            valueLabel = "${uiState.dockExpandedMaxApps} apps"
                        )
                    }

                    SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))

                    SettingsSwitchRow(
                        title = "Mesmerizing Dock Prismatic Glow",
                        subtitle = "Iridescent glass sheen catching light along the dock boundary",
                        checked = uiState.dockMesmerizingGlow,
                        onCheckedChange = { viewModel.setDockMesmerizingGlow(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ── 3. Glance Header & Widgets ──
                    Text(
                        text = "Glance Header & Widgets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    SettingsSwitchRow(
                        title = "24-Hour Clock",
                        subtitle = "Use 24-hour format instead of 12-hour AM/PM",
                        checked = uiState.is24Hour,
                        onCheckedChange = { viewModel.setIs24Hour(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Show Weather",
                        subtitle = "Display current temperature and weather conditions",
                        checked = uiState.showWeather,
                        onCheckedChange = { viewModel.setShowWeather(it) }
                    )

                    if (uiState.showWeather) {
                        SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "Temperature Unit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSegmentedRow(
                            options = listOf(
                                "CELSIUS" to "Celsius (°C)",
                                "FAHRENHEIT" to "Fahrenheit (°F)"
                            ),
                            selected = uiState.weatherUnit,
                            onSelect = { viewModel.setWeatherUnit(it) }
                        )

                        SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showCityDialog = true }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Weather Location",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = uiState.manualCityName?.let { "Manual: $it" } ?: "Auto (GPS Fused Location)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Show Status Indicators",
                        subtitle = "Show battery and Wi-Fi status indicator in between time and weather",
                        checked = uiState.showDuoStatusWidget,
                        onCheckedChange = { viewModel.setShowDuoStatusWidget(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Front Camera Mirror Shortcut",
                        subtitle = "Tap front camera punch hole to quickly open front camera mirror",
                        checked = uiState.enableCameraMirror,
                        onCheckedChange = { viewModel.setEnableCameraMirror(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Hide Status Bar",
                        subtitle = "Maximize screen real estate by hiding the system status bar",
                        checked = uiState.hideStatusBar,
                        onCheckedChange = { viewModel.setHideStatusBar(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Left-Hand Widget Page",
                        subtitle = "Enable dedicated full-page widget feed by swiping right from Home",
                        checked = uiState.enableWidgetsPage,
                        onCheckedChange = { viewModel.setEnableWidgetsPage(it) }
                    )
                }
            }

            DRAWER_PRIVACY -> {
                val installedVersion = remember(context) {
                    runCatching {
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        pInfo.versionName ?: "1.2.4"
                    }.getOrDefault("1.2.4")
                }
                val privacySummary = "Notifications • Local Backup • Version $installedVersion"

                SettingsDrawerCard(
                    title = "Privacy & Data",
                    subtitle = privacySummary,
                    icon = Icons.Default.Security,
                    iconTint = MaterialTheme.colorScheme.primary,
                    isExpanded = DRAWER_PRIVACY in expandedDrawers,
                    onToggle = { toggleDrawer(DRAWER_PRIVACY) },
                    isCompact = isCompact,
                    cornerRadius = cornerRadius
                ) {
                    // ── 1. Notification Hub & Stealth ──
                    Text(
                        text = "Notification Hub & Privacy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Listener Access",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Manage Android system notification access permission",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Privacy Mode",
                        subtitle = "Hide notification body content on the home screen",
                        checked = uiState.notificationsPrivacyMode,
                        onCheckedChange = { viewModel.setNotificationsPrivacyMode(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Glowing Drawer Icon",
                        subtitle = "Pulse glow the drawer icon when an app inside has a notification until acknowledged",
                        checked = uiState.drawerNotificationGlow,
                        onCheckedChange = { viewModel.setDrawerNotificationGlow(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Stealth Theme",
                        subtitle = "Makes notifications harder to read from the side. Not a security feature.",
                        checked = uiState.stealthTheme,
                        onCheckedChange = { viewModel.setStealthTheme(it) }
                    )

                    AnimatedVisibility(visible = uiState.stealthTheme) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Text(
                                text = "Contrast Level",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            SettingsSegmentedRow(
                                options = listOf(
                                    "LOW" to "Low",
                                    "LOWER" to "Lower",
                                    "LOWEST" to "Lowest"
                                ),
                                selected = uiState.stealthContrastLevel,
                                onSelect = { viewModel.setStealthContrastLevel(it) }
                            )

                            SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                            SettingsSwitchRow(
                                title = "Limit Brightness",
                                subtitle = "Dim the screen while the notification drawer is open",
                                checked = uiState.stealthLimitBrightness,
                                onCheckedChange = { viewModel.setStealthLimitBrightness(it) }
                            )
                        }
                    }

                    SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ── 2. Backup & Restore ──
                    Text(
                        text = "Backup & Restore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Export or import your entire configuration (categories, dock, mute rules, and personalisation settings) to a single JSON file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val fileName = "drawer_backup_${System.currentTimeMillis()}.json"
                                exportBackupLauncher.launch(fileName)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export")
                        }

                        OutlinedButton(
                            onClick = {
                                importBackupLauncher.launch(
                                    arrayOf(
                                        "application/json",
                                        "text/plain",
                                        "application/octet-stream",
                                        "text/json",
                                        "text/x-json"
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isRestoring) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import")
                        }
                    }

                    SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // ── 3. About Drawer ──
                    Text(
                        text = "About Drawer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version $installedVersion (API ${android.os.Build.VERSION.SDK_INT})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "A personalisation-first launcher built with 100% Jetpack Compose and offline privacy. Zero tracking, zero telemetry.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val webIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://uprasenjeet.vercel.app")
                            )
                            context.startActivity(webIntent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("uprasenjeet.vercel.app")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Walkthrough",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Revisit the first-install tour and feature highlights",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenWalkthrough
                        ) {
                            Text("Start Tour")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GitHub Updates",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Fetch and install latest releases directly from GitHub",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.checkForUpdates(installedVersion)
                            },
                            enabled = !uiState.isCheckingForUpdate
                        ) {
                            if (uiState.isCheckingForUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Checking…")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check")
                            }
                        }
                    }
                }
            }
        }
    }

    val isSystemDark = isSystemInDarkTheme()
    val isSettingsDark = when (uiState.themeMode) {
        "LIGHT" -> false
        "DARK", "AMOLED" -> true
        else -> isSystemDark
    }
    val isSettingsAmoled = uiState.themeMode == "AMOLED"

    // ponytail: Theme mode only styles the Settings panel; home screen overlay stays dark.
    DrawerTheme(
        darkTheme = isSettingsDark,
        isAmoled = isSettingsAmoled,
        dynamicColor = uiState.dynamicColor
    ) {
        // ponytail: Settings strictly enforces Google Sans font family regardless of user-selected launcher font
        CompositionLocalProvider(
            LocalHeaderFontFamily provides GoogleSansFontFamily
        ) {
            Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Settings",
                            fontFamily = GoogleSansFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Glance Header: Mirrors home screen glance aesthetics
                item {
                    SettingsGlanceHeader(
                        totalDrawers = ALL_DRAWER_KEYS.size,
                        openDrawersCount = expandedDrawers.size,
                        onToggleExpandAll = {
                            expandedDrawers = if (expandedDrawers.size == ALL_DRAWER_KEYS.size) {
                                emptySet()
                            } else {
                                ALL_DRAWER_KEYS.toSet()
                            }
                        },
                        cornerRadius = cornerRadius
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Settings Drawers: Always single column in Settings screen
                for (key in ALL_DRAWER_KEYS) {
                    item(key = key) {
                        RenderDrawer(key, isCompact = false)
                    }
                }

                // Bottom breathing space for smooth scrolling above system navigation
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // City Fallback Dialog
    if (showCityDialog) {
        var cityNameInput by remember { mutableStateOf(uiState.manualCityName ?: "") }

        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            title = { Text("Manual City Fallback") },
            text = {
                Column {
                    Text(
                        text = "Leave empty to use GPS Fused Location automatically, or enter a city name to override.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cityNameInput,
                        onValueChange = { cityNameInput = it },
                        label = { Text("City Name (e.g. London, Tokyo)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = cityNameInput.trim()
                        viewModel.setManualCityName(trimmed.ifEmpty { null })
                        showCityDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCityDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Icon Pack Picker Sheet
    if (showIconPicker) {
        IconPackPickerSheet(
            installedPacks = iconPackUiState.installedPacks,
            activePackPackage = uiState.activeIconPack,
            adaptiveShape = uiState.adaptiveIconShape,
            sheetState = iconSheetState,
            onDismissRequest = { showIconPicker = false },
            onSelectPack = { pack ->
                viewModel.setActiveIconPack(pack)
                iconPackViewModel.selectIconPack(pack)
            },
            onSelectShape = { shape ->
                viewModel.setAdaptiveIconShape(shape)
                iconPackViewModel.setAdaptiveIconShape(shape)
            }
        )
    }

    // App Update Dialog
    val updateInfo = uiState.updateInfo
    if (updateInfo != null && updateInfo.isUpdateAvailable) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdateDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Update Available")
                }
            },
            text = {
                Column {
                    Text(
                        text = updateInfo.releaseName.ifBlank { updateInfo.latestVersionTag },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tag: ${updateInfo.latestVersionTag}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (updateInfo.releaseNotes.isNotBlank()) {
                        Text(
                            text = "Release Notes:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = updateInfo.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 8,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val downloadUrl = updateInfo.apkDownloadUrl ?: updateInfo.htmlUrl
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        viewModel.dismissUpdateDialog()
                    }
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (updateInfo.apkDownloadUrl?.endsWith(".apk", ignoreCase = true) == true) "Download APK" else "Open Release")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                    Text("Later")
                }
            }
        )
    }
        }
    }
}

@Composable
private fun SettingsDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        thickness = 0.5.dp
    )
}

@Composable
private fun SettingsSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    steps: Int = 0,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    var localValue by remember(value) { mutableFloatStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        // ponytail: updates local state during drag and commits to DataStore on release (onValueChangeFinished) to eliminate 60fps disk writes and jank; ceiling: commit on release; upgrade path: debounced live preview.
        Slider(
            value = if (isDragging) localValue else value,
            onValueChange = {
                isDragging = true
                localValue = it
            },
            onValueChangeFinished = {
                isDragging = false
                onValueChange(localValue)
            },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ponytail: Native M3 SingleChoiceSegmentedButtonRow for compact fixed options; ceiling: up to 5 short items; upgrade path: scrollable row or bottom sheet for larger sets.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SettingsSegmentedRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, (key, label) ->
            SegmentedButton(
                selected = selected == key,
                onClick = { onSelect(key) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = {}, // ponytail: omit checkmark to maximize available text width and prevent line-wrapping on narrow screens
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

// ponytail: Natural-width choice chip for multi-word or variable options; ceiling: simple pill chips; upgrade path: grid cards with live previews.
@Composable
private fun SettingsChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        modifier = modifier
    )
}

@Composable
private fun ThemeOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsChoiceChip(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
