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
    fun defaultHeaderFontFamilyIsSystem() = runTest {
        val dataSource = createDataSource(backgroundScope)
        assertEquals("SYSTEM", dataSource.headerFontFamily.first())
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
}
