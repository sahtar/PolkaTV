package ru.polkatv.launcher.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TileOpsTest {
    private val t = listOf<String?>("a", "b", null, "d", "e")

    @Test fun `replace sets package at index`() {
        assertEquals(listOf("a", "x", null, "d", "e"), TileOps.replace(t, 1, "x"))
        assertEquals(listOf("a", "b", null, "d", null), TileOps.replace(t, 4, null))
    }

    @Test fun `replace ignores out of range index`() {
        assertEquals(t, TileOps.replace(t, 9, "x"))
        assertEquals(t, TileOps.replace(t, -1, "x"))
    }

    @Test fun `move swaps with neighbour`() {
        assertEquals(listOf("b", "a", null, "d", "e"), TileOps.move(t, 1, -1))
        assertEquals(listOf("a", null, "b", "d", "e"), TileOps.move(t, 1, +1))
    }

    @Test fun `move at edges is a no-op`() {
        assertEquals(t, TileOps.move(t, 0, -1))
        assertEquals(t, TileOps.move(t, 4, +1))
    }

    @Test fun `resize pads with null or truncates, clamped to 4 through 6`() {
        assertEquals(listOf("a", "b", null, "d", "e", null), TileOps.resize(t, 6))
        assertEquals(listOf("a", "b", null, "d"), TileOps.resize(t, 4))
        assertEquals(4, TileOps.resize(t, 1).size)
        assertEquals(6, TileOps.resize(t, 10).size)
    }

    @Test fun `addToFirstEmpty fills the first null and returns null when full`() {
        assertEquals(listOf("a", "b", "x", "d", "e"), TileOps.addToFirstEmpty(t, "x"))
        assertNull(TileOps.addToFirstEmpty(listOf("a", "b", "c", "d"), "x"))
    }

    @Test fun `addToFirstEmpty does not duplicate an existing package`() {
        assertEquals(t, TileOps.addToFirstEmpty(t, "a"))
    }
}
