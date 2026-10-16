package com.urunkarpm.drawer.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.urunkarpm.drawer.feature.iconpacks.IconPackViewModel
import com.urunkarpm.drawer.feature.iconpacks.component.IconPackPickerSheet
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
    iconPackViewModel: IconPackViewModel = hiltViewModel()
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
                    if (uiState.wallpaperBlur) append(" • Blur (${uiState.wallpaperBlurRadius.toInt()}px)")
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
                }
            }

            DRAWER_TYPOGRAPHY -> {
                val fontName = when (uiState.headerFontFamily) {
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
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            label = "Helvetica",
                            selected = uiState.headerFontFamily == "HELVETICA",
                            onClick = { viewModel.setHeaderFontFamily("HELVETICA") }
                        )
                        ThemeOptionChip(
                            label = "Roboto",
                            selected = uiState.headerFontFamily == "ROBOTO",
                            onClick = { viewModel.setHeaderFontFamily("ROBOTO") }
                        )
                        ThemeOptionChip(
                            label = "Times New Roman",
                            selected = uiState.headerFontFamily == "TIMES_NEW_ROMAN",
                            onClick = { viewModel.setHeaderFontFamily("TIMES_NEW_ROMAN") }
                        )
                        ThemeOptionChip(
                            label = "Arial",
                            selected = uiState.headerFontFamily == "ARIAL",
                            onClick = { viewModel.setHeaderFontFamily("ARIAL") }
                        )
                        ThemeOptionChip(
                            label = "Texas Troupe",
                            selected = uiState.headerFontFamily == "TEXAS_TROUPE",
                            onClick = { viewModel.setHeaderFontFamily("TEXAS_TROUPE") }
                        )
                        ThemeOptionChip(
                            label = "System Sans",
                            selected = uiState.headerFontFamily == "SYSTEM",
                            onClick = { viewModel.setHeaderFontFamily("SYSTEM") }
                        )
                        ThemeOptionChip(
                            label = "Serif",
                            selected = uiState.headerFontFamily == "SERIF",
                            onClick = { viewModel.setHeaderFontFamily("SERIF") }
                        )
                        ThemeOptionChip(
                            label = "Monospace",
                            selected = uiState.headerFontFamily == "MONOSPACE",
                            onClick = { viewModel.setHeaderFontFamily("MONOSPACE") }
                        )
                        ThemeOptionChip(
                            label = "Cursive",
                            selected = uiState.headerFontFamily == "CURSIVE",
                            onClick = { viewModel.setHeaderFontFamily("CURSIVE") }
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
                            Text(
                                text = "12:45 PM • Social • Utilities",
                                fontFamily = headerFontFamilyFromName(uiState.headerFontFamily),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val fontBio = when (uiState.headerFontFamily) {
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
                SettingsDrawerCard(
                    title = "About Drawer",
                    subtitle = "Version 1.0.0 (API 37) • Offline & Private",
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
                        text = "Version 1.0.0 (API 37)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A personalisation-first launcher built with 100% Jetpack Compose and offline privacy. Zero tracking, zero telemetry.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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
                                val currentVersion = runCatching {
                                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                    pInfo.versionName ?: "1.0.0"
                                }.getOrDefault("1.0.0")
                                viewModel.checkForUpdates(currentVersion)
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
