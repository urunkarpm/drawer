package com.urunkarpm.drawer.core.data.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.urunkarpm.drawer.core.model.AppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        const val CHANNEL_UPDATES = "drawer_app_updates"
        const val UPDATE_NOTIFICATION_ID = 9100
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_UPDATES,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when a new version of Drawer is available."
                setShowBadge(true)
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    // ponytail: standard platform notification for GitHub release updates with direct APK/release action; ceiling: background download manager; upgrade path: in-app package installer session.
    fun showUpdateNotification(info: AppUpdateInfo) {
        val downloadUrl = info.apkDownloadUrl ?: info.htmlUrl
        val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            UPDATE_NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Drawer ${info.latestVersionTag} Available"
        val subtitle = "A new version of Drawer is available to install."
        val notes = info.releaseNotes.takeIf { it.isNotBlank() && it != "No release notes provided." }

        val builder = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText("Update Available")
                    .bigText(if (notes != null) "$subtitle\n\n$notes" else subtitle)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.stat_sys_download,
                if (info.apkDownloadUrl?.endsWith(".apk", ignoreCase = true) == true) "Download APK" else "View Release",
                contentPendingIntent
            )

        try {
            notificationManager.notify(UPDATE_NOTIFICATION_ID, builder.build())
        } catch (_: Throwable) {
            // Permission or notification manager failure
        }
    }

    fun dismissUpdateNotification() {
        try {
            notificationManager.cancel(UPDATE_NOTIFICATION_ID)
        } catch (_: Throwable) {}
    }
}
