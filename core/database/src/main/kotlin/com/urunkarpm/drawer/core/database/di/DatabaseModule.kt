package com.urunkarpm.drawer.core.database.di

import android.content.Context
import androidx.room.Room
import com.urunkarpm.drawer.core.database.DrawerDatabase
import com.urunkarpm.drawer.core.database.MIGRATION_4_5
import com.urunkarpm.drawer.core.database.dao.AppGroupDao
import com.urunkarpm.drawer.core.database.dao.DockDao
import com.urunkarpm.drawer.core.database.dao.EventLogDao
import com.urunkarpm.drawer.core.database.dao.IconPackOverrideDao
import com.urunkarpm.drawer.core.database.dao.MutedAppDao
import com.urunkarpm.drawer.core.database.dao.RecentSearchDao
import com.urunkarpm.drawer.core.database.dao.SavedPlaceDao
import com.urunkarpm.drawer.core.database.dao.TodoDao
import com.urunkarpm.drawer.core.database.dao.WidgetDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDrawerDatabase(
        @ApplicationContext context: Context
    ): DrawerDatabase {
        return Room.databaseBuilder(
            context,
            DrawerDatabase::class.java,
            "drawer.db"
        )
            .addMigrations(MIGRATION_4_5)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAppGroupDao(db: DrawerDatabase): AppGroupDao = db.appGroupDao()

    @Provides
    fun provideDockDao(db: DrawerDatabase): DockDao = db.dockDao()

    @Provides
    fun provideMutedAppDao(db: DrawerDatabase): MutedAppDao = db.mutedAppDao()

    @Provides
    fun provideIconPackOverrideDao(db: DrawerDatabase): IconPackOverrideDao = db.iconPackOverrideDao()

    @Provides
    fun provideWidgetDao(db: DrawerDatabase): WidgetDao = db.widgetDao()

    @Provides
    fun provideTodoDao(db: DrawerDatabase): TodoDao = db.todoDao()

    @Provides
    fun provideSavedPlaceDao(db: DrawerDatabase): SavedPlaceDao = db.savedPlaceDao()

    @Provides
    fun provideRecentSearchDao(db: DrawerDatabase): RecentSearchDao = db.recentSearchDao()

    @Provides
    fun provideEventLogDao(db: DrawerDatabase): EventLogDao = db.eventLogDao()
}
