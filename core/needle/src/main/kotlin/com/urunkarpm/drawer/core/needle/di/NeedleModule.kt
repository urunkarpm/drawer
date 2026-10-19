package com.urunkarpm.drawer.core.needle.di

import com.urunkarpm.drawer.core.needle.NeedleEngine
import com.urunkarpm.drawer.core.needle.engine.CactusNeedleEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NeedleModule {
    @Binds
    @Singleton
    abstract fun bindNeedleEngine(impl: CactusNeedleEngine): NeedleEngine
}
