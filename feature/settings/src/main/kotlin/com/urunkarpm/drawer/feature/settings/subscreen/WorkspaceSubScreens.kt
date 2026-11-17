package com.urunkarpm.drawer.feature.settings.subscreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.urunkarpm.drawer.core.data.util.DevicePostureUtils
import com.urunkarpm.drawer.feature.settings.SettingsUiState
import com.urunkarpm.drawer.feature.settings.SettingsViewModel
import com.urunkarpm.drawer.feature.settings.component.SettingsDivider
import com.urunkarpm.drawer.feature.settings.component.SettingsSegmentedRow
import com.urunkarpm.drawer.feature.settings.component.SettingsSliderRow
import com.urunkarpm.drawer.feature.settings.component.SettingsSwitchRow
import com.urunkarpm.drawer.feature.settings.component.settingGlowEffect

@Composable
fun HomeDrawersSubScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    highlightKey: String? = null
) {
    DrawersSubScreen(uiState, viewModel, modifier, highlightKey)
}

@Composable
fun DrawersSubScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    highlightKey: String? = null
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.settingGlowEffect(
                isHighlighted = highlightKey == "HOME_DRAWERS",
                shape = RoundedCornerShape(20.dp)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Category Drawers",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

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
                        "BOTTOM" to "Bottom (Thumb Zone)",
                        "TOP" to "Top Stack"
                    ),
                    selected = uiState.categoryAlignment,
                    onSelect = { viewModel.setCategoryAlignment(it) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                SettingsSwitchRow(
                    title = "Two Drawers Side by Side",
                    subtitle = "Show category drawers in dual columns side by side on home page",
                    checked = uiState.twoDrawersSideBySide,
                    onCheckedChange = { viewModel.setTwoDrawersSideBySide(it) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "Auto-Collapse On Inactivity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Automatically snap category folders shut when idle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSegmentedRow(
                    options = listOf(
                        "0" to "Never",
                        "3" to "3 Seconds",
                        "5" to "5 Seconds"
                    ),
                    selected = uiState.categoryAutoCollapseSeconds.toString(),
                    onSelect = { viewModel.setCategoryAutoCollapseSeconds(it.toIntOrNull() ?: 3) }
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

                if (!uiState.hideDrawerIcons) {
                    SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "Drawer Icon Style",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Visual theme for category and drawer icon badges on your home screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSegmentedRow(
                        options = listOf(
                            "NEUTRAL" to "Frosted Glass",
                            "DYNAMIC" to "Material You",
                            "EDITORIAL" to "Muted Palette"
                        ),
                        selected = uiState.drawerIconStyle,
                        onSelect = { viewModel.setDrawerIconStyle(it) }
                    )

                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchRow(
                        title = "Glowing Drawer Icon",
                        subtitle = "Pulse glow the drawer icon when an app inside has a notification until acknowledged",
                        checked = uiState.drawerNotificationGlow,
                        onCheckedChange = { viewModel.setDrawerNotificationGlow(it) }
                    )
                }

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Auto-Suggest Categories for New Apps",
                    subtitle = "Suggest drawer categories when installing new apps",
                    checked = uiState.autoCategorizeNewApps,
                    onCheckedChange = { viewModel.setAutoCategorizeNewApps(it) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

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
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "All Apps Drawer",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

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
                    text = "App Drawer Columns",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Number of app icons per row in the all apps drawer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSegmentedRow(
                    options = listOf(
                        "4" to "4 Icons",
                        "5" to "5 Icons",
                        "6" to "6 Icons"
                    ),
                    selected = uiState.appDrawerColumns.toString(),
                    onSelect = { viewModel.setAppDrawerColumns(it.toIntOrNull() ?: 4) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                SettingsSwitchRow(
                    title = "Show Suggested Apps",
                    subtitle = "Surface relevant apps based on your routine and context",
                    checked = uiState.showDrawerSuggestions,
                    onCheckedChange = { viewModel.setShowDrawerSuggestions(it) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Open Drawer From Dock Only",
                    subtitle = "Only open the app drawer when swiping up on the dock, disabling home screen swipe up",
                    checked = uiState.openDrawerFromDockOnly,
                    onCheckedChange = { viewModel.setOpenDrawerFromDockOnly(it) }
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
                    title = "Blur App List on Open",
                    subtitle = "Obscure apps with a blur when opening All Apps until tapped",
                    checked = uiState.blurAppListOnOpen,
                    onCheckedChange = { viewModel.setBlurAppListOnOpen(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun WorkspaceSubScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onShowCityDialog: () -> Unit,
    modifier: Modifier = Modifier,
    highlightKey: String? = null
) {
    val context = LocalContext.current
    val isFoldableOrTablet = remember(context) {
        DevicePostureUtils.isFoldableOrTablet(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card 1: Dock Bar
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Dock Bar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingsSliderRow(
                    title = "Dock Icon Size",
                    subtitle = "Scaled proportionally across all dock slots (smaller size allows up to 8 dock apps)",
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
                    title = "Dock Edge Glow",
                    subtitle = "Displays a light highlight along the dock edge",
                    checked = uiState.dockMesmerizingGlow,
                    onCheckedChange = { viewModel.setDockMesmerizingGlow(it) }
                )
            }
        }

        // Card 2: Glance Header
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Glance Header",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

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
                            .clickable { onShowCityDialog() }
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
                    title = "DuoStatus Indicators",
                    subtitle = "Show live power wattage, battery, speed test, and Wi-Fi indicator between time and weather",
                    checked = uiState.showDuoStatusWidget,
                    onCheckedChange = { viewModel.setShowDuoStatusWidget(it) },
                    modifier = Modifier.settingGlowEffect(
                        isHighlighted = highlightKey == "DUO_STATUS",
                        shape = RoundedCornerShape(12.dp)
                    )
                )

                if (uiState.showDuoStatusWidget) {
                    SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "DuoStatus Position",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Placement in glance header row",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSegmentedRow(
                        options = listOf(
                            "TEMP" to "Weather",
                            "CENTER" to "Center Pill",
                            "TIME" to "Time"
                        ),
                        selected = uiState.duoStatusPosition,
                        onSelect = { viewModel.setDuoStatusPosition(it) }
                    )
                }

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Front Camera Mirror Shortcut",
                    subtitle = "Tap front camera punch hole to quickly open front camera mirror",
                    checked = uiState.enableCameraMirror,
                    onCheckedChange = { viewModel.setEnableCameraMirror(it) },
                    modifier = Modifier.settingGlowEffect(
                        isHighlighted = highlightKey == "CAMERA_MIRROR",
                        shape = RoundedCornerShape(12.dp)
                    )
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Frosted Island Backdrop",
                    subtitle = "Render a subtle specular hairline glass pill behind Glance clock and weather for guaranteed contrast",
                    checked = uiState.glanceFrostedBackdrop,
                    onCheckedChange = { viewModel.setGlanceFrostedBackdrop(it) }
                )
            }
        }

        // Card 3: Screen & Feed Layout
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Screen & Feed Layout",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingsSwitchRow(
                    title = "Left-Hand Widget Page",
                    subtitle = "Enable dedicated full-page widget feed by swiping right from Home",
                    checked = uiState.enableWidgetsPage,
                    onCheckedChange = { viewModel.setEnableWidgetsPage(it) },
                    modifier = Modifier.settingGlowEffect(
                        isHighlighted = highlightKey == "WIDGETS",
                        shape = RoundedCornerShape(12.dp)
                    )
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Hide Status Bar",
                    subtitle = "Maximize screen real estate by hiding the system status bar",
                    checked = uiState.hideStatusBar,
                    onCheckedChange = { viewModel.setHideStatusBar(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DockSubScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFoldableOrTablet = remember(context) {
        DevicePostureUtils.isFoldableOrTablet(context)
    }

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
                SettingsSliderRow(
                    title = "Dock Icon Size",
                    subtitle = "Scaled proportionally across all dock slots (smaller size allows up to 8 dock apps)",
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
                    title = "Dock Edge Glow",
                    subtitle = "Displays a light highlight along the dock edge",
                    checked = uiState.dockMesmerizingGlow,
                    onCheckedChange = { viewModel.setDockMesmerizingGlow(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun GlanceSubScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onShowCityDialog: () -> Unit,
    modifier: Modifier = Modifier,
    highlightKey: String? = null
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
                            .clickable { onShowCityDialog() }
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
                    title = "DuoStatus Indicators",
                    subtitle = "Show live power wattage, battery, speed test, and Wi-Fi indicator between time and weather",
                    checked = uiState.showDuoStatusWidget,
                    onCheckedChange = { viewModel.setShowDuoStatusWidget(it) },
                    modifier = Modifier.settingGlowEffect(
                        isHighlighted = highlightKey == "DUO_STATUS",
                        shape = RoundedCornerShape(12.dp)
                    )
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Front Camera Mirror Shortcut",
                    subtitle = "Tap front camera punch hole to quickly open front camera mirror",
                    checked = uiState.enableCameraMirror,
                    onCheckedChange = { viewModel.setEnableCameraMirror(it) },
                    modifier = Modifier.settingGlowEffect(
                        isHighlighted = highlightKey == "CAMERA_MIRROR",
                        shape = RoundedCornerShape(12.dp)
                    )
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Frosted Island Backdrop",
                    subtitle = "Render a subtle specular hairline glass pill behind Glance clock and weather for guaranteed contrast",
                    checked = uiState.glanceFrostedBackdrop,
                    onCheckedChange = { viewModel.setGlanceFrostedBackdrop(it) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Left-Hand Widget Page",
                    subtitle = "Enable dedicated full-page widget feed by swiping right from Home",
                    checked = uiState.enableWidgetsPage,
                    onCheckedChange = { viewModel.setEnableWidgetsPage(it) },
                    modifier = Modifier.settingGlowEffect(
                        isHighlighted = highlightKey == "WIDGETS",
                        shape = RoundedCornerShape(12.dp)
                    )
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                SettingsSwitchRow(
                    title = "Hide Status Bar",
                    subtitle = "Maximize screen real estate by hiding the system status bar",
                    checked = uiState.hideStatusBar,
                    onCheckedChange = { viewModel.setHideStatusBar(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
