package ru.polkatv.launcher.weather

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class OwmAdapterTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val cur = json.decodeFromString<OwmCurrentDto>(javaClass.getResource("/openmeteo/owm_weather_sample.json")!!.readText())
    private val fc = json.decodeFromString<OwmForecastDto>(javaClass.getResource("/openmeteo/owm_forecast_sample.json")!!.readText())
    private val r = OwmAdapter.toForecastResponse(cur, fc)

    @Test fun `fixture epoch arithmetic`() {
        assertEquals(LocalDateTime.of(2026, 10, 24, 21, 0), LocalDateTime.ofEpochSecond(1792864800L, 0, ZoneOffset.ofTotalSeconds(10800)))
    }

    @Test fun `current is converted with wmo code, local time, night flag`() {
        assertEquals("2026-10-24T21:30:00", r.current.time)
        assertEquals(7.3, r.current.temperature, 0.001); assertEquals(4.9, r.current.apparent, 0.001)
        assertEquals(81, r.current.humidity); assertEquals(1003.0, r.current.pressureMsl, 0.001)
        assertEquals(6.1, r.current.windSpeed, 0.001); assertEquals(250.0, r.current.windDirection, 0.001)
        assertEquals(61, r.current.code); assertEquals(0, r.current.isDay)
        assertEquals(10800, r.utcOffsetSeconds); assertEquals("+03:00", r.timezone)
    }

    @Test fun `hourly arrays come from the 3-hour items`() {
        assertEquals(8, r.hourly.time.size)
        assertEquals("2026-10-24T21:00:00", r.hourly.time[0]); assertEquals("2026-10-25T18:00:00", r.hourly.time[7])
        assertEquals(7.4, r.hourly.temperature[0], 0.001)
        assertEquals(listOf(61, 3, 2, 1, 0, 2, 61, 63), r.hourly.code)
        assertEquals(listOf(65, 30, 10, 0, 0, 5, 45, 80), r.hourly.precipProbability)
        assertEquals(listOf(0, 0, 0, 0, 1, 1, 1, 0), r.hourly.isDay)
    }

    @Test fun `daily is grouped by local date with min max pop and a midday code`() {
        assertEquals(listOf("2026-10-24", "2026-10-25"), r.daily.time)
        assertEquals(7.0, r.daily.tMin[0], 0.001); assertEquals(7.4, r.daily.tMax[0], 0.001)
        assertEquals(3.7, r.daily.tMin[1], 0.001); assertEquals(8.0, r.daily.tMax[1], 0.001)
        assertEquals(listOf(65, 80), r.daily.precipProbabilityMax)
        assertEquals(61, r.daily.code[0])      // only item of the day
        assertEquals(2, r.daily.code[1])       // item closest to 13:00 local is 12:00 → id 802 → 2
        assertEquals("2026-10-24T07:21:00", r.daily.sunrise[0]); assertEquals("2026-10-25T17:02:00", r.daily.sunset[1])
    }

    @Test fun `mapper accepts the adapted response`() {
        val w = WeatherMapper.map(r, 1L, LocalDateTime.of(2026, 10, 24, 21, 30))
        assertEquals(61, w.current.code); assertEquals(65, w.current.precipProbability)
        assertEquals(8, w.hourly.size); assertEquals(2, w.daily.size)
        assertEquals(java.time.LocalTime.of(7, 21), w.daily[0].sunrise)
    }

    @Test fun `missing sunrise yields empty strings and null times`() {
        val noSun = fc.copy(city = fc.city.copy(sunrise = null, sunset = null))
        val r2 = OwmAdapter.toForecastResponse(cur.copy(sys = OwmSys()), noSun)
        assertEquals("", r2.daily.sunrise[0])
        assertNull(WeatherMapper.map(r2, 1L, LocalDateTime.of(2026, 10, 24, 21, 30)).daily[0].sunrise)
        assertEquals(1, r2.current.isDay)   // unknown sunrise/sunset → assume day
    }
}
