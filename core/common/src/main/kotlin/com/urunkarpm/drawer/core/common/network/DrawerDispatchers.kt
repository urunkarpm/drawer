package com.urunkarpm.drawer.core.common.network

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val drawerDispatcher: DrawerDispatchers)

enum class DrawerDispatchers {
    Default,
    IO
}
