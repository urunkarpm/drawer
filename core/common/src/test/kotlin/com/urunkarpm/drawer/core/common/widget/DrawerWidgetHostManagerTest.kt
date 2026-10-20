package com.urunkarpm.drawer.core.common.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Looper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DrawerWidgetHostManagerTest {

    private val context: Context = mockk(relaxed = true)
    private val appWidgetManager: AppWidgetManager = mockk(relaxed = true)
    private val looper: Looper = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns appWidgetManager
        every { context.mainLooper } returns looper
        every { context.packageName } returns "com.urunkarpm.drawer"
        mockkConstructor(DrawerAppWidgetHost::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun createWidgetOptionsBundle_returnsNonNullBundle() {
        val manager = DrawerWidgetHostManager(context)
        val bundle = manager.createWidgetOptionsBundle(widthDp = 340, heightDp = 180)
        assertNotNull(bundle)
    }

    @Test
    fun onWidgetConfigured_invokesListenerAndClearsReference() {
        val manager = DrawerWidgetHostManager(context)
        var invokedResultCode = -1
        var invokedData: android.content.Intent? = null

        manager.onWidgetConfiguredListener = { resultCode, data ->
            invokedResultCode = resultCode
            invokedData = data
        }

        val testIntent = mockk<android.content.Intent>(relaxed = true)
        every { testIntent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) } returns 42

        manager.onWidgetConfigured(-1, testIntent)

        assertEquals(-1, invokedResultCode)
        assertEquals(42, invokedData?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1))
        assertNull(manager.onWidgetConfiguredListener)
    }

    @Test
    fun installedWidgetInfo_computesSpansCorrectly() {
        val info = InstalledWidgetInfo(
            providerInfo = mockk(relaxed = true),
            label = "Google Pay Scan QR",
            description = "Quick UPI QR scanner",
            spanX = 2,
            spanY = 2,
            minWidthDp = 150,
            minHeightDp = 110
        )

        assertEquals(2, info.spanX)
        assertEquals(2, info.spanY)
        assertEquals("Google Pay Scan QR", info.label)
        assertEquals("Quick UPI QR scanner", info.description)
    }
}
