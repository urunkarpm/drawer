package com.urunkarpm.drawer.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DrawerPreferencesDataSourceTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private fun createDataSource(scope: CoroutineScope): DrawerPreferencesDataSource {
        val testFile = File(tmpFolder.root, "test_prefs_${System.nanoTime()}.preferences_pb")
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { testFile }
        )
        return DrawerPreferencesDataSource(testDataStore)
    }

    @Test
    fun defaultHeaderFontFamilyIsGoogleSans() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals("GOOGLE_SANS", dataSource.headerFontFamily.first())
    }

    @Test
    fun updateHeaderFontFamilyPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setHeaderFontFamily("SERIF")
        assertEquals("SERIF", dataSource.headerFontFamily.first())
    }

    @Test
    fun defaultAppLabelScaleIsNormal() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals("NORMAL", dataSource.appLabelScale.first())
    }

    @Test
    fun updateAppLabelScalePersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setAppLabelScale("LARGE")
        assertEquals("LARGE", dataSource.appLabelScale.first())
    }

    @Test
    fun defaultNotificationSwipeActionsAreCorrect() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals(
            com.urunkarpm.drawer.core.model.NotificationSwipeAction.MARK_AS_READ,
            dataSource.notificationSwipeRightAction.first()
        )
        assertEquals(
            com.urunkarpm.drawer.core.model.NotificationSwipeAction.DISMISS,
            dataSource.notificationSwipeLeftAction.first()
        )
    }

    @Test
    fun updateNotificationSwipeRightActionPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setNotificationSwipeRightAction(com.urunkarpm.drawer.core.model.NotificationSwipeAction.OPEN)
        assertEquals(
            com.urunkarpm.drawer.core.model.NotificationSwipeAction.OPEN,
            dataSource.notificationSwipeRightAction.first()
        )
    }

    @Test
    fun updateNotificationSwipeLeftActionPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setNotificationSwipeLeftAction(com.urunkarpm.drawer.core.model.NotificationSwipeAction.NONE)
        assertEquals(
            com.urunkarpm.drawer.core.model.NotificationSwipeAction.NONE,
            dataSource.notificationSwipeLeftAction.first()
        )
    }

    @Test
    fun defaultOpenDrawerFromDockOnlyIsFalse() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals(false, dataSource.openDrawerFromDockOnly.first())
    }

    @Test
    fun updateOpenDrawerFromDockOnlyPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setOpenDrawerFromDockOnly(true)
        assertEquals(true, dataSource.openDrawerFromDockOnly.first())
    }

    @Test
    fun defaultDuoStatusPositionIsTemp() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals("TEMP", dataSource.duoStatusPosition.first())
    }

    @Test
    fun updateDuoStatusPositionPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setDuoStatusPosition("TIME")
        assertEquals("TIME", dataSource.duoStatusPosition.first())
    }

    @Test
    fun defaultHasCompletedWalkthroughIsFalse() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals(false, dataSource.hasCompletedWalkthrough.first())
    }

    @Test
    fun defaultDrawerNotificationGlowIsTrue() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals(true, dataSource.drawerNotificationGlow.first())
    }

    @Test
    fun defaultDynamicWallpaperPreferencesAreCorrect() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals(false, dataSource.dynamicWallpaperEnabled.first())
        assertEquals(3, dataSource.wallpaperGradientSpeedSeconds.first())
        assertEquals(40, dataSource.wallpaperGradientBlurPercent.first())
        assertEquals("FULL_SPECTRUM", dataSource.wallpaperGradientPalette.first())
        assertEquals("DIAGONAL", dataSource.wallpaperGradientAngle.first())
        assertEquals(true, dataSource.dockMesmerizingGlow.first())
    }

    @Test
    fun updateDynamicWallpaperEnabledPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setDynamicWallpaperEnabled(true)
        assertEquals(true, dataSource.dynamicWallpaperEnabled.first())
    }

    @Test
    fun updateWallpaperGradientSpeedPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setWallpaperGradientSpeedSeconds(5)
        assertEquals(5, dataSource.wallpaperGradientSpeedSeconds.first())
    }

    @Test
    fun updateWallpaperGradientBlurPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setWallpaperGradientBlurPercent(50)
        assertEquals(50, dataSource.wallpaperGradientBlurPercent.first())
    }

    @Test
    fun updateWallpaperGradientPalettePersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setWallpaperGradientPalette("AURORA_BOREALIS")
        assertEquals("AURORA_BOREALIS", dataSource.wallpaperGradientPalette.first())
    }

    @Test
    fun updateWallpaperGradientAnglePersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setWallpaperGradientAngle("RADIAL")
        assertEquals("RADIAL", dataSource.wallpaperGradientAngle.first())
    }

    @Test
    fun updateDockMesmerizingGlowPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setDockMesmerizingGlow(false)
        assertEquals(false, dataSource.dockMesmerizingGlow.first())
    }

    @Test
    fun defaultAppLaunchCountsIsEmpty() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals(emptyMap<String, Int>(), dataSource.appLaunchCounts.first())
    }

    @Test
    fun recordAppLaunchPersistsCount() = runTest {
        val dataSource = createDataSource(backgroundScope)
        val key = "com.example.app/MainActivity#0"
        dataSource.recordAppLaunch(key)
        assertEquals(1, dataSource.appLaunchCounts.first()[key])
        org.junit.Assert.assertTrue((dataSource.appLastLaunchedTimes.first()[key] ?: 0L) > 0L)
    }

    @Test
    fun defaultAutoCategorizeNewAppsIsTrue() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals(true, dataSource.autoCategorizeNewApps.first())
    }

    @Test
    fun updateAutoCategorizeNewAppsPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setAutoCategorizeNewApps(false)
        assertEquals(false, dataSource.autoCategorizeNewApps.first())
    }
}
