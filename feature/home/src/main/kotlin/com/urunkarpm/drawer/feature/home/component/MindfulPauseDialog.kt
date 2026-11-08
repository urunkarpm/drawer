package com.urunkarpm.drawer.feature.home.component

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.urunkarpm.drawer.core.designsystem.component.AppIconImage
import com.urunkarpm.drawer.core.model.AppInfo
import kotlinx.coroutines.delay

@Composable
fun MindfulPauseDialog(
    app: AppInfo,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    iconLoader: suspend () -> Drawable?,
    modifier: Modifier = Modifier
) {
    // ponytail: 3-second countdown running in a LaunchedEffect coroutine.
    // Ceiling: Fixed 3-second duration, does not persist across device reboots or lockouts. Upgrade path: configurable duration slider in Settings if users request 5s/10s.
    var secondsRemaining by remember { mutableIntStateOf(3) }

    LaunchedEffect(app) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        }
        onConfirm()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (3 - secondsRemaining) / 3f,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "mindfulProgress"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIconImage(
                    key = app.componentKey,
                    size = 56.dp,
                    label = app.label,
                    isWorkProfile = app.isWorkProfile,
                    iconLoader = iconLoader
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Take a breath",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Opening ${app.label}...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(76.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(76.dp)
                    )
                    Text(
                        text = if (secondsRemaining > 0) "$secondsRemaining" else "✓",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(0.48f)
            ) {
                Text("Open Now")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(0.48f)
            ) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}
