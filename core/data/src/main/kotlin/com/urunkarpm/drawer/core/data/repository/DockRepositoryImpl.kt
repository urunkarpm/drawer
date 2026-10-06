package com.urunkarpm.drawer.core.data.repository

import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.database.dao.DockDao
import com.urunkarpm.drawer.core.database.entity.DockItemEntity
import com.urunkarpm.drawer.core.model.AppInfo
import com.urunkarpm.drawer.core.model.DockItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class DockRepositoryImpl @Inject constructor(
    private val dockDao: DockDao,
    @param:Dispatcher(DrawerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @param:ApplicationContext private val context: Context? = null,
    private val appRepositoryProvider: Provider<AppRepository>? = null,
    private val preferencesDataSourceProvider: Provider<DrawerPreferencesDataSource>? = null
) : DockRepository {

    private var hasSeedAttempted = false

    override val dockItems: Flow<List<DockItem>> = dockDao.getDockItems()
        .onStart { ensureSeeded() }
        .map { list -> list.map { it.toDomain() } }

    override suspend fun addToDock(app: AppInfo): Boolean = withContext(ioDispatcher) {
        val currentItems = dockDao.getDockItems().first()
        if (currentItems.size >= 5) {
            return@withContext false
        }

        // Avoid adding the exact same app twice to the dock
        if (currentItems.any { it.packageName == app.packageName && it.activityName == app.activityName }) {
            return@withContext false
        }

        val nextPosition = currentItems.size
        dockDao.insertDockItem(
            DockItemEntity(
                position = nextPosition,
                packageName = app.packageName,
                activityName = app.activityName,
                userHandleId = app.userHandleId,
                customLabel = app.label
            )
        )
        true
    }

    override suspend fun removeFromDock(position: Int) = withContext(ioDispatcher) {
        val currentItems = dockDao.getDockItems().first().toMutableList()
        currentItems.removeAll { it.position == position }

        // Re-index remaining items so positions are contiguous 0..(size-1)
        val reindexed = currentItems.sortedBy { it.position }.mapIndexed { index, entity ->
            entity.copy(position = index)
        }
        dockDao.replaceDock(reindexed)
    }

    override suspend fun reorderDock(fromPosition: Int, toPosition: Int) = withContext(ioDispatcher) {
        val currentItems = dockDao.getDockItems().first().sortedBy { it.position }.toMutableList()
        val fromIndex = currentItems.indexOfFirst { it.position == fromPosition }
        val toIndex = currentItems.indexOfFirst { it.position == toPosition }
        if (fromIndex == -1 || toIndex == -1 || fromIndex == toIndex) {
            return@withContext
        }

        val itemToMove = currentItems.removeAt(fromIndex)
        currentItems.add(toIndex, itemToMove)

        val reindexed = currentItems.mapIndexed { index, entity ->
            entity.copy(position = index)
        }
        dockDao.replaceDock(reindexed)
    }

    override suspend fun clearDock() = withContext(ioDispatcher) {
        dockDao.clearDock()
    }

    private suspend fun ensureSeeded() {
        if (hasSeedAttempted) return
        val prefDs = preferencesDataSourceProvider?.get()
        val alreadySeeded = prefDs?.hasSeededDefaultDock?.first() ?: false
        val currentItems = dockDao.getDockItems().first()
        if (alreadySeeded && currentItems.isNotEmpty()) {
            hasSeedAttempted = true
            return
        }

        val appRepo = appRepositoryProvider?.get() ?: return
        val installed = appRepo.installedApps.first { it.isNotEmpty() }
        val defaultApps = resolveDefaultDockApps(installed)
        if (defaultApps.isNotEmpty()) {
            val entities = defaultApps.mapIndexed { index, app ->
                DockItemEntity(
                    position = index,
                    packageName = app.packageName,
                    activityName = app.activityName,
                    userHandleId = app.userHandleId,
                    customLabel = app.label
                )
            }
            dockDao.replaceDock(entities)
            prefDs?.setHasSeededDefaultDock(true)
            hasSeedAttempted = true
        }
    }

    private fun resolveDefaultDockApps(installedApps: List<AppInfo>): List<AppInfo> {
        val resolved = mutableListOf<AppInfo>()
        val usedKeys = mutableSetOf<String>()

        fun addIfDistinct(app: AppInfo?): Boolean {
            if (app == null) return false
            val key = "${app.packageName}/${app.activityName}"
            if (usedKeys.add(key)) {
                resolved.add(app)
                return true
            }
            return false
        }

        val pm = context?.packageManager

        // 1. Default Calling App (Dialer)
        val dialerApp: AppInfo? = run {
            val intent = Intent(Intent.ACTION_DIAL)
            val res = pm?.resolveActivity(intent, 0)
            val resPkg = res?.activityInfo?.packageName
            val resAct = res?.activityInfo?.name
            if (!resPkg.isNullOrBlank() && resPkg != "android" && !resPkg.contains("resolver", ignoreCase = true)) {
                installedApps.firstOrNull {
                    it.packageName == resPkg && (it.activityName == resAct || it.label.equals("Phone", ignoreCase = true) || it.label.equals("Dialer", ignoreCase = true) || it.activityName.contains("dialer", ignoreCase = true))
                } ?: installedApps.firstOrNull { it.packageName == resPkg }
            } else null
        } ?: run {
            val defaultDialerPkg = (context?.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.defaultDialerPackage
            if (!defaultDialerPkg.isNullOrBlank()) {
                installedApps.firstOrNull {
                    it.packageName == defaultDialerPkg && (it.label.equals("Phone", ignoreCase = true) || it.label.equals("Dialer", ignoreCase = true) || it.activityName.contains("dialer", ignoreCase = true))
                } ?: installedApps.firstOrNull { it.packageName == defaultDialerPkg }
            } else null
        } ?: run {
            installedApps.firstOrNull { app ->
                val l = app.label.lowercase()
                l == "phone" || l == "dialer" || l == "calls"
            }
        } ?: run {
            val knownDialerPkgs = listOf(
                "com.google.android.dialer",
                "com.android.dialer",
                "com.samsung.android.dialer",
                "com.android.contacts",
                "com.asus.contacts"
            )
            installedApps.firstOrNull {
                it.packageName in knownDialerPkgs && (it.label.equals("Phone", ignoreCase = true) || it.activityName.contains("dialer", ignoreCase = true))
            } ?: installedApps.firstOrNull { it.packageName in knownDialerPkgs }
        }
        addIfDistinct(dialerApp)

        // 2. Default Browser
        val browserApp: AppInfo? = run {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"))
            val res = pm?.resolveActivity(intent, 0)
            val resPkg = res?.activityInfo?.packageName
            if (!resPkg.isNullOrBlank() && resPkg != "android" && !resPkg.contains("resolver", ignoreCase = true)) {
                installedApps.firstOrNull { it.packageName == resPkg }
            } else null
        } ?: run {
            val knownBrowserPkgs = listOf(
                "com.android.chrome",
                "com.brave.browser",
                "org.mozilla.firefox",
                "com.sec.android.app.sbrowser",
                "com.microsoft.emmx",
                "com.opera.browser"
            )
            installedApps.firstOrNull { it.packageName in knownBrowserPkgs }
        } ?: run {
            installedApps.firstOrNull { app ->
                val l = app.label.lowercase()
                l.contains("browser") || l.contains("chrome") || l.contains("brave")
            }
        }
        addIfDistinct(browserApp)

        // 3. Phone Settings Button
        val settingsApp: AppInfo? = run {
            installedApps.firstOrNull { it.packageName == "com.android.settings" }
        } ?: run {
            val intent = Intent(Settings.ACTION_SETTINGS)
            val res = pm?.resolveActivity(intent, 0)
            val resPkg = res?.activityInfo?.packageName
            if (!resPkg.isNullOrBlank()) {
                installedApps.firstOrNull { it.packageName == resPkg }
            } else null
        } ?: run {
            installedApps.firstOrNull { it.label.equals("Settings", ignoreCase = true) }
        }
        addIfDistinct(settingsApp)

        // 4. Camera
        val cameraApp: AppInfo? = run {
            val intent = Intent("android.media.action.STILL_IMAGE_CAMERA")
            val res = pm?.resolveActivity(intent, 0)
            val resPkg = res?.activityInfo?.packageName
            if (!resPkg.isNullOrBlank() && resPkg != "android" && !resPkg.contains("resolver", ignoreCase = true)) {
                installedApps.firstOrNull { it.packageName == resPkg }
            } else null
        } ?: run {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val res = pm?.resolveActivity(intent, 0)
            val resPkg = res?.activityInfo?.packageName
            if (!resPkg.isNullOrBlank() && resPkg != "android" && !resPkg.contains("resolver", ignoreCase = true)) {
                installedApps.firstOrNull { it.packageName == resPkg }
            } else null
        } ?: run {
            val knownCameraPkgs = listOf(
                "com.android.camera",
                "com.google.android.GoogleCamera",
                "com.sec.android.app.camera",
                "com.motorola.camera"
            )
            installedApps.firstOrNull { it.packageName in knownCameraPkgs }
        } ?: run {
            installedApps.firstOrNull { it.label.equals("Camera", ignoreCase = true) }
        }
        addIfDistinct(cameraApp)

        // 5. Default Messages App
        val messagesApp: AppInfo? = run {
            val defaultSmsPkg = Telephony.Sms.getDefaultSmsPackage(context)
            if (!defaultSmsPkg.isNullOrBlank()) {
                installedApps.firstOrNull { it.packageName == defaultSmsPkg }
            } else null
        } ?: run {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))
            val res = pm?.resolveActivity(intent, 0)
            val resPkg = res?.activityInfo?.packageName
            if (!resPkg.isNullOrBlank() && resPkg != "android" && !resPkg.contains("resolver", ignoreCase = true)) {
                installedApps.firstOrNull { it.packageName == resPkg }
            } else null
        } ?: run {
            val knownMsgPkgs = listOf(
                "com.google.android.apps.messaging",
                "com.android.mms",
                "com.samsung.android.messaging"
            )
            installedApps.firstOrNull { it.packageName in knownMsgPkgs }
        } ?: run {
            installedApps.firstOrNull { app ->
                val l = app.label.lowercase()
                l == "messages" || l == "messaging" || l == "sms"
            }
        }
        addIfDistinct(messagesApp)

        return resolved.take(5)
    }
}
