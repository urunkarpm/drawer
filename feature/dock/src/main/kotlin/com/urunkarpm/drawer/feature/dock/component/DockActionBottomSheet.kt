package com.urunkarpm.drawer.feature.dock.component

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.DockItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockActionBottomSheet(
    dockItem: DockItem,
    app: AppInfo?,
    totalItems: Int,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    onRemoveFromDock: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onOpenDetails: () -> Unit,
    onUninstall: () -> Unit,
    iconLoader: suspend () -> Drawable?,
    modifier: Modifier = Modifier
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
            val label = app?.label ?: dockItem.customLabel ?: dockItem.packageName
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(
                    size = 56.dp,
                    label = label,
                    isWorkProfile = app?.isWorkProfile ?: false,
                    iconLoader = iconLoader
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Slot ${dockItem.position + 1} of $totalItems",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            NavigationDrawerItem(
                label = { Text("Remove from dock") },
                icon = { Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = "Remove") },
                selected = false,
                onClick = onRemoveFromDock,
                colors = NavigationDrawerItemDefaults.colors()
            )

            if (dockItem.position > 0) {
                NavigationDrawerItem(
                    label = { Text("Move left") },
                    icon = { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Move left") },
                    selected = false,
                    onClick = onMoveLeft,
                    colors = NavigationDrawerItemDefaults.colors()
                )
            }

            if (dockItem.position < totalItems - 1) {
                NavigationDrawerItem(
                    label = { Text("Move right") },
                    icon = { Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Move right") },
                    selected = false,
                    onClick = onMoveRight,
                    colors = NavigationDrawerItemDefaults.colors()
                )
            }

            if (app != null) {
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
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
