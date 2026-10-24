package ru.polkatv.launcher

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.polkatv.launcher.apps.AppsRepository
import ru.polkatv.launcher.apps.TileArtLoader
import ru.polkatv.launcher.settings.Prefs
import ru.polkatv.launcher.settings.SettingsRepository
import ru.polkatv.launcher.settings.polkaDataStore
import ru.polkatv.launcher.system.SystemActions
import ru.polkatv.launcher.wallpaper.WallpaperController
import ru.polkatv.launcher.wallpaper.WallpaperLoader

class AppContainer(val appContext: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings = SettingsRepository(appContext.polkaDataStore)
    val prefs: StateFlow<Prefs> = settings.prefs.stateIn(appScope, SharingStarted.Eagerly, Prefs())
    val wallpaperInterval: StateFlow<Int> =
        prefs.map { it.wallpaperIntervalMinutes }.stateIn(appScope, SharingStarted.Eagerly, 5)
    val wallpapers = WallpaperController(WallpaperLoader(appContext), wallpaperInterval, appScope)
    val apps = AppsRepository(appContext, appScope)
    val tileArt = TileArtLoader(appContext)
    val system = SystemActions(appContext)
}
