package com.urunkarpm.drawer.feature.notifications

import app.cash.turbine.test
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.data.repository.NotificationRepository
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.MuteRuleType
import com.urunkarpm.drawer.core.model.MutedAppRule
import com.urunkarpm.drawer.core.model.NotificationItem
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val notificationRepository: NotificationRepository = mockk(relaxed = true)
    private val appRepository: AppRepository = mockk(relaxed = true)
    private val preferencesDataSource: DrawerPreferencesDataSource = mockk(relaxed = true)

    private val notificationsFlow = MutableStateFlow<List<NotificationItem>>(emptyList())
    private val rulesFlow = MutableStateFlow<List<MutedAppRule>>(emptyList())
    private val privacyModeFlow = MutableStateFlow(false)

    private lateinit var viewModel: NotificationsViewModel

    private val notificationA1 = NotificationItem(
        key = "k1",
        packageName = "com.chat",
        appName = "Chat",
        title = "Alice",
        text = "Hi",
        postTimeMillis = 1000L
    )

    private val notificationA2 = NotificationItem(
        key = "k2",
        packageName = "com.chat",
        appName = "Chat",
        title = "Bob",
        text = "Hey",
        postTimeMillis = 2000L
    )

    private val notificationB1 = NotificationItem(
        key = "k3",
        packageName = "com.mail",
        appName = "Mail",
        title = "Invoice",
        text = "Your receipt",
        postTimeMillis = 1500L
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { notificationRepository.activeNotifications } returns notificationsFlow
        every { notificationRepository.mutedRules } returns rulesFlow
        every { notificationRepository.checkPermission() } returns true
        every { preferencesDataSource.notificationsPrivacyMode } returns privacyModeFlow

        notificationsFlow.value = listOf(notificationA1, notificationA2, notificationB1)

        viewModel = NotificationsViewModel(notificationRepository, appRepository, preferencesDataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_groupsNotificationsByApp() = runTest {
        viewModel.uiState.test {
            awaitItem()
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(3, state.activeNotifications.size)
            assertEquals(2, state.groupedNotifications.size)

            val chatGroup = state.groupedNotifications.find { it.packageName == "com.chat" }
            assertEquals(2, chatGroup?.items?.size)
            assertEquals("com.chat", chatGroup?.packageName)
        }
    }

    @Test
    fun dismissNotification_delegatesToRepository() = runTest {
        viewModel.dismissNotification("k1")
        verify { notificationRepository.dismissNotification("k1") }
    }

    @Test
    fun clearAllNotifications_delegatesToRepository() = runTest {
        viewModel.clearAllNotifications()
        verify { notificationRepository.clearAllNotifications() }
    }

    @Test
    fun muteApp_delegatesToRepository() = runTest {
        viewModel.muteApp("com.chat", MuteRuleType.HIDE)
        testScheduler.advanceUntilIdle()

        coVerify { notificationRepository.muteApp("com.chat", MuteRuleType.HIDE, null, false) }
    }

    @Test
    fun unmuteApp_delegatesToRepository() = runTest {
        viewModel.unmuteApp("com.chat")
        testScheduler.advanceUntilIdle()

        coVerify { notificationRepository.unmuteApp("com.chat") }
    }

    @Test
    fun togglePrivacyMode_updatesPreferences() = runTest {
        privacyModeFlow.value = false
        viewModel.togglePrivacyMode()
        testScheduler.advanceUntilIdle()

        coVerify { preferencesDataSource.setNotificationsPrivacyMode(true) }
    }
}
