package com.urunkarpm.drawer.core.data.reconcile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.urunkarpm.drawer.core.data.worker.TodoWatchdogWorker
import com.urunkarpm.drawer.core.data.worker.TodoWorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Broadcast receiver triggered on system boot, app update, time changes,
 * or exact alarm permission state changes to reconcile all todo alarms and background checks.
 */
class TodoBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    TodoWorkerEntryPoint::class.java
                )
                entryPoint.todoReconciler().reconcile()
                TodoWatchdogWorker.enqueuePeriodic(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
