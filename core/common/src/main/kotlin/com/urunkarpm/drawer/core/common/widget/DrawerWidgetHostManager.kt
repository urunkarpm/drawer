package com.urunkarpm.drawer.core.common.widget

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.UserManager
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledWidgetInfo(
    val providerInfo: AppWidgetProviderInfo,
    val label: String,
    val description: String?,
    val spanX: Int,
    val spanY: Int,
    val minWidthDp: Int,
    val minHeightDp: Int
)

data class AppWidgetGroup(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable?,
    val widgets: List<InstalledWidgetInfo>
)

open class DrawerAppWidgetHost(
    context: Context,
    hostId: Int
) : AppWidgetHost(context, hostId) {
    override fun allocateAppWidgetId(): Int {
        return super.allocateAppWidgetId()
    }

    override fun deleteAppWidgetId(appWidgetId: Int) {
        super.deleteAppWidgetId(appWidgetId)
    }

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
open class DrawerWidgetHostManager @Inject constructor(
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

    open fun allocateAppWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    open fun deleteAppWidgetId(appWidgetId: Int) {
        viewCache.remove(appWidgetId)
        try {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        } catch (_: Exception) {}
    }

    fun clearViewCache() {
        viewCache.clear()
    }

    // ponytail: re-binds restored or dangling widget entities to live AppWidgetHost allocations so widgets survive cross-device backups and re-installs.
    fun rebindOrRestoreWidget(
        packageName: String,
        providerClassName: String,
        existingAppWidgetId: Int,
        spanX: Int = 4,
        heightDp: Int = 180
    ): Int {
        val targetComponent = ComponentName(packageName, providerClassName)

        // 1. Check if existingAppWidgetId is currently valid and still bound to this provider
        val existingInfo = try {
            appWidgetManager.getAppWidgetInfo(existingAppWidgetId)
        } catch (_: Exception) {
            null
        }
        if (existingInfo != null &&
            (existingInfo.provider == targetComponent ||
             (existingInfo.provider?.packageName == packageName && existingInfo.provider?.className == providerClassName))
        ) {
            triggerWidgetUpdate(existingAppWidgetId)
            return existingAppWidgetId
        }

        // 2. Discover matching provider installed on this device
        val allProviders = try {
            val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
            val profileProviders = userManager?.userProfiles?.flatMap { profile ->
                try {
                    appWidgetManager.getInstalledProvidersForProfile(profile) ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
            }?.distinctBy { it.provider }

            if (!profileProviders.isNullOrEmpty()) {
                profileProviders
            } else {
                appWidgetManager.installedProviders
            }
        } catch (_: Exception) {
            try {
                appWidgetManager.installedProviders
            } catch (_: Exception) {
                emptyList()
            }
        }

        val matchedProvider = allProviders.find {
            it.provider.packageName == packageName && it.provider.className == providerClassName
        } ?: allProviders.find {
            it.provider.packageName == packageName
        }

        if (matchedProvider == null) {
            return existingAppWidgetId
        }

        // 3. Allocate a fresh host ID for this session
        val newAppWidgetId = try {
            allocateAppWidgetId()
        } catch (_: Exception) {
            return existingAppWidgetId
        }

        // 4. Configure layout options bundle
        val widthDp = if (spanX <= 2) 160 else 340
        val options = createWidgetOptionsBundle(widthDp, heightDp)

        // 5. Attempt automatic binding
        val bound = try {
            appWidgetManager.bindAppWidgetIdIfAllowed(newAppWidgetId, matchedProvider.provider, options)
        } catch (_: Exception) {
            false
        }

        if (bound) {
            triggerWidgetUpdate(newAppWidgetId)
            return newAppWidgetId
        } else {
            try {
                deleteAppWidgetId(newAppWidgetId)
            } catch (_: Exception) {}
            return existingAppWidgetId
        }
    }

    // ponytail: builds a production-grade sizing bundle with responsive SizeF constraints for Android 12+ and min/max bounds for older APIs.
    fun createWidgetOptionsBundle(widthDp: Int, heightDp: Int): Bundle {
        return Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                putParcelableArrayList(
                    AppWidgetManager.OPTION_APPWIDGET_SIZES,
                    arrayListOf(SizeF(widthDp.toFloat(), heightDp.toFloat()))
                )
            }
        }
    }

    // ponytail: triggers an explicit update broadcast to the widget provider component to guarantee immediate layout generation even under background execution limits.
    fun triggerWidgetUpdate(appWidgetId: Int) {
        try {
            val info = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return
            val updateIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                component = info.provider
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
            }
            context.sendBroadcast(updateIntent)
        } catch (_: Exception) {}
    }

    // ponytail: caches views in memory with strict MATCH_PARENT layout parameters and re-bound provider metadata so child RemoteViews never measure to 0x0.
    fun getOrCreateView(context: Context, appWidgetId: Int): AppWidgetHostView {
        val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
        val hostView = viewCache.getOrPut(appWidgetId) {
            appWidgetHost.createView(context, appWidgetId, info)
        }
        (hostView.parent as? ViewGroup)?.removeView(hostView)
        hostView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        if (info != null) {
            try {
                hostView.setAppWidget(appWidgetId, info)
            } catch (_: Exception) {}
        }
        if (hostView.childCount == 0) {
            triggerWidgetUpdate(appWidgetId)
        }
        return hostView
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

    // ponytail: queries all installed widget providers across personal and work profiles directly from AppWidgetManager, bypassing deprecated OEM pickers.
    fun getInstalledWidgetProviders(packageManager: PackageManager): List<AppWidgetGroup> {
        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
        val rawProviders: List<AppWidgetProviderInfo> = if (userManager != null) {
            try {
                val profiles = userManager.userProfiles
                profiles.flatMap { profile ->
                    try {
                        appWidgetManager.getInstalledProvidersForProfile(profile) ?: emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }
                }.distinctBy { it.provider }
            } catch (_: Exception) {
                appWidgetManager.installedProviders
            }
        } else {
            appWidgetManager.installedProviders
        }

        val grouped = rawProviders.groupBy { it.provider.packageName }
        val result = mutableListOf<AppWidgetGroup>()

        for ((pkg, providers) in grouped) {
            val appInfo = try {
                packageManager.getApplicationInfo(pkg, 0)
            } catch (_: Exception) {
                null
            }
            val appName = appInfo?.let { packageManager.getApplicationLabel(it).toString() } ?: pkg
            val appIcon = appInfo?.let { packageManager.getApplicationIcon(it) }

            val widgets = providers.map { info ->
                val label = try {
                    val rawLabel = info.loadLabel(packageManager)
                    if (rawLabel.isNullOrBlank()) appName else rawLabel
                } catch (_: Exception) {
                    appName
                }

                val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        info.loadDescription(context)?.toString()
                    } catch (_: Exception) {
                        null
                    }
                } else null

                val spanX = if (info.minWidth in 1..160) 2 else 4
                val spanY = ((info.minHeight + 69) / 70).coerceIn(2, 4)

                InstalledWidgetInfo(
                    providerInfo = info,
                    label = label,
                    description = description,
                    spanX = spanX,
                    spanY = spanY,
                    minWidthDp = info.minWidth,
                    minHeightDp = info.minHeight
                )
            }.sortedBy { it.label }

            result.add(
                AppWidgetGroup(
                    packageName = pkg,
                    appName = appName,
                    appIcon = appIcon,
                    widgets = widgets
                )
            )
        }

        return result.sortedBy { it.appName.lowercase() }
    }

    companion object {
        const val APPWIDGET_HOST_ID = 2026
        const val REQUEST_PICK_APPWIDGET = 2027
        const val REQUEST_BIND_APPWIDGET = 2028
        const val REQUEST_CONFIGURE_APPWIDGET = 2029
    }
}
