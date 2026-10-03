package com.urunkarpm.drawer.feature.groups.component

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
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
import com.urunkarpm.drawer.core.model.AppGroup
import com.urunkarpm.drawer.core.model.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupItemActionBottomSheet(
    app: AppInfo,
    group: AppGroup,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onLaunchApp: () -> Unit,
    onRemoveFromGroup: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(
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
                        text = "Category: ${group.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            NavigationDrawerItem(
                label = { Text("Open") },
                icon = { Icon(Icons.Outlined.PlayArrow, contentDescription = "Open") },
                selected = false,
                onClick = onLaunchApp,
                colors = NavigationDrawerItemDefaults.colors()
            )

            NavigationDrawerItem(
                label = { Text("Remove from ${group.name}") },
                icon = { Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = "Remove") },
                selected = false,
                onClick = onRemoveFromGroup,
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
