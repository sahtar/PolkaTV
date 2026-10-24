package ru.polkatv.launcher.weather

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class WeatherMapperTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val resp = json.decodeFromString<ForecastResponse>(javaClass.getResource("/openmeteo/forecast_sample.json")!!.readText())
    private val now = LocalDateTime.of(2026, 10, 24, 21, 30)
    private val w = WeatherMapper.map(resp, fetchedAtEpochMs = 1_000L, now = now)

    @Test fun `current fields are converted`() {
        assertEquals(7.3, w.current.tempC, 0.001)
        assertEquals(4.9, w.current.feelsLikeC, 0.001)
        assertEquals(81, w.current.humidity)
        assertEquals(753, w.current.pressureMmHg)
        assertEquals(6.1, w.current.windMs, 0.001)
        assertEquals(WindDir.W, w.current.windDir)
        assertEquals(61, w.current.code)
        assertEquals(false, w.current.isDay)
        assertEquals(65, w.current.precipProbability)   // probability of the current hour (21:00)
        assertEquals("Europe/Moscow", w.zoneId)
        assertEquals(1_000L, w.fetchedAtEpochMs)
    }

    @Test fun `hourly starts at the current hour and keeps at most 24 entries`() {
        assertEquals(LocalDateTime.of(2026, 10, 24, 21, 0), w.hourly.first().time)
        assertEquals(9, w.hourly.size)
        assertEquals(7.4, w.hourly.first().tempC, 0.001)
        assertNull(w.hourly[7].precipProbability)        // 04:00 has null probability
    }

    @Test fun `daily has seven days with sunrise and sunset`() {
        assertEquals(7, w.daily.size)
        val d0 = w.daily.first()
        assertEquals(LocalDate.of(2026, 10, 24), d0.date)
        assertEquals(9.5, d0.tMaxC, 0.001); assertEquals(3.7, d0.tMinC, 0.001)
        assertEquals(61, d0.code); assertEquals(65, d0.precipProbabilityMax)
        assertEquals(LocalTime.of(7, 21), d0.sunrise); assertEquals(LocalTime.of(17, 2), d0.sunset)
        assertNull(w.daily[5].precipProbabilityMax)
    }

    @Test fun `when now is past all hourly data the hourly list is empty and precip is null`() {
        val late = WeatherMapper.map(resp, 0L, LocalDateTime.of(2026, 10, 26, 12, 0))
        assertEquals(0, late.hourly.size)
        assertNull(late.current.precipProbability)
    }

    @Test fun `ragged arrays and unparsable sunrise are tolerated`() {
        val ragged = resp.copy(
            hourly = resp.hourly.copy(temperature = resp.hourly.temperature.take(5)),
            daily = resp.daily.copy(sunrise = listOf("garbage")),
        )
        val m = WeatherMapper.map(ragged, 0L, now)
        assertEquals(2, m.hourly.size)                    // indices 3 and 4 of the 5 available
        assertNull(m.daily[0].sunrise)
        assertNull(m.daily[1].sunrise)                    // missing entry
        assertEquals(LocalTime.of(17, 2), m.daily[0].sunset)
    }
}
