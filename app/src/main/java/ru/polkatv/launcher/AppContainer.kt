package ru.polkatv.launcher

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import okhttp3.OkHttpClient
import ru.polkatv.launcher.apps.AppsRepository
import ru.polkatv.launcher.apps.TileArtLoader
import ru.polkatv.launcher.settings.Prefs
import ru.polkatv.launcher.settings.SettingsRepository
import ru.polkatv.launcher.settings.polkaDataStore
import ru.polkatv.launcher.system.InputsController
import ru.polkatv.launcher.system.NetworkStatus
import ru.polkatv.launcher.system.SystemActions
import ru.polkatv.launcher.wallpaper.WallpaperController
import ru.polkatv.launcher.wallpaper.WallpaperLoader
import ru.polkatv.launcher.weather.OpenMeteoApi
import ru.polkatv.launcher.weather.OwmApi
import ru.polkatv.launcher.weather.WeatherCache
import ru.polkatv.launcher.weather.WeatherRepository
import java.io.File
import java.util.concurrent.TimeUnit

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
    val network = NetworkStatus(appContext)
    val inputs = InputsController(appContext, system)
    val httpClient = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).retryOnConnectionFailure(true).build()
    val weather = WeatherRepository(
        OwmApi(httpClient, BuildConfig.OWM_API_KEY, geocoder = OpenMeteoApi(httpClient)),
        WeatherCache(File(appContext.filesDir, "weather")),
    )
}
