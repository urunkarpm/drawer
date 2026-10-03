package com.urunkarpm.drawer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.urunkarpm.drawer.core.datastore.DrawerPreferencesDataSource
import com.urunkarpm.drawer.core.designsystem.theme.DrawerTheme
import com.urunkarpm.drawer.feature.home.HomeScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesDataSource: DrawerPreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.urunkarpm.drawer.core.data.worker.WeatherRefreshWorker.enqueuePeriodic(this)
        setContent {
            val themeMode by preferencesDataSource.themeMode.collectAsStateWithLifecycle(initialValue = "SYSTEM")
            val dynamicColor by preferencesDataSource.dynamicColor.collectAsStateWithLifecycle(initialValue = true)

            val isSystemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "LIGHT" -> false
                "DARK", "AMOLED" -> true
                else -> isSystemDark
            }
            val isAmoled = themeMode == "AMOLED"

            DrawerTheme(
                darkTheme = isDark,
                isAmoled = isAmoled,
                dynamicColor = dynamicColor
            ) {
                HomeScreen()
            }
        }
    }
}
