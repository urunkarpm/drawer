package com.urunkarpm.drawer.core.data.repository

import android.content.Context
import com.urunkarpm.drawer.core.data.notification.NotificationBridge
import com.urunkarpm.drawer.core.database.dao.MutedAppDao
import com.urunkarpm.drawer.core.database.entity.MutedAppRuleEntity
import com.urunkarpm.drawer.core.model.MuteRuleType
import com.urunkarpm.drawer.core.model.NotificationItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepositoryImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context: Context = mockk(relaxed = true)
    private val notificationBridge: NotificationBridge = mockk(relaxed = true)
    private val mutedAppDao: MutedAppDao = mockk(relaxed = true)

    private val notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    private val rulesFlow = MutableStateFlow<List<MutedAppRuleEntity>>(emptyList())

    private lateinit var repository: NotificationRepositoryImpl

    private val notification1 = NotificationItem(
        key = "key_1",
        packageName = "com.test.app1",
        appName = "App One",
        title = "Hello 1",
        text = "Message 1",
        postTimeMillis = 1000L
    )

    private val notification2 = NotificationItem(
        key = "key_2",
        packageName = "com.test.app2",
        appName = "App Two",
        title = "Hello 2",
        text = "Message 2",
        postTimeMillis = 2000L
    )

    @Before
    fun setUp() {
        every { notificationBridge.notifications } returns notificationsFlow
        every { mutedAppDao.getAllRules() } returns rulesFlow
        repository = NotificationRepositoryImpl(context, notificationBridge, mutedAppDao, testDispatcher)
    }

    @Test
    fun activeNotifications_keepsUnmutedNotifications() = runTest(testDispatcher) {
        notificationsFlow.value = listOf(notification1, notification2)
        rulesFlow.value = emptyList()

        val active = repository.activeNotifications.first()
        assertEquals(2, active.size)
    }

    @Test
    fun activeNotifications_filtersOutHiddenApps() = runTest(testDispatcher) {
        notificationsFlow.value = listOf(notification1, notification2)
        rulesFlow.value = listOf(
            MutedAppRuleEntity(packageName = "com.test.app1", ruleType = MuteRuleType.HIDE.name)
        )

        val active = repository.activeNotifications.first()
        assertEquals(1, active.size)
        assertEquals("com.test.app2", active.first().packageName)
    }

    @Test
    fun activeNotifications_filtersOutActiveSnoozedApps() = runTest(testDispatcher) {
        notificationsFlow.value = listOf(notification1, notification2)
        rulesFlow.value = listOf(
            MutedAppRuleEntity(
                packageName = "com.test.app2",
                ruleType = MuteRuleType.SNOOZE_1H.name,
                snoozedUntilTimestamp = System.currentTimeMillis() + 100_000L
            )
        )

        val active = repository.activeNotifications.first()
        assertEquals(1, active.size)
        assertEquals("com.test.app1", active.first().packageName)
    }

    @Test
    fun muteApp_upsertsRuleInDao() = runTest(testDispatcher) {
        val captured = slot<MutedAppRuleEntity>()
        coEvery { mutedAppDao.upsertRule(capture(captured)) } returns Unit

        repository.muteApp(
            packageName = "com.test.app1",
            ruleType = MuteRuleType.HIDE,
            snoozeDurationMillis = null,
            autoDismiss = false
        )

        coVerify { mutedAppDao.upsertRule(any()) }
        assertEquals("com.test.app1", captured.captured.packageName)
        assertEquals(MuteRuleType.HIDE.name, captured.captured.ruleType)
    }

    @Test
    fun unmuteApp_deletesRuleInDao() = runTest(testDispatcher) {
        coEvery { mutedAppDao.deleteRule("com.test.app1") } returns Unit

        repository.unmuteApp("com.test.app1")

        coVerify { mutedAppDao.deleteRule("com.test.app1") }
    }
}
