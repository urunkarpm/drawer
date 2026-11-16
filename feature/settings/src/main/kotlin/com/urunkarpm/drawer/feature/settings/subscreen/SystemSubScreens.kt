package com.urunkarpm.drawer.feature.settings.subscreen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.urunkarpm.drawer.core.model.NotificationSwipeAction
import com.urunkarpm.drawer.feature.settings.SettingsUiState
import com.urunkarpm.drawer.feature.settings.SettingsViewModel
import com.urunkarpm.drawer.feature.settings.component.SettingsDivider
import com.urunkarpm.drawer.feature.settings.component.SettingsSegmentedRow
import com.urunkarpm.drawer.feature.settings.component.SettingsSwitchRow
import com.urunkarpm.drawer.feature.settings.component.settingGlowEffect

@Composable
fun SystemSubScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenWalkthrough: () -> Unit,
    modifier: Modifier = Modifier,
    highlightKey: String? = null
) {
    val context = LocalContext.current
    val installedVersion = remember(context) {
        runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.2.4"
        }.getOrDefault("1.2.4")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card 1: App Vault & Privacy
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.settingGlowEffect(
                isHighlighted = highlightKey == "APP_VAULT",
                shape = RoundedCornerShape(20.dp)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "App Vault & Privacy",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingsSwitchRow(
                    title = "Private App Vault",
                    subtitle = "Discreet hidden app drawer accessed by swiping the rightmost dock icon to the right",
                    checked = uiState.appVaultEnabled,
                    onCheckedChange = { viewModel.setAppVaultEnabled(it) }
                )

                if (uiState.appVaultEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "App Vault Guide",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    VaultSwipeGestureAnimation()

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "How to Access",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Swipe the rightmost app icon on your dock to the right to trigger authentication. Complete biometric or device PIN verification to reveal the vault. Swiping down on the vault drawer dismisses it instantly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            GlowingCaretIndicator()
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Private Notifications & Caret Glow",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Alerts from vault apps are kept completely private and hidden from the home drawer. When a hidden app receives an alert, the caret expand icon (chevron down) on your home notification drawer gently glows as a discreet cue. Open the vault by swiping the last dock icon to view and action these notifications inside its private notification drawer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 2: Notification Hub & Stealth
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.settingGlowEffect(
                isHighlighted = highlightKey == "NOTIFICATIONS",
                shape = RoundedCornerShape(20.dp)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Notification Hub & Stealth",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

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

                Text(
                    text = "Notification Swipe Right",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Action when swiping notification card from left to right",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSegmentedRow(
                    options = listOf(
                        "MARK_AS_READ" to "Mark Read",
                        "DISMISS" to "Dismiss",
                        "OPEN" to "Open",
                        "NONE" to "None"
                    ),
                    selected = uiState.notificationSwipeRightAction.name,
                    onSelect = { viewModel.setNotificationSwipeRightAction(NotificationSwipeAction.valueOf(it)) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Notification Swipe Left",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Action when swiping notification card from right to left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSegmentedRow(
                    options = listOf(
                        "DISMISS" to "Dismiss",
                        "MARK_AS_READ" to "Mark Read",
                        "OPEN" to "Open",
                        "NONE" to "None"
                    ),
                    selected = uiState.notificationSwipeLeftAction.name,
                    onSelect = { viewModel.setNotificationSwipeLeftAction(NotificationSwipeAction.valueOf(it)) }
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
            }
        }

        // Card 3: Backup & Restore
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
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
                        onClick = onExportBackup,
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
                        onClick = onImportBackup,
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
                    OutlinedButton(onClick = onOpenWalkthrough) {
                        Text("Start Tour")
                    }
                }
            }
        }

        // Card 4: Updates & System
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Updates & System",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingsSwitchRow(
                    title = "Haptic Feedback",
                    subtitle = "Tactile response on gestures, slider notches, and long-press actions",
                    checked = uiState.hapticFeedbackEnabled,
                    onCheckedChange = { viewModel.setHapticFeedbackEnabled(it) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 10.dp))

                SettingsSwitchRow(
                    title = "Auto-Check for Updates",
                    subtitle = "Automatically check for new releases on launch",
                    checked = uiState.autoCheckUpdates,
                    onCheckedChange = { viewModel.setAutoCheckUpdates(it) }
                )

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
                        onClick = { viewModel.checkForUpdates(installedVersion) },
                        enabled = !uiState.isCheckingForUpdate
                    ) {
                        if (uiState.isCheckingForUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Checking...")
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

                SettingsDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "About Drawer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PrivacySubScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    highlightKey: String? = null
) {
    val context = LocalContext.current

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
                isHighlighted = highlightKey == "APP_VAULT",
                shape = RoundedCornerShape(20.dp)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "App Vault",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Discreet hidden app drawer with Biometric protection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingsSwitchRow(
                    title = "Private App Vault",
                    subtitle = "Discreet hidden app drawer accessed by swiping the rightmost dock icon to the right",
                    checked = uiState.appVaultEnabled,
                    onCheckedChange = { viewModel.setAppVaultEnabled(it) }
                )

                if (uiState.appVaultEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "App Vault Guide",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    VaultSwipeGestureAnimation()

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "How to Access",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Swipe the rightmost app icon on your dock to the right to trigger authentication. Complete biometric or device PIN verification to reveal the vault. Swiping down on the vault drawer dismisses it instantly.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            GlowingCaretIndicator()
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Private Notifications & Caret Glow",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Alerts from vault apps are kept completely private and hidden from the home drawer. When a hidden app receives an alert, the caret expand icon (chevron down) on your home notification drawer gently glows as a discreet cue. Open the vault by swiping the last dock icon to view and action these notifications inside its private notification drawer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.settingGlowEffect(
                isHighlighted = highlightKey == "NOTIFICATIONS",
                shape = RoundedCornerShape(20.dp)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Notification Hub & Privacy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Manage notification listeners, body previews, and stealth mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

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

                Text(
                    text = "Notification Swipe Right",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Action when swiping notification card from left to right",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSegmentedRow(
                    options = listOf(
                        "MARK_AS_READ" to "Mark Read",
                        "DISMISS" to "Dismiss",
                        "OPEN" to "Open",
                        "NONE" to "None"
                    ),
                    selected = uiState.notificationSwipeRightAction.name,
                    onSelect = { viewModel.setNotificationSwipeRightAction(NotificationSwipeAction.valueOf(it)) }
                )

                SettingsDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Notification Swipe Left",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Action when swiping notification card from right to left",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSegmentedRow(
                    options = listOf(
                        "DISMISS" to "Dismiss",
                        "MARK_AS_READ" to "Mark Read",
                        "OPEN" to "Open",
                        "NONE" to "None"
                    ),
                    selected = uiState.notificationSwipeLeftAction.name,
                    onSelect = { viewModel.setNotificationSwipeLeftAction(NotificationSwipeAction.valueOf(it)) }
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AboutSubScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOpenWalkthrough: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val installedVersion = remember(context) {
        runCatching {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.2.4"
        }.getOrDefault("1.2.4")
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
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
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
                        onClick = onExportBackup,
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
                        onClick = onImportBackup,
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

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "About Drawer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
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
                    OutlinedButton(onClick = onOpenWalkthrough) {
                        Text("Start Tour")
                    }
                }

                SettingsDivider(modifier = Modifier.padding(vertical = 10.dp))

                SettingsSwitchRow(
                    title = "Auto-Check for Updates",
                    subtitle = "Automatically check for new releases on launch",
                    checked = uiState.autoCheckUpdates,
                    onCheckedChange = { viewModel.setAutoCheckUpdates(it) }
                )

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
                        onClick = { viewModel.checkForUpdates(installedVersion) },
                        enabled = !uiState.isCheckingForUpdate
                    ) {
                        if (uiState.isCheckingForUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Checking...")
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ponytail: looping animated dock gesture preview demonstrating rightmost icon drag to trigger hidden vault; ceiling: 1D horizontal loop; upgrade path: interactive touch playground.
@Composable
private fun VaultSwipeGestureAnimation(
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val maxDragDp = 42.dp
    val maxDragPx = with(density) { maxDragDp.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "vaultSwipeDemo")
    val dragOffsetPx by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3200
                0f at 0
                0f at 400
                maxDragPx at 1400 using FastOutSlowInEasing
                maxDragPx at 1900
                0f at 2400 using FastOutSlowInEasing
                0f at 3200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dragOffsetPx"
    )

    val touchAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3200
                0f at 0
                0.9f at 350
                0.9f at 1900
                0f at 2300
                0f at 3200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "touchAlpha"
    )

    val isTriggered = dragOffsetPx >= maxDragPx * 0.7f
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stage: Miniature Dock + Vault trigger zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Dock Bar representation (background pill with slots, unclipped so 4th icon can slide out)
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(23.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(23.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 3 static placeholder dock apps
                            repeat(3) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (index) {
                                                0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                1 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                                else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (index) {
                                                    0 -> MaterialTheme.colorScheme.primary
                                                    1 -> MaterialTheme.colorScheme.secondary
                                                    else -> MaterialTheme.colorScheme.tertiary
                                                }
                                            )
                                    )
                                }
                            }

                            // 4th app: Rightmost dock app being dragged/swiped right!
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .graphicsLayer {
                                        translationX = dragOffsetPx
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // The app icon
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .border(
                                            width = 1.dp,
                                            color = if (isTriggered) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isTriggered) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isTriggered) primaryColor else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Touch pointer / finger indicator
                                if (touchAlpha > 0.01f) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor.copy(alpha = touchAlpha * 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(primaryColor.copy(alpha = touchAlpha))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (isTriggered) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Target Vault Indicator
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .then(
                                if (isTriggered) {
                                    Modifier.drawBehind {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    primaryColor.copy(alpha = 0.4f),
                                                    primaryColor.copy(alpha = 0.15f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                        drawCircle(
                                            color = primaryColor.copy(alpha = 0.8f),
                                            style = Stroke(width = 1.5.dp.toPx())
                                        )
                                    }
                                } else {
                                    Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isTriggered) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isTriggered) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Caption explaining the swipe action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Swipe rightmost dock icon to the right to open Vault",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ponytail: animated notification caret demonstrating discreet alert glow; ceiling: 2D pulse; upgrade path: synced with live notification state.
@Composable
private fun GlowingCaretIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "caretGlowDemo")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "demoCaretAlpha"
    )
    val glowColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = alpha),
                            glowColor.copy(alpha = alpha * 0.45f),
                            Color.Transparent
                        )
                    )
                )
                drawCircle(
                    color = glowColor.copy(alpha = alpha * 0.8f),
                    style = Stroke(width = 1.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Notification Caret Glow",
            tint = glowColor,
            modifier = Modifier.size(20.dp)
        )
    }
}
