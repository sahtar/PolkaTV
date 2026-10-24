package ru.polkatv.launcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.apps.TileResolver
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.apps.TvApp
import ru.polkatv.launcher.settings.City
import ru.polkatv.launcher.settings.ClockFormat
import ru.polkatv.launcher.settings.DimLevel
import ru.polkatv.launcher.settings.Prefs
import ru.polkatv.launcher.settings.TileOps

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val prefs: StateFlow<Prefs> = container.prefs
    val apps: StateFlow<List<TvApp>> = container.apps.apps
    val tiles: StateFlow<List<TileSlot>> = combine(container.prefs, container.apps.apps) { p, a -> TileResolver.resolve(p.tiles, a) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun edit(f: (Prefs) -> Prefs) = viewModelScope.launch { container.settings.update(f) }

    fun setTileCount(n: Int) = edit { it.copy(tiles = TileOps.resize(it.tiles, n)) }
    fun setTile(index: Int, pkg: String?) = edit { it.copy(tiles = TileOps.replace(it.tiles, index, pkg)) }

    fun addCity(city: City) {
        edit { p -> if (p.cities.size >= 6 || p.cities.any { it.key == city.key }) p else p.copy(cities = p.cities + city) }
        viewModelScope.launch { container.weather.refresh(city) }
    }
    fun removeCity(index: Int) {
        val c = container.prefs.value.cities.getOrNull(index) ?: return
        edit { p -> p.copy(cities = p.cities.filterIndexed { i, _ -> i != index }, selectedCity = 0) }
        container.weather.remove(c)
    }
    fun moveCity(index: Int, direction: Int) = edit { p ->
        val t = index + direction
        if (index !in p.cities.indices || t !in p.cities.indices) p
        else p.copy(cities = p.cities.toMutableList().also { l -> l[index] = p.cities[t]; l[t] = p.cities[index] }, selectedCity = 0)
    }

    fun setWallpaperInterval(min: Int) = edit { it.copy(wallpaperIntervalMinutes = min) }
    fun setDim(level: DimLevel) = edit { it.copy(dimLevel = level) }
    fun setClockFormat(f: ClockFormat) = edit { it.copy(clockFormat = f) }
    fun setShowDate(b: Boolean) = edit { it.copy(showDate = b) }

    suspend fun searchCities(q: String): List<City> = container.weather.search(q)
}
