package ru.polkatv.launcher.ui.apps

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
import ru.polkatv.launcher.settings.TileOps

class AllAppsViewModel(private val container: AppContainer) : ViewModel() {
    val apps: StateFlow<List<TvApp>> = container.apps.apps
    val tiles: StateFlow<List<TileSlot>> = combine(container.prefs, container.apps.apps) { p, a -> TileResolver.resolve(p.tiles, a) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun launch(app: TvApp) { container.apps.launch(app.packageName) }

    /** Returns false when every tile is taken (caller asks which one to replace). */
    fun addToFirstEmpty(pkg: String): Boolean {
        val current = container.prefs.value.tiles
        val next = TileOps.addToFirstEmpty(current, pkg) ?: return false
        viewModelScope.launch { container.settings.update { it.copy(tiles = next) } }
        return true
    }

    fun replaceTile(index: Int, pkg: String) = viewModelScope.launch {
        container.settings.update { it.copy(tiles = TileOps.replace(it.tiles, index, pkg)) }
    }

    fun openAppInfo(pkg: String): Boolean = container.system.openAppInfo(pkg)
}
