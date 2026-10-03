package com.urunkarpm.drawer.feature.notifications.component

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
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import com.urunkarpm.drawer.core.model.MuteRuleType
import com.urunkarpm.drawer.core.model.NotificationItem
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickMuteBottomSheet(
    item: NotificationItem,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onMute: (ruleType: MuteRuleType, durationMillis: Long?, autoDismiss: Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit,
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
                Icon(
                    imageVector = Icons.Outlined.NotificationsOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quick Mute: ${item.appName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notice card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Launcher rules hide or auto-dismiss notifications on Drawer home screen. For system-wide OS blocking, use Android settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            NavigationDrawerItem(
                label = { Text("Snooze for 1 hour") },
                icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                selected = false,
                onClick = {
                    onMute(MuteRuleType.SNOOZE_1H, TimeUnit.HOURS.toMillis(1), false)
                },
                colors = NavigationDrawerItemDefaults.colors()
            )

            NavigationDrawerItem(
                label = { Text("Snooze until tomorrow (24h)") },
                icon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                selected = false,
                onClick = {
                    onMute(MuteRuleType.SNOOZE_UNTIL_TOMORROW, TimeUnit.DAYS.toMillis(1), false)
                },
                colors = NavigationDrawerItemDefaults.colors()
            )

            NavigationDrawerItem(
                label = { Text("Hide on launcher (Permanent)") },
                icon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null) },
                selected = false,
                onClick = {
                    onMute(MuteRuleType.HIDE, null, false)
                },
                colors = NavigationDrawerItemDefaults.colors()
            )

            NavigationDrawerItem(
                label = { Text("Auto-dismiss future notifications") },
                icon = { Icon(Icons.Outlined.Cancel, contentDescription = null) },
                selected = false,
                onClick = {
                    onMute(MuteRuleType.HIDE, null, true)
                },
                colors = NavigationDrawerItemDefaults.colors()
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            NavigationDrawerItem(
                label = { Text("System notification settings") },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                selected = false,
                onClick = onOpenSystemSettings,
                colors = NavigationDrawerItemDefaults.colors()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
