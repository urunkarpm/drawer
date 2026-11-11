package com.urunkarpm.drawer.core.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.urunkarpm.drawer.core.data.repository.TodoRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StaleAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var staleTaskChecker: StaleTaskChecker

    @Inject
    lateinit var staleAlarmScheduler: StaleAlarmScheduler

    @Inject
    lateinit var todoRepository: TodoRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = todoRepository.settings.first()
                when (action) {
                    StaleAlarmScheduler.ACTION_DAILY_STALE_CHECK -> {
                        staleTaskChecker.runDailyCheck()
                        staleAlarmScheduler.scheduleDailyCheck(settings)
                    }
                    StaleAlarmScheduler.ACTION_HIGH_PRIORITY_CHECK -> {
                        val dispatched = staleTaskChecker.runHighPriorityDaytimeCheck()
                        if (dispatched > 0) {
                            staleAlarmScheduler.scheduleHighPriorityDaytimeCheck(4)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
