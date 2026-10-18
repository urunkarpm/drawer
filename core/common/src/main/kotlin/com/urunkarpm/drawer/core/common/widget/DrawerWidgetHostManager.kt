package com.urunkarpm.drawer.core.common.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

open class DrawerAppWidgetHost(
    context: Context,
    hostId: Int
) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView {
        return DrawerAppWidgetHostView(context)
    }
}

class DrawerAppWidgetHostView(context: Context) : AppWidgetHostView(context) {
    override fun getErrorView(): View {
        return TextView(context).apply {
            text = "Widget error"
            setPadding(24, 24, 24, 24)
        }
    }
}

@Singleton
class DrawerWidgetHostManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    val appWidgetHost: AppWidgetHost = DrawerAppWidgetHost(context, APPWIDGET_HOST_ID)

    private val viewCache = mutableMapOf<Int, AppWidgetHostView>()

    var onWidgetConfiguredListener: ((resultCode: Int, data: Intent?) -> Unit)? = null

    fun onWidgetConfigured(resultCode: Int, data: Intent?) {
        val listener = onWidgetConfiguredListener
        onWidgetConfiguredListener = null
        listener?.invoke(resultCode, data)
    }

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
        viewCache.remove(appWidgetId)
        try {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        } catch (_: Exception) {}
    }

    // ponytail: caches views in memory so LazyColumn reuse doesn't discard inflated RemoteViews; ceiling: in-memory cache; upgrade path: LRU cache if active widget count exceeds 50.
    fun getOrCreateView(context: Context, appWidgetId: Int): AppWidgetHostView {
        return viewCache.getOrPut(appWidgetId) {
            val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
            appWidgetHost.createView(context, appWidgetId, info)
        }.also { hostView ->
            (hostView.parent as? ViewGroup)?.removeView(hostView)
        }
    }

    fun createView(context: Context, appWidgetId: Int, appWidgetInfo: AppWidgetProviderInfo? = null): AppWidgetHostView {
        return getOrCreateView(context, appWidgetId)
    }

    fun startConfigure(activity: Activity, appWidgetId: Int): Boolean {
        return try {
            appWidgetHost.startAppWidgetConfigureActivityForResult(
                activity,
                appWidgetId,
                0,
                REQUEST_CONFIGURE_APPWIDGET,
                null
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val APPWIDGET_HOST_ID = 2026
        const val REQUEST_PICK_APPWIDGET = 2027
        const val REQUEST_BIND_APPWIDGET = 2028
        const val REQUEST_CONFIGURE_APPWIDGET = 2029
    }
}
