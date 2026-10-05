package com.urunkarpm.drawer.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

// ponytail: AccessibilityService performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) locks screen without triggering PIN lock (preserves fingerprint/face unlock); ceiling: Android 9+ only; upgrade path: DevicePolicyManager fallback for legacy.
class DrawerAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No event listening required
    }

    override fun onInterrupt() {
        // No interrupt action required
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    companion object {
        private var instance: WeakReference<DrawerAccessibilityService>? = null

        fun isEnabled(): Boolean = instance?.get() != null

        fun lockScreen(): Boolean {
            val service = instance?.get() ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                false
            }
        }
    }
}
