package com.urunkarpm.drawer.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily
import com.urunkarpm.drawer.core.designsystem.theme.headerFontFamilyFromName
import com.urunkarpm.drawer.core.designsystem.theme.labelScaleFromName
import com.urunkarpm.drawer.core.needle.model.ModelStatus
import com.urunkarpm.drawer.feature.iconpacks.IconPackViewModel
import com.urunkarpm.drawer.feature.iconpacks.component.IconPackPickerSheet
import com.urunkarpm.drawer.feature.settings.component.NeedleSettingsCard
import com.urunkarpm.drawer.feature.settings.component.SettingsDrawerCard
import com.urunkarpm.drawer.feature.settings.component.SettingsGlanceHeader
import kotlinx.coroutines.launch

private const val DRAWER_APPEARANCE = "APPEARANCE"
private const val DRAWER_TYPOGRAPHY = "TYPOGRAPHY"
private const val DRAWER_DOCK = "DOCK"
private const val DRAWER_CATEGORIES = "CATEGORIES"
private const val DRAWER_GLANCE = "GLANCE"
private const val DRAWER_NOTIFICATIONS = "NOTIFICATIONS"
private const val DRAWER_ICON_PACKS = "ICON_PACKS"
private const val DRAWER_BACKUP = "BACKUP"
private const val DRAWER_ABOUT = "ABOUT"

