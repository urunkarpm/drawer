package com.urunkarpm.drawer.core.common.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawerWidgetHostManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(context, APPWIDGET_HOST_ID)

    fun startListening() {
        try {
            appWidgetHost.startListening()
        } catch (_: Exception) {}
    }

    fun stopListening() {
        try {
            appWidgetHost.stopListening()
        } catch (_: Exception) {}
    }

    fun allocateAppWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun deleteAppWidgetId(appWidgetId: Int) {
        try {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        } catch (_: Exception) {}
    }

    fun createView(context: Context, appWidgetId: Int, appWidgetInfo: AppWidgetProviderInfo?): AppWidgetHostView {
        val info = appWidgetInfo ?: appWidgetManager.getAppWidgetInfo(appWidgetId)
        val hostView = appWidgetHost.createView(context, appWidgetId, info)
        hostView.setAppWidget(appWidgetId, info)
        return hostView
    }

    companion object {
        const val APPWIDGET_HOST_ID = 2026
        const val REQUEST_PICK_APPWIDGET = 2027
        const val REQUEST_BIND_APPWIDGET = 2028
        const val REQUEST_CONFIGURE_APPWIDGET = 2029
    }
}
