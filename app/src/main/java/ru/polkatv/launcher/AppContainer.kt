package ru.polkatv.launcher

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import ru.polkatv.launcher.wallpaper.WallpaperController
import ru.polkatv.launcher.wallpaper.WallpaperLoader

class AppContainer(val appContext: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val wallpaperInterval = MutableStateFlow(1)   // minutes; Task 3 replaces this with the Prefs flow
    val wallpapers = WallpaperController(WallpaperLoader(appContext), wallpaperInterval, appScope)
}
