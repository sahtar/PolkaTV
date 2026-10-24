package ru.polkatv.launcher.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class WallpaperSequenceTest {
    @Test fun `first cycle is a permutation of all indices`() {
        val seq = WallpaperSequence(count = 24, random = Random(1))
        val first = List(24) { seq.next() }
        assertEquals((0 until 24).toSet(), first.toSet())
    }

    @Test fun `second cycle is again a permutation and does not repeat the last frame`() {
        val seq = WallpaperSequence(count = 24, random = Random(7))
        val first = List(24) { seq.next() }
        val second = List(24) { seq.next() }
        assertEquals((0 until 24).toSet(), second.toSet())
        assertNotEquals(first.last(), second.first())
    }

    @Test fun `single wallpaper always returns zero`() {
        val seq = WallpaperSequence(count = 1, random = Random(3))
        repeat(5) { assertEquals(0, seq.next()) }
    }

    @Test fun `no repeats across many cycles boundary check`() {
        val seq = WallpaperSequence(count = 5, random = Random(11))
        var prev = seq.next()
        repeat(200) { val n = seq.next(); assertNotEquals(prev, n); prev = n }
    }
}
