package com.urunkarpm.drawer.core.data.di

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
}
