package ru.polkatv.launcher.ui.home

import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.apps.TvApp
import ru.polkatv.launcher.settings.Prefs
import ru.polkatv.launcher.weather.CityWeather

data class HomeState(
    val prefs: Prefs = Prefs(),
    val tiles: List<TileSlot> = emptyList(),
    val apps: List<TvApp> = emptyList(),
    val cities: List<CityWeather> = emptyList(),
    val selectedCity: Int = 0,
)
