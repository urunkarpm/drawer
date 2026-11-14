package com.urunkarpm.drawer.feature.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UnlockReceiver(
    private val onUnlock: () -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_USER_PRESENT) {
            onUnlock()
        }
    }
}
