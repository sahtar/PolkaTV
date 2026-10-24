package ru.polkatv.launcher.settings

object TileOps {
    const val MIN_TILES = 4
    const val MAX_TILES = 6

    fun replace(tiles: List<String?>, index: Int, pkg: String?): List<String?> =
        if (index !in tiles.indices) tiles else tiles.toMutableList().also { it[index] = pkg }

    /** direction: -1 moves left, +1 moves right; no-op at the edges. */
    fun move(tiles: List<String?>, index: Int, direction: Int): List<String?> {
        val target = index + direction
        if (index !in tiles.indices || target !in tiles.indices) return tiles
        return tiles.toMutableList().also { it[index] = tiles[target]; it[target] = tiles[index] }
    }

    fun resize(tiles: List<String?>, newSize: Int): List<String?> {
        val size = newSize.coerceIn(MIN_TILES, MAX_TILES)
        return if (size <= tiles.size) tiles.take(size) else tiles + List(size - tiles.size) { null }
    }

    /** Returns the new list, or null if there is no empty slot. Existing package → unchanged list. */
    fun addToFirstEmpty(tiles: List<String?>, pkg: String): List<String?>? {
        if (pkg in tiles) return tiles
        val i = tiles.indexOf(null)
        return if (i < 0) null else replace(tiles, i, pkg)
    }
}
