package com.urunkarpm.drawer.feature.home.component

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.designsystem.component.toImageBitmapSafe
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.AppShortcutInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionBottomSheet(
    app: AppInfo,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    onLaunch: () -> Unit,
    onOpenDetails: () -> Unit,
    onUninstall: () -> Unit,
    onPinToDock: () -> Unit,
    onAddToGroup: () -> Unit,
    iconLoader: suspend () -> Drawable?,
    modifier: Modifier = Modifier,
    shortcuts: List<AppShortcutInfo> = emptyList(),
    onLaunchShortcut: (String) -> Unit = {},
    shortcutIconLoader: suspend (String) -> Drawable? = { null }
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // App Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(
                    key = app.componentKey,
                    size = 56.dp,
                    label = app.label,
                    isWorkProfile = app.isWorkProfile,
                    iconLoader = iconLoader
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (app.isWorkProfile) {
                        Spacer(modifier = Modifier.height(4.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Work Profile", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Native App Shortcuts
            if (shortcuts.isNotEmpty()) {
                Text(
                    text = "Shortcuts",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                shortcuts.forEach { shortcut ->
                    var iconDrawable by remember(shortcut.id) { mutableStateOf<Drawable?>(null) }
                    LaunchedEffect(shortcut.id) {
                        iconDrawable = shortcutIconLoader(shortcut.id)
                    }
                    NavigationDrawerItem(
                        label = {
                            Text(
                                text = shortcut.shortLabel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            val drawable = iconDrawable
                            if (drawable != null) {
                                val bitmap = remember(drawable) {
                                    drawable.toImageBitmapSafe()
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = shortcut.shortLabel,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        selected = false,
                        onClick = { onLaunchShortcut(shortcut.id) },
                        colors = NavigationDrawerItemDefaults.colors()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Actions
            NavigationDrawerItem(
                label = { Text("Open") },
                icon = { Icon(Icons.Outlined.PlayArrow, contentDescription = "Open") },
                selected = false,
                onClick = onLaunch,
                colors = NavigationDrawerItemDefaults.colors()
            )

            NavigationDrawerItem(
                label = { Text("Pin to Dock") },
                icon = { Icon(Icons.Outlined.VerticalAlignBottom, contentDescription = "Pin to Dock") },
                selected = false,
                onClick = onPinToDock,
                colors = NavigationDrawerItemDefaults.colors()
            )

            NavigationDrawerItem(
                label = { Text("Add to Category") },
                icon = { Icon(Icons.Outlined.Folder, contentDescription = "Add to Category") },
                selected = false,
                onClick = onAddToGroup,
                colors = NavigationDrawerItemDefaults.colors()
            )

            NavigationDrawerItem(
                label = { Text("App info") },
                icon = { Icon(Icons.Outlined.Info, contentDescription = "App info") },
                selected = false,
                onClick = onOpenDetails,
                colors = NavigationDrawerItemDefaults.colors()
            )

            NavigationDrawerItem(
                label = { Text("Uninstall", color = MaterialTheme.colorScheme.error) },
                icon = { Icon(Icons.Outlined.Delete, contentDescription = "Uninstall", tint = MaterialTheme.colorScheme.error) },
                selected = false,
                onClick = onUninstall,
                colors = NavigationDrawerItemDefaults.colors()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
