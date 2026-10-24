package ru.polkatv.launcher.apps

import android.content.ComponentName
import org.junit.Assert.assertEquals
import org.junit.Test

class TileResolverTest {
    private fun app(pkg: String) = TvApp(pkg, pkg.uppercase(), ComponentName(pkg, "$pkg.Main"), hasBanner = false)
    private val installed = listOf(app("a"), app("c"))

    @Test fun `installed packages resolve to App slots, others to Empty, order preserved`() {
        val slots = TileResolver.resolve(listOf("a", "b", null, "c"), installed)
        // ComponentName.equals is a stub under isReturnDefaultValues, so compare by fields.
        assertEquals(
            listOf(0 to "a", 1 to null, 2 to null, 3 to "c"),
            slots.map { it.index to (it as? TileSlot.App)?.app?.packageName },
        )
    }

    @Test fun `empty tiles list resolves to empty slots list`() {
        assertEquals(emptyList<TileSlot>(), TileResolver.resolve(emptyList(), installed))
    }
}
