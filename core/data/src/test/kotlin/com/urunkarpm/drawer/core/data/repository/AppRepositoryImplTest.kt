package com.urunkarpm.drawer.core.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.UserHandle
import android.os.UserManager
import com.urunkarpm.drawer.core.model.AppInfo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk(relaxed = true)
    private val launcherApps: LauncherApps = mockk(relaxed = true)
    private val userManager: UserManager = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk(relaxed = true)

    private val myUser: UserHandle = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { context.getSystemService(Context.LAUNCHER_APPS_SERVICE) } returns launcherApps
        every { context.getSystemService(Context.USER_SERVICE) } returns userManager
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.urunkarpm.drawer"
        every { userManager.userProfiles } returns listOf(myUser)
    }

    @Test
    fun launchApp_startsMainActivityWithCorrectComponentAndUser() = runTest(testDispatcher) {
        val repository = AppRepositoryImpl(context, testDispatcher)

        val app = AppInfo(
            packageName = "com.test.app",
            activityName = "com.test.app.MainActivity",
            label = "Test App",
            userHandleId = myUser.hashCode()
        )

        val launched = repository.launchApp(app)
        assertTrue(launched)

        verify {
            launcherApps.startMainActivity(
                any(),
                myUser,
                null,
                null
            )
        }
    }

    @Test
    fun getShortcuts_returnsEmptyListWhenPermissionMissing() = runTest(testDispatcher) {
        val repository = AppRepositoryImpl(context, testDispatcher)
        every { launcherApps.hasShortcutHostPermission() } returns false

        val app = AppInfo(
            packageName = "com.test.app",
            activityName = "com.test.app.MainActivity",
            label = "Test App",
            userHandleId = myUser.hashCode()
        )

        val shortcuts = repository.getShortcuts(app)
        assertTrue(shortcuts.isEmpty())
    }
}
