package ru.polkatv.launcher.apps

sealed interface TileSlot {
    val index: Int
    data class App(override val index: Int, val app: TvApp) : TileSlot
    data class Empty(override val index: Int) : TileSlot
}

object TileResolver {
    fun resolve(tiles: List<String?>, apps: List<TvApp>): List<TileSlot> {
        val byPkg = apps.associateBy { it.packageName }
        return tiles.mapIndexed { i, pkg ->
            val app = pkg?.let { byPkg[it] }
            if (app != null) TileSlot.App(i, app) else TileSlot.Empty(i)
        }
    }
}
