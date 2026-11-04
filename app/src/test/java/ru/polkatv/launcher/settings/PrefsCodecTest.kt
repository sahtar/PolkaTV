package ru.polkatv.launcher.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsCodecTest {
    @Test fun `round trip keeps every field`() {
        val p = Prefs(
            tiles = listOf("a", null, "c", "d"),
            cities = listOf(City("Москва", "Москва", "Россия", 55.7558, 37.6173, "Europe/Moscow")),
            selectedCity = 0,
            wallpaperIntervalMinutes = 15,
            dimLevel = DimLevel.LIGHT,
            showDate = false,
            clockFormat = ClockFormat.H12,
            owmApiKey = "0123456789abcdef0123456789abcdef",
        )
        assertEquals(p, PrefsCodec.decode(PrefsCodec.encode(p)))
    }

    @Test fun `null or garbage yields defaults`() {
        assertEquals(Prefs(), PrefsCodec.decode(null))
        assertEquals(Prefs(), PrefsCodec.decode("{not json"))
    }

    @Test fun `unknown keys are ignored and missing keys take defaults`() {
        val decoded = PrefsCodec.decode("""{"tiles":["x","y","z","w"],"futureField":1}""")
        assertEquals(listOf("x", "y", "z", "w"), decoded.tiles)
        assertEquals(5, decoded.wallpaperIntervalMinutes)
        assertEquals("", decoded.owmApiKey)
    }

    @Test fun `city key is stable and display name adds region when different`() {
        val c = City("Ростов-на-Дону", "Ростовская область", "Россия", 47.2357, 39.7015)
        assertEquals("47.2357_39.7015", c.key)
        assertEquals("Ростов-на-Дону, Ростовская область", c.displayName)
        assertEquals("Москва", City("Москва", "Москва", "Россия", 55.7558, 37.6173).displayName)
    }
}
