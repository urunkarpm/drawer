package com.urunkarpm.drawer.feature.settings

import android.app.Activity
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.feature.iconpacks.IconPackViewModel
import com.urunkarpm.drawer.feature.iconpacks.component.IconPackPickerSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    var showCityDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Theme & Appearance
            item {
                SettingsCategoryHeader(title = "Appearance & Theme", icon = Icons.Outlined.ColorLens)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                Spacer(modifier = Modifier.height(12.dp))

                SettingsSwitchRow(
                    title = "Dynamic Color (Material You)",
                    subtitle = "Derive accent colors from your wallpaper (Android 12+)",
                    checked = uiState.dynamicColor,
                    onCheckedChange = { viewModel.setDynamicColor(it) }
                )

                SettingsSwitchRow(
                    title = "Hide Status Bar (Immersive)",
                    subtitle = "Maximize screen real estate by hiding the system status bar",
                    checked = uiState.hideStatusBar,
                    onCheckedChange = { viewModel.setHideStatusBar(it) }
                )
            }

            // 2. Dock Customization
            item {
                SettingsCategoryHeader(title = "Dock", icon = Icons.Default.ViewCarousel)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Background Style",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionChip(
                        label = "Blur",
                        selected = uiState.dockBackground == "BLUR",
                        onClick = { viewModel.setDockBackground("BLUR") },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionChip(
                        label = "Solid",
                        selected = uiState.dockBackground == "SOLID",
                        onClick = { viewModel.setDockBackground("SOLID") },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionChip(
                        label = "Transparent",
                        selected = uiState.dockBackground == "TRANSPARENT",
                        onClick = { viewModel.setDockBackground("TRANSPARENT") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Dock Icon Size: ${uiState.dockIconSize.toInt()} dp",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = uiState.dockIconSize,
                    onValueChange = { viewModel.setDockIconSize(it) },
                    valueRange = 40f..72f,
                    steps = 7,
                    modifier = Modifier.fillMaxWidth()
                )

                SettingsSwitchRow(
                    title = "Show App Labels in Dock",
                    subtitle = "Display app names beneath dock icons",
                    checked = uiState.dockShowLabels,
                    onCheckedChange = { viewModel.setDockShowLabels(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dock Corner Radius: ${uiState.dockCornerRadius.toInt()} dp",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = uiState.dockCornerRadius,
                    onValueChange = { viewModel.setDockCornerRadius(it) },
                    valueRange = 0f..36f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 3. App Groups
            item {
                SettingsCategoryHeader(title = "App Categories & Groups", icon = Icons.Default.FormatPaint)
                Spacer(modifier = Modifier.height(8.dp))

                SettingsSwitchRow(
                    title = "Multi-Group Apps",
                    subtitle = "Allow a single app to be assigned to multiple categories",
                    checked = uiState.multiGroupApps,
                    onCheckedChange = { viewModel.setMultiGroupApps(it) }
                )
            }

            // 4. Home Glance & Weather
            item {
                SettingsCategoryHeader(title = "At A Glance", icon = Icons.Default.Schedule)
                Spacer(modifier = Modifier.height(8.dp))

                SettingsSwitchRow(
                    title = "24-Hour Clock",
                    subtitle = "Use 24-hour format instead of 12-hour AM/PM",
                    checked = uiState.is24Hour,
                    onCheckedChange = { viewModel.setIs24Hour(it) }
                )

                SettingsSwitchRow(
                    title = "Show Weather",
                    subtitle = "Display current temperature and weather conditions",
                    checked = uiState.showWeather,
                    onCheckedChange = { viewModel.setShowWeather(it) }
                )

                if (uiState.showWeather) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Temperature Unit",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        onClick = { showCityDialog = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
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
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 5. Notifications
            item {
                SettingsCategoryHeader(title = "Notifications", icon = Icons.Default.Notifications)
                Spacer(modifier = Modifier.height(8.dp))

                SettingsSwitchRow(
                    title = "Privacy Mode",
                    subtitle = "Hide notification body content on the home screen",
                    checked = uiState.notificationsPrivacyMode,
                    onCheckedChange = { viewModel.setNotificationsPrivacyMode(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    onClick = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
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

            // 6. Icons & Themes
            item {
                SettingsCategoryHeader(title = "Icon Packs & Shapes", icon = Icons.Default.Palette)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    onClick = { showIconPicker = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Customize Icons & Shapes",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            val packLabel = iconPackUiState.installedPacks.firstOrNull { it.packageName == uiState.activeIconPack }?.name
                                ?: if (uiState.activeIconPack == null) "System Default" else uiState.activeIconPack
                            Text(
                                text = "Pack: $packLabel • Shape: ${uiState.adaptiveIconShape.lowercase().replaceFirstChar { it.uppercase() }}",
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

            // 7. Backup & Restore
            item {
                SettingsCategoryHeader(title = "Backup & Restore", icon = Icons.Default.FileUpload)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Export or import your entire configuration (categories, dock, mute rules, and personalisation settings) to a single JSON file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

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

            // 8. About & Information
            item {
                SettingsCategoryHeader(title = "About Drawer", icon = Icons.Outlined.Info)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
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
}

@Composable
private fun SettingsCategoryHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
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
            .padding(vertical = 8.dp),
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
