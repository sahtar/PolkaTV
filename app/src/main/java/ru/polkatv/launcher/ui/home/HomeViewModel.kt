package ru.polkatv.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.apps.TileResolver
import ru.polkatv.launcher.apps.TvApp
import ru.polkatv.launcher.settings.City
import ru.polkatv.launcher.settings.TileOps
import ru.polkatv.launcher.weather.CityWeather
import ru.polkatv.launcher.weather.WeatherRepository

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<HomeState> = combine(container.prefs, container.apps.apps, container.weather.states) { prefs, apps, wx ->
        val cities = prefs.cities.map { c -> wx[c.key] ?: CityWeather(c, loading = true) }
        HomeState(
            prefs = prefs,
            tiles = TileResolver.resolve(prefs.tiles, apps),
            apps = apps,
            cities = cities,
            selectedCity = prefs.selectedCity.coerceIn(0, (cities.size - 1).coerceAtLeast(0)),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private var refreshJob: Job? = null

    /** Called when the home screen becomes visible: refresh now (cache decides), then every 30 minutes. */
    fun startWeatherLoop() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            while (isActive) {
                container.weather.refreshAll(container.prefs.value.cities)
                delay(WeatherRepository.FRESH_MS)
            }
        }
    }
    fun stopWeatherLoop() { refreshJob?.cancel(); refreshJob = null }

    fun refreshWeather(force: Boolean = false) = viewModelScope.launch { container.weather.refreshAll(container.prefs.value.cities, force) }

    fun selectCity(index: Int) = viewModelScope.launch {
        container.settings.update { it.copy(selectedCity = index.coerceIn(0, (it.cities.size - 1).coerceAtLeast(0))) }
        container.prefs.value.cities.getOrNull(index)?.let { container.weather.refresh(it) }
    }

    fun addCity(city: City) = viewModelScope.launch {
        container.settings.update { p -> if (p.cities.size >= 6 || p.cities.any { it.key == city.key }) p else p.copy(cities = p.cities + city, selectedCity = p.cities.size) }
        container.weather.refresh(city)
    }

    fun removeCity(index: Int) = viewModelScope.launch {
        val city = container.prefs.value.cities.getOrNull(index) ?: return@launch
        container.settings.update { p -> p.copy(cities = p.cities.filterIndexed { i, _ -> i != index }, selectedCity = 0) }
        container.weather.remove(city)
    }

    fun makeCityFirst(index: Int) = viewModelScope.launch {
        container.settings.update { p ->
            val c = p.cities.getOrNull(index) ?: return@update p
            p.copy(cities = listOf(c) + p.cities.filterIndexed { i, _ -> i != index }, selectedCity = 0)
        }
    }

    suspend fun searchCities(query: String): List<City> = container.weather.search(query)

    fun launchApp(app: TvApp) {
        container.apps.launch(app.packageName)
    }

    fun replaceTile(index: Int, pkg: String?) = viewModelScope.launch {
        container.settings.update { it.copy(tiles = TileOps.replace(it.tiles, index, pkg)) }
    }

    fun moveTile(index: Int, direction: Int) = viewModelScope.launch {
        container.settings.update { it.copy(tiles = TileOps.move(it.tiles, index, direction)) }
    }

    fun addToFirstEmpty(pkg: String) = viewModelScope.launch {
        container.settings.update { p -> TileOps.addToFirstEmpty(p.tiles, pkg)?.let { p.copy(tiles = it) } ?: p }
    }
}
