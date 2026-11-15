package com.urunkarpm.drawer.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Explore
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.designsystem.component.LocalMonochromeIcons
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
import com.urunkarpm.drawer.feature.settings.subscreen.AppearanceSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.DockSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.DrawersSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.FeaturesSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.GlanceSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.HomeDrawersSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.PrivacySubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.SettingsSection
import com.urunkarpm.drawer.feature.settings.subscreen.SurfacesSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.SystemSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.ThemesSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.TypographyIconsSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.WallpaperSubScreen
import com.urunkarpm.drawer.feature.settings.subscreen.WorkspaceSubScreen
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

    var sectionNames by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val sectionBackStack = remember(sectionNames) {
        sectionNames.mapNotNull { name ->
            when (name) {
                "THEMES", "WALLPAPER", "SURFACES", "TYPOGRAPHY_ICONS" -> SettingsSection.APPEARANCE
                "HOME_DRAWERS" -> SettingsSection.DRAWERS
                "DOCK", "GLANCE" -> SettingsSection.WORKSPACE
                "PRIVACY", "ABOUT" -> SettingsSection.SYSTEM
                else -> runCatching { SettingsSection.valueOf(name) }.getOrNull()
            }
        }
    }
    val currentSection = sectionBackStack.lastOrNull()

    var lastSelectedFeatureIndex by rememberSaveable { androidx.compose.runtime.mutableIntStateOf(0) }
    var targetHighlightKey by rememberSaveable { mutableStateOf<String?>(null) }

    val navigateBack: () -> Unit = {
        if (sectionNames.isNotEmpty()) {
            sectionNames = sectionNames.dropLast(1)
            targetHighlightKey = null
        } else {
            onNavigateBack()
        }
    }

    val navigateToSection: (SettingsSection) -> Unit = { section ->
        sectionNames = sectionNames + section.name
    }

    var showCityDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val predictiveBackProgress = remember { Animatable(0f) }
    var predictiveBackSwipeEdge by remember { androidx.compose.runtime.mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    // ponytail: Android 14+ Predictive Back gesture - scales, shifts, and rounds container corners during swipe-to-back, with smooth spring-cancel on release; ceiling: 1D scale/translation; upgrade path: full Material3 PredictiveBackHandler transitions.
    PredictiveBackHandler(enabled = true) { progressFlow ->
        try {
            progressFlow.collect { backEvent ->
                predictiveBackSwipeEdge = backEvent.swipeEdge
                predictiveBackProgress.snapTo(backEvent.progress)
            }
            navigateBack()
            predictiveBackProgress.animateTo(0f, animationSpec = tween(120))
        } catch (e: kotlinx.coroutines.CancellationException) {
            predictiveBackProgress.animateTo(0f, animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f))
        }
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
        accentColor = uiState.accentColor,
        monochromeMode = uiState.monochromeIcons
    ) {
        val density = LocalDensity.current
        val isHub = currentSection == null

        CompositionLocalProvider(
            LocalHeaderFontFamily provides GoogleSansFontFamily,
            LocalMonochromeIcons provides uiState.monochromeIcons
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
                            IconButton(onClick = navigateBack) {
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
                modifier = modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (isHub && predictiveBackProgress.value > 0.001f) {
                            val p = predictiveBackProgress.value
                            val scale = 1f - (p * 0.10f)
                            scaleX = scale
                            scaleY = scale
                            val txMax = with(density) { 56.dp.toPx() }
                            translationX = if (predictiveBackSwipeEdge == BackEventCompat.EDGE_LEFT) p * txMax else -p * txMax
                            shadowElevation = p * 20f
                            shape = RoundedCornerShape((p * 28f).dp)
                            clip = true
                        }
                    }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (currentSection != null) {
                        MainSettingsHub(
                            uiState = uiState,
                            onSelectSection = navigateToSection,
                            horizontalPadding = horizontalPadding,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val p = predictiveBackProgress.value
                                    val scale = 0.94f + (p * 0.06f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = 0.5f + (p * 0.5f)
                                }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (!isHub && predictiveBackProgress.value > 0.001f) {
                                    val p = predictiveBackProgress.value
                                    val scale = 1f - (p * 0.10f)
                                    scaleX = scale
                                    scaleY = scale
                                    val txMax = with(density) { 56.dp.toPx() }
                                    translationX = if (predictiveBackSwipeEdge == BackEventCompat.EDGE_LEFT) p * txMax else -p * txMax
                                    shadowElevation = p * 24f
                                    shape = RoundedCornerShape((p * 28f).dp)
                                    clip = true
                                }
                            }
                            .background(MaterialTheme.colorScheme.surface),
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
                                onSelectSection = navigateToSection,
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
                                    SettingsSection.FEATURES -> FeaturesSubScreen(
                                        initialPageIndex = lastSelectedFeatureIndex,
                                        onNavigateToSetting = { section, key, cardIndex ->
                                            lastSelectedFeatureIndex = cardIndex
                                            targetHighlightKey = key
                                            navigateToSection(section)
                                        }
                                    )
                                    SettingsSection.APPEARANCE,
                                    @Suppress("DEPRECATION") SettingsSection.THEMES,
                                    @Suppress("DEPRECATION") SettingsSection.WALLPAPER,
                                    @Suppress("DEPRECATION") SettingsSection.SURFACES,
                                    @Suppress("DEPRECATION") SettingsSection.TYPOGRAPHY_ICONS -> AppearanceSubScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        onShowIconPicker = { showIconPicker = true },
                                        highlightKey = targetHighlightKey
                                    )
                                    SettingsSection.DRAWERS,
                                    @Suppress("DEPRECATION") SettingsSection.HOME_DRAWERS -> DrawersSubScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        highlightKey = targetHighlightKey
                                    )
                                    SettingsSection.WORKSPACE,
                                    @Suppress("DEPRECATION") SettingsSection.DOCK,
                                    @Suppress("DEPRECATION") SettingsSection.GLANCE -> WorkspaceSubScreen(
                                        uiState = uiState,
                                        viewModel = viewModel,
                                        onShowCityDialog = { showCityDialog = true },
                                        highlightKey = targetHighlightKey
                                    )
                                    SettingsSection.SYSTEM,
                                    @Suppress("DEPRECATION") SettingsSection.PRIVACY,
                                    @Suppress("DEPRECATION") SettingsSection.ABOUT -> SystemSubScreen(
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
                                        onOpenWalkthrough = onOpenWalkthrough,
                                        highlightKey = targetHighlightKey
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
        // Section: Features & Guides
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingsSectionHeader(title = "Guides")
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                SettingsNavTileRow(
                    title = SettingsSection.FEATURES.title,
                    subtitle = "App vault, tasks, notification muting, power metrics & shortcuts",
                    icon = Icons.Default.Explore,
                    onClick = { onSelectSection(SettingsSection.FEATURES) }
                )
            }
        }

        // Section: Core Settings (The Big 4 Hubs)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingsSectionHeader(title = "Settings")
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column {
                    SettingsNavTileRow(
                        title = SettingsSection.APPEARANCE.title,
                        subtitle = "$themeLabel Theme • ${if (uiState.dynamicColor) "Material You" else "Custom Accent"} • ${uiState.surfaceThemeStyle.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}",
                        icon = Icons.Default.Palette,
                        onClick = { onSelectSection(SettingsSection.APPEARANCE) }
                    )

                    SettingsDivider(modifier = Modifier.padding(horizontal = 12.dp))

                    SettingsNavTileRow(
                        title = SettingsSection.DRAWERS.title,
                        subtitle = "${uiState.appDrawerColumns} cols • ${if (uiState.twoDrawersSideBySide) "Dual column" else "Single column"} • ${uiState.categoryAutoCollapseSeconds}s auto-collapse",
                        icon = Icons.Default.ViewCarousel,
                        onClick = { onSelectSection(SettingsSection.DRAWERS) }
                    )

                    SettingsDivider(modifier = Modifier.padding(horizontal = 12.dp))

                    SettingsNavTileRow(
                        title = SettingsSection.WORKSPACE.title,
                        subtitle = "${uiState.dockIconSize.toInt()}dp dock • ${if (uiState.showWeather) "Weather on" else "Weather off"} • ${if (uiState.is24Hour) "24h" else "12h"}",
                        icon = Icons.Default.Phone,
                        onClick = { onSelectSection(SettingsSection.WORKSPACE) }
                    )

                    SettingsDivider(modifier = Modifier.padding(horizontal = 12.dp))

                    SettingsNavTileRow(
                        title = SettingsSection.SYSTEM.title,
                        subtitle = "${if (uiState.appVaultEnabled) "App Vault on" else "App Vault off"} • Backup & Restore • v$installedVersion",
                        icon = Icons.Default.Security,
                        onClick = { onSelectSection(SettingsSection.SYSTEM) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
