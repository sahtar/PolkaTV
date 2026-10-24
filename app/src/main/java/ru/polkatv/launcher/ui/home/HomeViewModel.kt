package ru.polkatv.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.apps.TileResolver
import ru.polkatv.launcher.apps.TvApp
import ru.polkatv.launcher.settings.TileOps

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    val state: StateFlow<HomeState> = combine(container.prefs, container.apps.apps) { prefs, apps ->
        HomeState(prefs = prefs, tiles = TileResolver.resolve(prefs.tiles, apps), apps = apps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

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
