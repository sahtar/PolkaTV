package ru.polkatv.launcher.weather

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WeatherCacheTest {
    @get:Rule val tmp = TemporaryFolder()
    private val resp = Json { ignoreUnknownKeys = true }.decodeFromString<ForecastResponse>(javaClass.getResource("/openmeteo/forecast_sample.json")!!.readText())

    @Test fun `write then read round trips`() {
        val cache = WeatherCache(tmp.root)
        cache.write("55.7500_37.6250", CachedForecast(123L, resp))
        val back = cache.read("55.7500_37.6250")!!
        assertEquals(123L, back.fetchedAtEpochMs); assertEquals(61, back.response.current.code)
    }

    @Test fun `missing key reads null`() { assertNull(WeatherCache(tmp.root).read("nope")) }

    @Test fun `corrupted file is deleted and reads null`() {
        val cache = WeatherCache(tmp.root)
        File(tmp.root, "weather_bad.json").writeText("{oops")
        assertNull(cache.read("bad")); assertEquals(false, File(tmp.root, "weather_bad.json").exists())
    }

    @Test fun `delete removes the file`() {
        val cache = WeatherCache(tmp.root)
        cache.write("k", CachedForecast(1L, resp)); cache.delete("k"); assertNull(cache.read("k"))
    }
}
