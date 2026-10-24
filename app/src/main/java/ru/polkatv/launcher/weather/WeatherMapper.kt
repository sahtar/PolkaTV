package ru.polkatv.launcher.weather

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

object WeatherMapper {
    fun map(resp: ForecastResponse, fetchedAtEpochMs: Long, now: LocalDateTime): Weather {
        val startHour = now.truncatedTo(ChronoUnit.HOURS)
        val h = resp.hourly
        val hourCount = minOf(h.time.size, h.temperature.size, h.code.size, h.isDay.size)
        val hourly = (0 until hourCount).asSequence()
            .map { i ->
                HourForecast(
                    time = LocalDateTime.parse(resp.hourly.time[i]),
                    tempC = resp.hourly.temperature[i],
                    code = resp.hourly.code[i],
                    isDay = resp.hourly.isDay[i] == 1,
                    precipProbability = resp.hourly.precipProbability.getOrNull(i),
                )
            }
            .filter { !it.time.isBefore(startHour) }
            .take(24)
            .toList()
        val d = resp.daily
        val dayCount = minOf(d.time.size, d.code.size, d.tMax.size, d.tMin.size)
        val daily = (0 until dayCount).map { i ->
            DayForecast(
                date = LocalDate.parse(resp.daily.time[i]),
                tMinC = resp.daily.tMin[i],
                tMaxC = resp.daily.tMax[i],
                code = resp.daily.code[i],
                precipProbabilityMax = resp.daily.precipProbabilityMax.getOrNull(i),
                sunrise = resp.daily.sunrise.getOrNull(i)?.let(::parseTimeOrNull),
                sunset = resp.daily.sunset.getOrNull(i)?.let(::parseTimeOrNull),
            )
        }.take(7)
        val c = resp.current
        val current = CurrentWeather(
            tempC = c.temperature, feelsLikeC = c.apparent, humidity = c.humidity,
            pressureMmHg = Units.hpaToMmHg(c.pressureMsl), windMs = c.windSpeed, windDir = Units.windDir(c.windDirection),
            precipProbability = hourly.firstOrNull()?.precipProbability, code = c.code, isDay = c.isDay == 1,
        )
        return Weather(current, hourly, daily, fetchedAtEpochMs, resp.timezone)
    }

    private fun parseTimeOrNull(s: String): LocalTime? = runCatching { LocalDateTime.parse(s).toLocalTime() }.getOrNull()
}
