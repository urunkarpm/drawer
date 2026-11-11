package com.urunkarpm.drawer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.urunkarpm.drawer.core.database.dao.AppGroupDao
import com.urunkarpm.drawer.core.database.dao.DockDao
import com.urunkarpm.drawer.core.database.dao.EventLogDao
import com.urunkarpm.drawer.core.database.dao.IconPackOverrideDao
import com.urunkarpm.drawer.core.database.dao.MutedAppDao
import com.urunkarpm.drawer.core.database.dao.RecentSearchDao
import com.urunkarpm.drawer.core.database.dao.SavedPlaceDao
import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.database.dao.WidgetDao
import com.urunkarpm.drawer.core.database.entity.AppGroupEntity
import com.urunkarpm.drawer.core.database.entity.AppGroupItemEntity
import com.urunkarpm.drawer.core.database.entity.DockItemEntity
import com.urunkarpm.drawer.core.database.entity.EventLogEntity
import com.urunkarpm.drawer.core.database.entity.IconPackOverrideEntity
import com.urunkarpm.drawer.core.database.entity.MutedAppRuleEntity
import com.urunkarpm.drawer.core.database.entity.RecentSearchEntity
import com.urunkarpm.drawer.core.database.entity.SavedPlaceEntity
import com.urunkarpm.drawer.core.database.entity.TodoEntity
import com.urunkarpm.drawer.core.database.entity.WidgetDrawerEntity
import com.urunkarpm.drawer.core.database.entity.WidgetItemEntity

@Database(
    entities = [
        AppGroupEntity::class,
        AppGroupItemEntity::class,
        DockItemEntity::class,
        MutedAppRuleEntity::class,
        IconPackOverrideEntity::class,
        WidgetDrawerEntity::class,
        WidgetItemEntity::class,
        TodoEntity::class,
        SavedPlaceEntity::class,
        RecentSearchEntity::class,
        EventLogEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class DrawerDatabase : RoomDatabase() {
    abstract fun appGroupDao(): AppGroupDao
    abstract fun dockDao(): DockDao
    abstract fun mutedAppDao(): MutedAppDao
    abstract fun iconPackOverrideDao(): IconPackOverrideDao
    abstract fun widgetDao(): WidgetDao

    abstract fun todoDao(): TodoDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun eventLogDao(): EventLogDao
}
