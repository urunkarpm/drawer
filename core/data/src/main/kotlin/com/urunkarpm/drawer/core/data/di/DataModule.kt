package com.urunkarpm.drawer.core.data.di

import com.urunkarpm.drawer.core.data.repository.AppGroupRepository
import com.urunkarpm.drawer.core.data.repository.AppGroupRepositoryImpl
import com.urunkarpm.drawer.core.data.repository.AppRepository
import com.urunkarpm.drawer.core.data.repository.AppRepositoryImpl
import com.urunkarpm.drawer.core.data.repository.DockRepository
import com.urunkarpm.drawer.core.data.repository.DockRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindAppRepository(
        impl: AppRepositoryImpl
    ): AppRepository

    @Binds
    @Singleton
    abstract fun bindDockRepository(
        impl: DockRepositoryImpl
    ): DockRepository

    @Binds
    @Singleton
    abstract fun bindAppGroupRepository(
        impl: AppGroupRepositoryImpl
    ): AppGroupRepository

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        impl: com.urunkarpm.drawer.core.data.repository.WeatherRepositoryImpl
    ): com.urunkarpm.drawer.core.data.repository.WeatherRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        impl: com.urunkarpm.drawer.core.data.repository.NotificationRepositoryImpl
    ): com.urunkarpm.drawer.core.data.repository.NotificationRepository

    @Binds
    @Singleton
    abstract fun bindIconPackRepository(
        impl: com.urunkarpm.drawer.core.data.repository.IconPackRepositoryImpl
    ): com.urunkarpm.drawer.core.data.repository.IconPackRepository
}