private val ALL_DRAWER_KEYS = listOf(
    DRAWER_APPEARANCE,
    DRAWER_TYPOGRAPHY,
    DRAWER_DOCK,
    DRAWER_CATEGORIES,
    DRAWER_GLANCE,
    DRAWER_NOTIFICATIONS,
    DRAWER_ICON_PACKS,
    DRAWER_BACKUP,
    DRAWER_ABOUT
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
    val needleModelStatus by viewModel.needleModelStatus.collectAsStateWithLifecycle(initialValue = ModelStatus.NotDownloaded)
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
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.bufferedWriter().use { writer ->
                            writer.write(jsonContent)
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
                    val jsonContent = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { reader ->
                            reader.readText()
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
                val appearanceSummary = buildString {
                    append(themeLabel)
                    if (uiState.dynamicWallpaperEnabled) {
                        append(" • Pantone Gradient (${uiState.wallpaperGradientSpeedSeconds}s)")
                    } else if (uiState.wallpaperBlur) {
                        append(" • Blur (${uiState.wallpaperBlurRadius.toInt()}px)")
                    }
                    if (uiState.hideStatusBar) append(" • Fullscreen")
                }

                SettingsDrawerCard(
                    title = "Appearance & Display",
                    subtitle = appearanceSummary,
                    icon = Icons.Outlined.ColorLens,
                    iconTint = Color(0xFF6366F1),
                    isExpanded = DRAWER_APPEARANCE in expandedDrawers,
                    onToggle = { toggleDrawer(DRAWER_APPEARANCE) },
                    isCompact = isCompact,
                    cornerRadius = cornerRadius
                ) {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThemeOptionChip(
                            label = "System",
                            selected = uiState.themeMode == "SYSTEM",
                            onClick = { viewModel.setThemeMode("SYSTEM") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "Light",
                            selected = uiState.themeMode == "LIGHT",
                            onClick = { viewModel.setThemeMode("LIGHT") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "Dark",
                            selected = uiState.themeMode == "DARK",
                            onClick = { viewModel.setThemeMode("DARK") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "AMOLED",
                            selected = uiState.themeMode == "AMOLED",
                            onClick = { viewModel.setThemeMode("AMOLED") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "App Drawer Theme",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Clean, solid theme for the full app drawer (3 modes)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            label = "White",
                            selected = uiState.drawerThemeMode == "WHITE" || uiState.drawerThemeMode == "LIGHT",
                            onClick = { viewModel.setDrawerThemeMode("WHITE") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "Dark",
                            selected = uiState.drawerThemeMode == "DARK",
                            onClick = { viewModel.setDrawerThemeMode("DARK") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "System",
                            selected = uiState.drawerThemeMode == "SYSTEM",
                            onClick = { viewModel.setDrawerThemeMode("SYSTEM") },
                            modifier = Modifier.weight(1f)
                        )
                    }

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

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Hide Status Bar",
                        subtitle = "Maximize screen real estate by hiding the system bar",
                        checked = uiState.hideStatusBar,
                        onCheckedChange = { viewModel.setHideStatusBar(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Frequency of smooth gradient color morphing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(2 to "2s", 3 to "3s", 5 to "5s", 8 to "8s", 12 to "12s").forEach { (speed, label) ->
                                ThemeOptionChip(
                                    label = label,
                                    selected = uiState.wallpaperGradientSpeedSeconds == speed,
                                    onClick = { viewModel.setWallpaperGradientSpeedSeconds(speed) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        SettingsDivider(modifier = Modifier.padding(vertical = 6.dp))

                        Text(
                            text = "Pantone Harmonic Palette",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Harmonized authentic Pantone tones across the spectrum",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ThemeOptionChip(
                                    label = "Full Spectrum",
                                    selected = uiState.wallpaperGradientPalette == "FULL_SPECTRUM",
                                    onClick = { viewModel.setWallpaperGradientPalette("FULL_SPECTRUM") },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionChip(
                                    label = "Color of Year",
                                    selected = uiState.wallpaperGradientPalette == "PANTONE_COTY",
                                    onClick = { viewModel.setWallpaperGradientPalette("PANTONE_COTY") },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionChip(
                                    label = "Aurora",
                                    selected = uiState.wallpaperGradientPalette == "AURORA_BOREALIS",
                                    onClick = { viewModel.setWallpaperGradientPalette("AURORA_BOREALIS") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ThemeOptionChip(
                                    label = "Sunset",
                                    selected = uiState.wallpaperGradientPalette == "SUNSET_CHROMA",
                                    onClick = { viewModel.setWallpaperGradientPalette("SUNSET_CHROMA") },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionChip(
                                    label = "Twilight",
                                    selected = uiState.wallpaperGradientPalette == "DEEP_TWILIGHT",
                                    onClick = { viewModel.setWallpaperGradientPalette("DEEP_TWILIGHT") },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionChip(
                                    label = "Pastel",
                                    selected = uiState.wallpaperGradientPalette == "PASTEL_DREAM",
                                    onClick = { viewModel.setWallpaperGradientPalette("PASTEL_DREAM") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        SettingsDivider(modifier = Modifier.padding(vertical = 6.dp))

                        Text(
                            text = "Gradient Flow Direction",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "DIAGONAL" to "Diagonal",
                                "VERTICAL" to "Vertical",
                                "RADIAL" to "Radial"
                            ).forEach { (dir, label) ->
                                ThemeOptionChip(
                                    label = label,
                                    selected = uiState.wallpaperGradientAngle == dir,
                                    onClick = { viewModel.setWallpaperGradientAngle(dir) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

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

                        SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))

                        SettingsSwitchRow(
                            title = "Mesmerizing Dock Prismatic Glow",
                            subtitle = "Iridescent glass sheen catching light from the gradient wallpaper",
                            checked = uiState.dockMesmerizingGlow,
                            onCheckedChange = { viewModel.setDockMesmerizingGlow(it) }
                        )
                    }
                }
            }

            DRAWER_TYPOGRAPHY -> {
                val fontName = when (uiState.headerFontFamily) {
                    "GOOGLE_SANS", "GOOGLE SANS", "PRODUCT_SANS" -> "Google Sans"
                    "HELVETICA" -> "Helvetica"
                    "ROBOTO" -> "Roboto"
                    "TIMES_NEW_ROMAN", "TIMES" -> "Times New Roman"
                    "ARIAL" -> "Arial"
                    "TEXAS_TROUPE" -> "Texas Troupe"
                    "SYSTEM" -> "System Sans"
                    "SERIF" -> "Serif"
                    "MONOSPACE" -> "Monospace"
                    "CURSIVE" -> "Cursive"
                    else -> uiState.headerFontFamily
                }
                val labelSizeName = when (uiState.appLabelScale) {
                    "SMALL" -> "Small"
                    "LARGE" -> "Large"
                    else -> "Default"
                }
                val typographySummary = "$fontName • $labelSizeName Labels"

                SettingsDrawerCard(
                    title = "Typography & Font Packs",
                    subtitle = typographySummary,
                    icon = Icons.Outlined.TextFields,
                    iconTint = Color(0xFF8B5CF6),
                    isExpanded = DRAWER_TYPOGRAPHY in expandedDrawers,
                    onToggle = { toggleDrawer(DRAWER_TYPOGRAPHY) },
                    isCompact = isCompact,
                    cornerRadius = cornerRadius
                ) {
                    Text(
                        text = "Header & Clock Font",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        ?: currentFontKey

                    // ponytail: DropdownMenu showing fonts rendered directly in their actual custom typeface; ceiling: scrollable dropdown popup; upgrade path: modal bottom sheet with search.
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
                                        fontFamily = currentFontFamily,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Sample: 12:45 • Social • Notes",
                                        fontFamily = currentFontFamily,
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
                            val fontBio = when (uiState.headerFontFamily) {
                                "GOOGLE_SANS", "GOOGLE SANS" -> "Geometric modern sans-serif designed for Google Pixel & Material Design"
                                "SPOOKYY" -> "Quirky playful hand-cut Halloween-themed display typeface"
                                "DS_DIGITAL", "DS-DIGITAL" -> "Authentic 7-segment retro digital LCD display font"
                                "EVIL_EMPIRE" -> "Bold angular stencil display font with high-impact sci-fi aesthetic"
                                "HYPIK" -> "Futuristic cyber-geometric cyberpunk display font"
                                "HELVETICA" -> "1957 • Clean, neutral, neo-grotesque sans-serif (BMW, NYC Subway)"
                                "ROBOTO" -> "2011 • Geometric-leaning sans-serif designed by Google for Android"
                                "TIMES_NEW_ROMAN", "TIMES" -> "1931 • Classic, high-legibility serif built for The Times"
                                "ARIAL" -> "1982 • Ubiquitous, widely installed modern digital sans-serif"
                                "TEXAS_TROUPE" -> "Display decorative western header font"
                                "SYSTEM" -> "Android native system sans-serif"
                                "SERIF" -> "Standard book serif"
                                "MONOSPACE" -> "Fixed-width terminal typeface"
                                "CURSIVE" -> "Fluid handwriting cursive script"
                                else -> ""
                            }
                            if (fontBio.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = fontBio,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "App Label Size",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Scale font size for app titles across home, groups, and dock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            label = "Small",
                            selected = uiState.appLabelScale == "SMALL",
                            onClick = { viewModel.setAppLabelScale("SMALL") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "Default",
                            selected = uiState.appLabelScale == "NORMAL",
                            onClick = { viewModel.setAppLabelScale("NORMAL") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "Large",
                            selected = uiState.appLabelScale == "LARGE",
                            onClick = { viewModel.setAppLabelScale("LARGE") },
                            modifier = Modifier.weight(1f)
                        )
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
                            val labelScale = labelScaleFromName(uiState.appLabelScale)
                            Text(
                                text = "Sample App",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize * labelScale
                                ),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            DRAWER_DOCK -> {
                val dockStyleName = when (uiState.surfaceThemeStyle) {
                    "LIQUID_GLASS" -> "Liquid Glass"
                    "BLUR" -> "Frosted Blur"
                    "SOLID" -> "Solid"
                    "TRANSPARENT" -> "Clear"
                    else -> uiState.surfaceThemeStyle
                }
                val dockSummary = "$dockStyleName • ${uiState.dockIconSize.toInt()}dp${if (uiState.dockShowLabels) " • Labels" else ""}"

                SettingsDrawerCard(
                    title = "Dock Theming & Style",
                    subtitle = dockSummary,
                    icon = Icons.Default.ViewCarousel,
                    iconTint = Color(0xFF0EA5E9),
                    isExpanded = DRAWER_DOCK in expandedDrawers,
                    onToggle = { toggleDrawer(DRAWER_DOCK) },
                    isCompact = isCompact,
                    cornerRadius = cornerRadius
                ) {
                    Text(
                        text = "Dock Style",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Aesthetic appearance for the bottom dock bar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ThemeOptionChip(
                            label = "Liquid Glass",
                            selected = uiState.surfaceThemeStyle == "LIQUID_GLASS",
                            onClick = { viewModel.setSurfaceThemeStyle("LIQUID_GLASS") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "Frosted Blur",
                            selected = uiState.surfaceThemeStyle == "BLUR",
                            onClick = { viewModel.setSurfaceThemeStyle("BLUR") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "Solid",
                            selected = uiState.surfaceThemeStyle == "SOLID",
                            onClick = { viewModel.setSurfaceThemeStyle("SOLID") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "Clear",
                            selected = uiState.surfaceThemeStyle == "TRANSPARENT",
                            onClick = { viewModel.setSurfaceThemeStyle("TRANSPARENT") },
                            modifier = Modifier.weight(1f)
                        )
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val previewShape = RoundedCornerShape(uiState.surfaceCornerRadius.dp)
                                val previewIcons = listOf(
                                    Icons.Default.Phone to Color(0xFF10B981),
                                    Icons.Default.Email to Color(0xFF3B82F6),
                                    Icons.Default.CameraAlt to Color(0xFFEC4899),
                                    Icons.Default.Language to Color(0xFFF59E0B)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(224.dp)
                                        .height(52.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    com.urunkarpm.drawer.core.designsystem.component.DockSurfaceBackground(
                                        backgroundStyle = uiState.surfaceThemeStyle,
                                        shape = previewShape,
                                        surfaceOpacity = uiState.surfaceOpacity,
                                        surfaceBlurRadius = uiState.surfaceBlurRadius,
                                        surfaceStrokeOpacity = uiState.surfaceStrokeOpacity,
                                        modifier = Modifier.matchParentSize()
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        previewIcons.forEach { (icon, tint) ->
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(tint.copy(alpha = 0.20f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = tint,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            val styleDescription = when (uiState.surfaceThemeStyle) {
                                "LIQUID_GLASS" -> "Liquid Glass: Multi-layer luminous glass gradient with top specular shine"
                                "BLUR" -> "Frosted Blur: Translucent acrylic diffusion with soft frosted borders"
                                "SOLID" -> "Solid: 100% opaque Material 3 floating shelf with elevation"
                                "TRANSPARENT" -> "Clear: Minimal floating icons with zero container chrome"
                                else -> ""
                            }
                            if (styleDescription.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = styleDescription,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    if (uiState.surfaceThemeStyle == "LIQUID_GLASS" || uiState.surfaceThemeStyle == "BLUR") {
                        SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))
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

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Dock Layout",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsSliderRow(
                        title = "Dock Icon Size",
                        subtitle = "Scaled proportionally across all dock slots",
                        value = uiState.dockIconSize,
                        onValueChange = { viewModel.setDockIconSize(it) },
                        valueRange = 40f..72f,
                        steps = 7,
                        valueLabel = "${uiState.dockIconSize.toInt()} dp"
                    )
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
                }
            }

            DRAWER_CATEGORIES -> {
                val layoutMode = if (uiState.twoDrawersSideBySide) "2 Columns" else "Single Column"
                val categoriesSummary = "$layoutMode${if (uiState.enableWidgetsPage) " • Widget Feed" else ""}${if (uiState.lockLayout) " • Locked" else ""}"

                SettingsDrawerCard(
                    title = "App Categories & Drawers",
                    subtitle = categoriesSummary,
                    icon = Icons.Default.FormatPaint,
                    iconTint = Color(0xFFF59E0B),
                    isExpanded = DRAWER_CATEGORIES in expandedDrawers,
                    onToggle = { toggleDrawer(DRAWER_CATEGORIES) },
                    isCompact = isCompact,
                    cornerRadius = cornerRadius
                ) {
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
                        title = "Left-Hand Widget Page",
                        subtitle = "Enable a dedicated full-page widget feed by swiping right from Home",
                        checked = uiState.enableWidgetsPage,
                        onCheckedChange = { viewModel.setEnableWidgetsPage(it) }
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
                }
            }

            DRAWER_GLANCE -> {
                val clockFormat = if (uiState.is24Hour) "24h Clock" else "12h Clock"
                val glanceSummary = buildString {
                    append(clockFormat)
                    if (uiState.showWeather) {
                        append(" • Weather (${if (uiState.weatherUnit == "FAHRENHEIT") "°F" else "°C"})")
                    }
                    if (uiState.enableCameraMirror) append(" • Camera Mirror")
                }

                SettingsDrawerCard(
                    title = "At A Glance",
                    subtitle = glanceSummary,
                    icon = Icons.Default.Schedule,
                    iconTint = Color(0xFF10B981),
                    isExpanded = DRAWER_GLANCE in expandedDrawers,
                    onToggle = { toggleDrawer(DRAWER_GLANCE) },
                    isCompact = isCompact,
                    cornerRadius = cornerRadius
                ) {
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeOptionChip(
                                label = "Celsius (°C)",
                                selected = uiState.weatherUnit == "CELSIUS",
                                onClick = { viewModel.setWeatherUnit("CELSIUS") },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeOptionChip(
                                label = "Fahrenheit (°F)",
                                selected = uiState.weatherUnit == "FAHRENHEIT",
                                onClick = { viewModel.setWeatherUnit("FAHRENHEIT") },
                                modifier = Modifier.weight(1f)
                            )
                        }

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
                }
            }

            DRAWER_NOTIFICATIONS -> {
                val notifSummary = if (uiState.notificationsPrivacyMode) "Privacy Mode Active" else "Standard Alerts"

                SettingsDrawerCard(
                    title = "Notifications",
                    subtitle = notifSummary,
                    icon = Icons.Default.Notifications,
                    iconTint = Color(0xFFEF4444),
                    isExpanded = DRAWER_NOTIFICATIONS in expandedDrawers,
                    onToggle = { toggleDrawer(DRAWER_NOTIFICATIONS) },
                    isCompact = isCompact,
                    cornerRadius = cornerRadius
                ) {
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

                    // Stealth theme
                    // ponytail: flat toggle + AnimatedVisibility sub-panel; no nested nav; ceiling: single stealth level, no per-app masking.
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeOptionChip(
                                    label = "Low",
                                    selected = uiState.stealthContrastLevel == "LOW",
                                    onClick = { viewModel.setStealthContrastLevel("LOW") },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionChip(
                                    label = "Lower",
                                    selected = uiState.stealthContrastLevel == "LOWER",
                                    onClick = { viewModel.setStealthContrastLevel("LOWER") },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionChip(
                                    label = "Lowest",
                                    selected = uiState.stealthContrastLevel == "LOWEST",
                                    onClick = { viewModel.setStealthContrastLevel("LOWEST") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                            SettingsSwitchRow(
                                title = "Limit Brightness",
                                subtitle = "Dim the screen while the notification drawer is open",
                                checked = uiState.stealthLimitBrightness,
                                onCheckedChange = { viewModel.setStealthLimitBrightness(it) }
                            )
                        }
                    }
                }
            }

            DRAWER_ICON_PACKS -> {
                val packLabel = iconPackUiState.installedPacks.firstOrNull { it.packageName == uiState.activeIconPack }?.name
                    ?: if (uiState.activeIconPack == null) "System Default" else uiState.activeIconPack
                val shapeName = uiState.adaptiveIconShape.lowercase().replaceFirstChar { it.uppercase() }
                val iconPackSummary = "$packLabel • $shapeName"

                SettingsDrawerCard(
                    title = "Icon Packs & Shapes",
                    subtitle = iconPackSummary,
                    icon = Icons.Default.Palette,
                    iconTint = Color(0xFFEC4899),
                    isExpanded = DRAWER_ICON_PACKS in expandedDrawers,
                    onToggle = { toggleDrawer(DRAWER_ICON_PACKS) },
                    isCompact = isCompact,
                    cornerRadius = cornerRadius
                ) {
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

            DRAWER_BACKUP -> {
                SettingsDrawerCard(
                    title = "Backup & Restore",
                    subtitle = "JSON Configuration Export / Import",
                    icon = Icons.Default.FileUpload,
                    iconTint = Color(0xFF3B82F6),
                    isExpanded = DRAWER_BACKUP in expandedDrawers,
                    onToggle = { toggleDrawer(DRAWER_BACKUP) },
                    isCompact = isCompact,
                    cornerRadius = cornerRadius
                ) {
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
                                importBackupLauncher.launch(arrayOf("application/json", "*/*"))
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
                }
            }

            DRAWER_ABOUT -> {
                // ponytail: dynamically read package versionName at runtime rather than hardcoding string; ceiling: fallback to 1.2.4 if packageInfo fails; upgrade path: inject BuildConfig via DI.
                val installedVersion = remember(context) {
                    runCatching {
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        pInfo.versionName ?: "1.2.4"
                    }.getOrDefault("1.2.4")
                }

                SettingsDrawerCard(
                    title = "About Drawer",
                    subtitle = "Version $installedVersion • Offline & Private",
                    icon = Icons.Outlined.Info,
                    iconTint = Color(0xFF64748B),
                    isExpanded = DRAWER_ABOUT in expandedDrawers,
                    onToggle = { toggleDrawer(DRAWER_ABOUT) },
                    isCompact = isCompact,
                    cornerRadius = cornerRadius
                ) {
                    Text(
                        text = "Drawer Launcher",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version $installedVersion (API ${android.os.Build.VERSION.SDK_INT})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A personalisation-first launcher built with 100% Jetpack Compose and offline privacy. Zero tracking, zero telemetry.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Spacer(modifier = Modifier.height(8.dp))

                    // About Creator Section
                    Text(
                        text = "Developer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Designed and crafted by Prasenjeet Urunkar with a focus on fluid gesture navigation, deep aesthetic personalisation, and offline privacy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

                    Spacer(modifier = Modifier.height(14.dp))
                    SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Spacer(modifier = Modifier.height(8.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Spacer(modifier = Modifier.height(8.dp))

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontFamily = LocalHeaderFontFamily.current,
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
                }
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

                // On-Device AI Controls
                item(key = "NEEDLE_AI_SETTINGS") {
                    NeedleSettingsCard(
                        status = needleModelStatus,
                        strictness = uiState.aiCategorizationStrictness,
                        onStrictnessChange = { viewModel.setAiCategorizationStrictness(it) },
                        categorizeAllApps = uiState.aiCategorizeAllApps,
                        onCategorizeAllAppsChange = { viewModel.setAiCategorizeAllApps(it) },
                        forceWorkProfile = uiState.aiForceWorkProfile,
                        onForceWorkProfileChange = { viewModel.setAiForceWorkProfile(it) },
                        autoCategorizeNewApps = uiState.autoCategorizeNewApps,
                        onAutoCategorizeNewAppsChange = { viewModel.setAutoCategorizeNewApps(it) },
                        onDownload = { viewModel.downloadNeedleModel() },
                        onDelete = { viewModel.deleteNeedleModel() }
                    )
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
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ThemeOptionChip(
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
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
