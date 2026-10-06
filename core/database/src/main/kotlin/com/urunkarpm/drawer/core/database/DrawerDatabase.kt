package com.urunkarpm.drawer.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.urunkarpm.drawer.core.database.dao.AppGroupDao
import com.urunkarpm.drawer.core.database.dao.DockDao
import com.urunkarpm.drawer.core.database.dao.IconPackOverrideDao
import com.urunkarpm.drawer.core.database.dao.MutedAppDao
import com.urunkarpm.drawer.core.database.entity.AppGroupEntity
import com.urunkarpm.drawer.core.database.entity.AppGroupItemEntity
import com.urunkarpm.drawer.core.database.entity.DockItemEntity
import com.urunkarpm.drawer.core.database.entity.IconPackOverrideEntity
import com.urunkarpm.drawer.core.database.entity.MutedAppRuleEntity

@Database(
    entities = [
        AppGroupEntity::class,
        AppGroupItemEntity::class,
        DockItemEntity::class,
        MutedAppRuleEntity::class,
        IconPackOverrideEntity::class,
        com.urunkarpm.drawer.core.database.entity.WidgetItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class DrawerDatabase : RoomDatabase() {
    abstract fun appGroupDao(): AppGroupDao
    abstract fun dockDao(): DockDao
    abstract fun mutedAppDao(): MutedAppDao
    abstract fun iconPackOverrideDao(): IconPackOverrideDao
    abstract fun widgetDao(): com.urunkarpm.drawer.core.database.dao.WidgetDao
}
