package com.urunkarpm.drawer.core.database.di

import android.content.Context
import androidx.room.Room
import com.urunkarpm.drawer.core.database.DrawerDatabase
import com.urunkarpm.drawer.core.database.dao.AppGroupDao
import com.urunkarpm.drawer.core.database.dao.DockDao
import com.urunkarpm.drawer.core.database.dao.IconPackOverrideDao
import com.urunkarpm.drawer.core.database.dao.MutedAppDao
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
        ).fallbackToDestructiveMigration().build()
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
    fun provideWidgetDao(db: DrawerDatabase): com.urunkarpm.drawer.core.database.dao.WidgetDao = db.widgetDao()
}
