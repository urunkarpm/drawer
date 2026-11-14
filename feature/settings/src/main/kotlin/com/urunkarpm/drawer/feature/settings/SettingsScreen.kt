package com.urunkarpm.drawer.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.designsystem.theme.CinematicPalettes
import com.urunkarpm.drawer.core.designsystem.theme.DrawerTheme
import com.urunkarpm.drawer.core.designsystem.theme.GoogleSansFontFamily
import com.urunkarpm.drawer.core.designsystem.theme.LocalHeaderFontFamily
import com.urunkarpm.drawer.feature.iconpacks.IconPackViewModel
import com.urunkarpm.drawer.feature.iconpacks.component.IconPackPickerSheet
import com.urunkarpm.drawer.feature.settings.component.SettingsDivider
import com.urunkarpm.drawer.feature.settings.component.SettingsNavTileRow
import com.urunkarpm.drawer.feature.settings.component.SettingsSectionHeader
import com.urunkarpm.drawer.feature.settings.subscreen.AboutSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.DockSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.GlanceSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.HomeDrawersSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.PrivacySubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.SettingsSection
import com.urunkarpm.drawer.feature.settings.subscreen.SurfacesSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.ThemesSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.TypographyIconsSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.WallpaperSubScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
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

    var currentSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    var showCityDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Hardware and system back gesture handling
    BackHandler(enabled = currentSection != null) {
        currentSection = null
    }

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

    val isSystemDark = isSystemInDarkTheme()
    val isSettingsDark = when (uiState.themeMode) {
        "LIGHT" -> false
        "DARK", "AMOLED" -> true
        else -> isSystemDark
    }
    val isSettingsAmoled = uiState.themeMode == "AMOLED"

    DrawerTheme(
        darkTheme = isSettingsDark,
        isAmoled = isSettingsAmoled,
        dynamicColor = uiState.dynamicColor,
        accentColor = uiState.accentColor
    ) {
        CompositionLocalProvider(
            LocalHeaderFontFamily provides GoogleSansFontFamily
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.surface,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = currentSection?.title ?: "Settings",
                                fontFamily = GoogleSansFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (currentSection != null) {
                                    currentSection = null
                                } else {
                                    onNavigateBack()
                                }
                            }) {
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
                    AnimatedContent(
                        targetState = currentSection,
                        transitionSpec = {
                            if (targetState != null) {
                                (slideInHorizontally(animationSpec = tween(220)) { width -> width / 3 } + fadeIn(animationSpec = tween(220)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(200)) { width -> -width / 3 } + fadeOut(animationSpec = tween(200)))
                            } else {
                                (slideInHorizontally(animationSpec = tween(220)) { width -> -width / 3 } + fadeIn(animationSpec = tween(220)))
                                    .togetherWith(slideOutHorizontally(animationSpec = tween(200)) { width -> width / 3 } + fadeOut(animationSpec = tween(200)))
                            }
                        },
                        label = "SettingsSectionTransition"
                    ) { targetSection ->
                        if (targetSection == null) {
                            MainSettingsHub(
                                uiState = uiState,
                                onSelectSection = { currentSection = it },
                                horizontalPadding = horizontalPadding
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 840.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                when (targetSection) {
                                    SettingsSection.THEMES -> ThemesSubScreen(uiState, viewModel)
                                    SettingsSection.WALLPAPER -> WallpaperSubScreen(uiState, viewModel)
                                    SettingsSection.SURFACES -> SurfacesSubScreen(uiState, viewModel)
                                    SettingsSection.TYPOGRAPHY_ICONS -> TypographyIconsSubScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        onShowIconPicker = { showIconPicker = true }
                                    )
                                    SettingsSection.HOME_DRAWERS -> HomeDrawersSubScreen(uiState, viewModel)
                                    SettingsSection.DOCK -> DockSubScreen(uiState, viewModel)
                                    SettingsSection.GLANCE -> GlanceSubScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        onShowCityDialog = { showCityDialog = true }
                                    )
                                    SettingsSection.PRIVACY -> PrivacySubScreen(uiState, viewModel)
                                    SettingsSection.ABOUT -> AboutSubScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        onExportBackup = {
                                            val fileName = "drawer_backup_${System.currentTimeMillis()}.json"
                                            exportBackupLauncher.launch(fileName)
                                        },
                                        onImportBackup = {
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
                                        onOpenWalkthrough = onOpenWalkthrough
                                    )
                                }
                            }
                        }
                    }
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
private fun MainSettingsHub(
    uiState: SettingsUiState,
    onSelectSection: (SettingsSection) -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedVersion = remember(context) {
        runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.2.4"
        }.getOrDefault("1.2.4")
    }

    val selectedPalette = CinematicPalettes.find(uiState.cinematicPalette)
    val themeLabel = when (uiState.themeMode) {
        "SYSTEM" -> "System"
        "LIGHT" -> "Light"
        "DARK" -> "Dark"
        "AMOLED" -> "AMOLED"
        else -> uiState.themeMode
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(max = 840.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Personalisation & Look
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingsSectionHeader(title = "Personalisation & Look")
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column {
                    SettingsNavTileRow(
                        title = SettingsSection.THEMES.title,
                        subtitle = "$themeLabel Theme • ${uiState.surfaceThemeStyle.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}",
                        icon = Icons.Default.Palette,
                        onClick = { onSelectSection(SettingsSection.THEMES) }
                    )

                    SettingsDivider(modifier = Modifier.padding(horizontal = 14.dp))

                    SettingsNavTileRow(
                        title = SettingsSection.WALLPAPER.title,
                        subtitle = "${selectedPalette.name} • ${if (uiState.dynamicWallpaperEnabled) "Pantone active" else if (uiState.wallpaperBlur) "Blur active" else "Default"}",
                        icon = Icons.Default.FormatPaint,
                        onClick = { onSelectSection(SettingsSection.WALLPAPER) }
                    )

                    SettingsDivider(modifier = Modifier.padding(horizontal = 14.dp))

                    SettingsNavTileRow(
                        title = SettingsSection.SURFACES.title,
                        subtitle = "${uiState.surfaceThemeStyle.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }} • ${(uiState.surfaceOpacity * 100).toInt()}% opacity • ${uiState.surfaceCornerRadius.toInt()}dp radius",
                        icon = Icons.Outlined.ColorLens,
                        onClick = { onSelectSection(SettingsSection.SURFACES) }
                    )

                    SettingsDivider(modifier = Modifier.padding(horizontal = 14.dp))

                    SettingsNavTileRow(
                        title = SettingsSection.TYPOGRAPHY_ICONS.title,
                        subtitle = "${uiState.headerFontFamily.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }} • ${uiState.appLabelScale.lowercase().replaceFirstChar { it.uppercase() }} Labels",
                        icon = Icons.Outlined.TextFields,
                        onClick = { onSelectSection(SettingsSection.TYPOGRAPHY_ICONS) }
                    )
                }
            }
        }

        // Section 2: Workspace & Layout
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingsSectionHeader(title = "Workspace & Layout")
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column {
                    SettingsNavTileRow(
                        title = SettingsSection.HOME_DRAWERS.title,
                        subtitle = "${if (uiState.categoryAlignment == "BOTTOM") "Bottom thumb-zone" else "Top stack"} • ${if (uiState.twoDrawersSideBySide) "Dual column" else "Single column"}",
                        icon = Icons.Default.ViewCarousel,
                        onClick = { onSelectSection(SettingsSection.HOME_DRAWERS) }
                    )

                    SettingsDivider(modifier = Modifier.padding(horizontal = 14.dp))

                    SettingsNavTileRow(
                        title = SettingsSection.DOCK.title,
                        subtitle = "${uiState.dockIconSize.toInt()}dp icons • ${if (uiState.dockShowLabels) "Labels on" else "Labels off"}",
                        icon = Icons.Default.Phone,
                        onClick = { onSelectSection(SettingsSection.DOCK) }
                    )

                    SettingsDivider(modifier = Modifier.padding(horizontal = 14.dp))

                    SettingsNavTileRow(
                        title = SettingsSection.GLANCE.title,
                        subtitle = "${if (uiState.is24Hour) "24h clock" else "12h clock"} • ${if (uiState.showWeather) "Weather on" else "Weather off"}",
                        icon = Icons.Default.Schedule,
                        onClick = { onSelectSection(SettingsSection.GLANCE) }
                    )
                }
            }
        }

        // Section 3: Privacy & System
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingsSectionHeader(title = "Privacy & System")
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column {
                    SettingsNavTileRow(
                        title = SettingsSection.PRIVACY.title,
                        subtitle = "${if (uiState.appVaultEnabled) "App Vault on" else "App Vault off"} • ${if (uiState.notificationsPrivacyMode) "Private" else "Standard"}",
                        icon = Icons.Default.Security,
                        onClick = { onSelectSection(SettingsSection.PRIVACY) }
                    )

                    SettingsDivider(modifier = Modifier.padding(horizontal = 14.dp))

                    SettingsNavTileRow(
                        title = SettingsSection.ABOUT.title,
                        subtitle = "Version $installedVersion • Offline & Private",
                        icon = Icons.Outlined.Info,
                        onClick = { onSelectSection(SettingsSection.ABOUT) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
