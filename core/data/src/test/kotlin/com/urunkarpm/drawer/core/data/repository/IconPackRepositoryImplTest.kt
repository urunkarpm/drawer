package com.urunkarpm.drawer.core.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.urunkarpm.drawer.core.database.dao.IconPackOverrideDao
import com.urunkarpm.drawer.core.database.entity.IconPackOverrideEntity
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IconPackRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)
    private val iconPackOverrideDao: IconPackOverrideDao = mockk(relaxed = true)

    private val activeIconPackFlow = MutableStateFlow<String?>(null)
    private val overridesFlow = MutableStateFlow<List<IconPackOverrideEntity>>(emptyList())

    private lateinit var repository: IconPackRepositoryImpl

    @Before
    fun setUp() {
        every { context.packageManager } returns packageManager
        every { packageManager.queryIntentActivities(any(), any<Int>()) } returns emptyList()
        every { preferencesDataSource.activeIconPack } returns activeIconPackFlow
        every { iconPackOverrideDao.getAllOverrides() } returns overridesFlow

        repository = IconPackRepositoryImpl(context, preferencesDataSource, iconPackOverrideDao, testDispatcher)
    }

    @Test
    fun getInstalledIconPacks_alwaysIncludesSystemDefault() = runTest(testDispatcher) {
        val packs = repository.getInstalledIconPacks()
        assertTrue(packs.isNotEmpty())
        assertEquals("System Default", packs.first().name)
        assertTrue(packs.first().isSystemDefault)
    }

    @Test
    fun setActiveIconPack_updatesPreferences() = runTest(testDispatcher) {
        coEvery { preferencesDataSource.setActiveIconPack("com.test.pack") } returns Unit

        repository.setActiveIconPack("com.test.pack")

        coVerify { preferencesDataSource.setActiveIconPack("com.test.pack") }
    }

    @Test
    fun setAppOverride_upsertsOverrideInDao() = runTest(testDispatcher) {
        val captured = slot<IconPackOverrideEntity>()
        coEvery { iconPackOverrideDao.upsertOverride(capture(captured)) } returns Unit

        repository.setAppOverride("com.app/com.app.Act", "com.test.pack", "ic_app")

        coVerify { iconPackOverrideDao.upsertOverride(any()) }
        assertEquals("com.app/com.app.Act", captured.captured.componentName)
        assertEquals("com.test.pack", captured.captured.iconPackPackageName)
        assertEquals("ic_app", captured.captured.drawableName)
    }

    @Test
    fun removeAppOverride_deletesOverrideInDao() = runTest(testDispatcher) {
        coEvery { iconPackOverrideDao.deleteOverride("com.app/com.app.Act") } returns Unit

        repository.removeAppOverride("com.app/com.app.Act")

        coVerify { iconPackOverrideDao.deleteOverride("com.app/com.app.Act") }
    }
}
