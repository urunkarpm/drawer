package com.urunkarpm.drawer.core.data.update

import android.app.NotificationManager
import android.content.Context
import com.urunkarpm.drawer.core.model.AppUpdateInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class UpdateNotificationHelperTest {

    private val context: Context = mockk(relaxed = true)
    private val notificationManager: NotificationManager = mockk(relaxed = true)
    private lateinit var helper: UpdateNotificationHelper

    @Before
    fun setUp() {
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        helper = UpdateNotificationHelper(context)
    }

    @Test
    fun showUpdateNotification_executesSafely() {
        val info = AppUpdateInfo(
            latestVersionTag = "v2.9.0",
            releaseName = "v2.9.0 Release",
            releaseNotes = "New features and fixes",
            apkDownloadUrl = "https://example.com/app.apk",
            htmlUrl = "https://example.com",
            isUpdateAvailable = true
        )
        helper.showUpdateNotification(info)
    }

    @Test
    fun dismissUpdateNotification_executesSafely() {
        helper.dismissUpdateNotification()
    }
}
