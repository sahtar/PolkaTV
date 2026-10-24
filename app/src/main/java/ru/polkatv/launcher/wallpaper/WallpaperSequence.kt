package ru.polkatv.launcher.wallpaper

import kotlin.random.Random

/**
 * Yields indices 0 until count in a random order, then reshuffles for the next cycle.
 * Guarantees the first index of a new cycle differs from the last index of the previous one
 * (when count > 1).
 */
class WallpaperSequence(private val count: Int, private val random: Random = Random.Default) {
    private var order: List<Int> = emptyList()
    private var pos = 0
    private var last = -1

    fun next(): Int {
        require(count > 0) { "count must be positive" }
        if (count == 1) return 0
        if (pos >= order.size) {
            var shuffled = (0 until count).shuffled(random)
            if (shuffled.first() == last) shuffled = shuffled.drop(1) + shuffled.first()
            order = shuffled
            pos = 0
        }
        last = order[pos++]
        return last
    }
}
