package com.urunkarpm.drawer.feature.home.component

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContextMenuSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    onOpenSettings: () -> Unit,
    onAddCategory: () -> Unit,
    onAddWidget: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Home Screen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            HomeContextOption(
                icon = Icons.Outlined.Palette,
                label = "Change Wallpaper",
                description = "Choose system or live wallpaper",
                onClick = {
                    onDismissRequest()
                    launchWallpaperPicker(context)
                }
            )

            if (onAddWidget != null) {
                HomeContextOption(
                    icon = Icons.Default.Add,
                    label = "Add Widget",
                    description = "Choose and place an app widget",
                    onClick = {
                        onDismissRequest()
                        onAddWidget()
                    }
                )
            }

            HomeContextOption(
                icon = Icons.Default.Add,
                label = "Add Category Drawer",
                description = "Create a new expandable app drawer",
                onClick = {
                    onDismissRequest()
                    onAddCategory()
                }
            )

            HomeContextOption(
                icon = Icons.Outlined.Settings,
                label = "Launcher Settings",
                description = "Personalize layout, dock, icons, and gestures",
                onClick = {
                    onDismissRequest()
                    onOpenSettings()
                }
            )

            HomeContextOption(
                icon = Icons.Outlined.Smartphone,
                label = "System Settings",
                description = "Open Android device settings",
                onClick = {
                    onDismissRequest()
                    launchSystemSettings(context)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HomeContextOption(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun launchWallpaperPicker(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_SET_WALLPAPER)
        context.startActivity(Intent.createChooser(intent, "Set Wallpaper"))
    } catch (_: Exception) {
        // Fallback: system settings
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) {}
    }
}

private fun launchSystemSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (_: Exception) {}
}
