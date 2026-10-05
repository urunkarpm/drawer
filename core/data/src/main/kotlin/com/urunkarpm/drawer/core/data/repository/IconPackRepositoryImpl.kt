package com.urunkarpm.drawer.core.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.content.res.ResourcesCompat
import com.urunkarpm.drawer.core.common.network.Dispatcher
import com.urunkarpm.drawer.core.common.network.DrawerDispatchers
import com.urunkarpm.drawer.core.database.dao.IconPackOverrideDao
import com.urunkarpm.drawer.core.database.entity.IconPackOverrideEntity
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.model.IconOverride
import com.urunkarpm.drawer.core.model.IconPackInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class IconPackRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesDataSource: DrawerPreferencesDataSource,
    private val iconPackOverrideDao: IconPackOverrideDao,
    @param:Dispatcher(DrawerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher
) : IconPackRepository {

    private val iconCache = LruCache<String, Drawable>(250)
    private val appFilterCache = ConcurrentHashMap<String, Map<String, String>>()
    private val overridesCache = ConcurrentHashMap<String, IconPackOverrideEntity>()
    @Volatile private var cachedActiveIconPack: String? = null
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    init {
        scope.launch {
            preferencesDataSource.activeIconPack.collect { pack ->
                cachedActiveIconPack = pack
            }
        }
        scope.launch {
            iconPackOverrideDao.getAllOverrides().collect { list ->
                overridesCache.clear()
                for (item in list) {
                    overridesCache[item.componentName] = item
                }
            }
        }
    }

    override val activeIconPack: Flow<String?> = preferencesDataSource.activeIconPack

    override val overrides: Flow<List<IconOverride>> = iconPackOverrideDao.getAllOverrides().map { list ->
        list.map { it.toDomain() }
    }.flowOn(ioDispatcher)

    override val installedIconPacks: Flow<List<IconPackInfo>> = flow {
        emit(getInstalledIconPacks())
    }.flowOn(ioDispatcher)

    override suspend fun getInstalledIconPacks(): List<IconPackInfo> = withContext(ioDispatcher) {
        val pm = context.packageManager
        val iconPackIntents = listOf(
            Intent("org.adw.launcher.THEMES"),
            Intent("com.novalauncher.THEME"),
            Intent("com.anddoes.launcher.THEME"),
            Intent("com.teslacoilsw.launcher.THEME"),
            Intent("com.fede.launcher.THEME_ICONPACK")
        )

        val foundPackages = mutableSetOf<String>()
        val result = mutableListOf<IconPackInfo>()

        // System default entry
        result.add(IconPackInfo(packageName = "", name = "System Default", isSystemDefault = true))

        for (intent in iconPackIntents) {
            val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            for (ri in resolved) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (foundPackages.add(pkg)) {
                    val label = try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        pkg
                    }
                    result.add(IconPackInfo(packageName = pkg, name = label))
                }
            }
        }
        result
    }

    override suspend fun setActiveIconPack(packageName: String?) = withContext(ioDispatcher) {
        preferencesDataSource.setActiveIconPack(if (packageName.isNullOrBlank()) null else packageName)
        iconCache.evictAll()
    }

    override suspend fun setAppOverride(
        componentName: String,
        iconPackPackage: String,
        drawableName: String
    ) = withContext(ioDispatcher) {
        iconPackOverrideDao.upsertOverride(
            IconPackOverrideEntity(
                componentName = componentName,
                iconPackPackageName = iconPackPackage,
                drawableName = drawableName
            )
        )
        iconCache.evictAll()
    }

    override suspend fun removeAppOverride(componentName: String) = withContext(ioDispatcher) {
        iconPackOverrideDao.deleteOverride(componentName)
        iconCache.evictAll()
    }

    override suspend fun parseAppFilter(iconPackPackage: String): Map<String, String> = withContext(ioDispatcher) {
        appFilterCache[iconPackPackage]?.let { return@withContext it }

        val map = mutableMapOf<String, String>()
        try {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(iconPackPackage)
            val xmlId = res.getIdentifier("appfilter", "xml", iconPackPackage)

            val inputStream: InputStream? = if (xmlId != 0) {
                res.openRawResource(xmlId)
            } else {
                try {
                    val iconPackContext = context.createPackageContext(iconPackPackage, 0)
                    iconPackContext.assets.open("appfilter.xml")
                } catch (_: Exception) {
                    null
                }
            }

            inputStream?.use { stream ->
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(stream, "UTF-8")

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                            // Extract "package/activity" from "ComponentInfo{package/activity}"
                            val cleanComp = component.removePrefix("ComponentInfo{").removeSuffix("}")
                            map[cleanComp] = drawable
                            val slashIdx = cleanComp.indexOf('/')
                            if (slashIdx > 0) {
                                val pkgOnly = cleanComp.substring(0, slashIdx)
                                map.putIfAbsent(pkgOnly, drawable)
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (_: Exception) {
            // Log and return parsed so far
        }
        appFilterCache[iconPackPackage] = map
        map
    }

    override suspend fun loadIcon(
        packageName: String,
        activityName: String,
        fallback: suspend () -> Drawable?
    ): Drawable? = withContext(ioDispatcher) {
        val compKey = "$packageName/$activityName"
        val activePack = cachedActiveIconPack ?: preferencesDataSource.activeIconPack.first()

        // 1. Check override in memory (eliminates per-icon SQLite disk queries)
        val override = overridesCache[compKey]
        if (override != null) {
            val cacheKey = "override_${override.iconPackPackageName}_${override.drawableName}"
            iconCache.get(cacheKey)?.let { return@withContext it }

            val drawable = loadDrawableFromPack(override.iconPackPackageName, override.drawableName)
            if (drawable != null) {
                iconCache.put(cacheKey, drawable)
                return@withContext drawable
            }
        }

        // 2. Check active icon pack
        if (!activePack.isNullOrBlank()) {
            val cacheKey = "${activePack}_${compKey}"
            iconCache.get(cacheKey)?.let { return@withContext it }

            val appFilter = parseAppFilter(activePack)
            val drawableName = appFilter[compKey] ?: appFilter[packageName]
            if (drawableName != null) {
                val drawable = loadDrawableFromPack(activePack, drawableName)
                if (drawable != null) {
                    iconCache.put(cacheKey, drawable)
                    return@withContext drawable
                }
            }
        }

        // 3. Fallback to default launcher icon
        fallback()
    }

    private fun loadDrawableFromPack(packPackage: String, drawableName: String): Drawable? {
        return try {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(packPackage)
            val resId = res.getIdentifier(drawableName, "drawable", packPackage)
            if (resId != 0) {
                ResourcesCompat.getDrawable(res, resId, null)
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
