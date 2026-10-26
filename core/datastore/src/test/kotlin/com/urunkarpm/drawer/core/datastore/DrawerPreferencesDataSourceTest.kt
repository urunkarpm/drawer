package com.urunkarpm.drawer.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.urunkarpm.drawer.core.model.LauncherPreferencesBackup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun defaultDrawerTitleAnimationIsAccentPop() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals("ACCENT_POP", dataSource.drawerTitleAnimation.first())
    }

    @Test
    fun updateDrawerTitleAnimationPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setDrawerTitleAnimation("KINETIC_DECEL")
        assertEquals("KINETIC_DECEL", dataSource.drawerTitleAnimation.first())
    }

    @Test
    fun getPreferencesBackupAndRestorePreferencesRoundTrip() = runTest {
        val dataSource = createDataSource(backgroundScope)
        val initialBackup = dataSource.getPreferencesBackup()
        assertEquals("SYSTEM", initialBackup.themeMode)
        assertEquals("ACCENT_POP", initialBackup.drawerTitleAnimation)
        assertEquals(true, initialBackup.dynamicColor)

        val customBackup = initialBackup.copy(
            themeMode = "DARK",
            drawerThemeMode = "AMOLED",
            drawerTitleAnimation = "KINETIC_DECEL",
            dynamicColor = false,
            hideStatusBar = false,
            dockBackground = "SOLID",
            surfaceThemeStyle = "FROSTED",
            surfaceOpacity = 0.85f,
            headerFontFamily = "SERIF",
            appLabelScale = "LARGE",
            stealthTheme = true,
            stealthContrastLevel = "HIGH",
            dynamicWallpaperEnabled = true,
            wallpaperGradientSpeedSeconds = 5,
            autoCategorizeNewApps = false,
            aiCategorizationStrictness = "STRICT",
            enableOnDeviceAi = true,
            cinematicPalette = "TEAL_AMBER"
        )

        dataSource.restorePreferences(customBackup)

        val restoredBackup = dataSource.getPreferencesBackup()
        assertEquals("DARK", restoredBackup.themeMode)
        assertEquals("AMOLED", restoredBackup.drawerThemeMode)
        assertEquals("KINETIC_DECEL", restoredBackup.drawerTitleAnimation)
        assertEquals(false, restoredBackup.dynamicColor)
        assertEquals(false, restoredBackup.hideStatusBar)
        assertEquals("SOLID", restoredBackup.dockBackground)
        assertEquals("FROSTED", restoredBackup.surfaceThemeStyle)
        assertEquals(0.85f, restoredBackup.surfaceOpacity, 0.001f)
        assertEquals("SERIF", restoredBackup.headerFontFamily)
        assertEquals("LARGE", restoredBackup.appLabelScale)
        assertEquals(true, restoredBackup.stealthTheme)
        assertEquals("HIGH", restoredBackup.stealthContrastLevel)
        assertEquals(true, restoredBackup.dynamicWallpaperEnabled)
        assertEquals(5, restoredBackup.wallpaperGradientSpeedSeconds)
        assertEquals(false, restoredBackup.autoCategorizeNewApps)
        assertEquals("STRICT", restoredBackup.aiCategorizationStrictness)
        assertEquals(true, restoredBackup.enableOnDeviceAi)
        assertEquals("TEAL_AMBER", restoredBackup.cinematicPalette)
    }

    @Test
    fun defaultCinematicPaletteIsDefault() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals("DEFAULT", dataSource.cinematicPalette.first())
    }

    @Test
    fun updateCinematicPalettePersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setCinematicPalette("CYBERPUNK")
        assertEquals("CYBERPUNK", dataSource.cinematicPalette.first())
    }

    @Test
    fun lastKnownWeatherLocationPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setLastKnownWeatherLocation(37.7749, -122.4194, "San Francisco")
        assertEquals(37.7749, dataSource.lastKnownWeatherLat.first()!!, 0.0001)
        assertEquals(-122.4194, dataSource.lastKnownWeatherLon.first()!!, 0.0001)
        assertEquals("San Francisco", dataSource.lastKnownWeatherCity.first())
    }

    @Test
    fun lastKnownWeatherLocationIgnoresPlaceholders() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setLastKnownWeatherLocation(37.7750, -122.4195, "My Location")
        assertEquals(37.7750, dataSource.lastKnownWeatherLat.first()!!, 0.0001)
        assertEquals(-122.4195, dataSource.lastKnownWeatherLon.first()!!, 0.0001)
        assertNull(dataSource.lastKnownWeatherCity.first())
    }

    @Test
    fun cachedWeatherInfoPersistsAndHydrates() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertNull(dataSource.cachedWeatherInfo.first())

        val info = com.urunkarpm.drawer.core.model.WeatherInfo(
            temperatureCelsius = 22.5,
            weatherCode = 0,
            conditionDescription = "Clear sky",
            cityName = "Austin",
            lastUpdatedMillis = 1700000000000L
        )
        dataSource.setCachedWeatherInfo(info)

        val cached = dataSource.cachedWeatherInfo.first()
        assertNotNull(cached)
        assertEquals(22.5, cached!!.temperatureCelsius, 0.01)
        assertEquals("Clear sky", cached.conditionDescription)
        assertEquals("Austin", cached.cityName)
    }

    @Test
    fun defaultBlurAppListOnOpenIsFalse() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals(false, dataSource.blurAppListOnOpen.first())
    }

    @Test
    fun updateBlurAppListOnOpenPersists() = runTest {
        val dataSource = createDataSource(backgroundScope)
        dataSource.setBlurAppListOnOpen(true)
        assertEquals(true, dataSource.blurAppListOnOpen.first())
    }
}
