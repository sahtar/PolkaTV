package ru.polkatv.launcher.weather

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object WeatherMapper {
    fun map(resp: ForecastResponse, fetchedAtEpochMs: Long, now: LocalDateTime): Weather {
        val startHour = now.truncatedTo(ChronoUnit.HOURS)
        val hourly = resp.hourly.time.indices.asSequence()
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
        val daily = resp.daily.time.indices.map { i ->
            DayForecast(
                date = LocalDate.parse(resp.daily.time[i]),
                tMinC = resp.daily.tMin[i],
                tMaxC = resp.daily.tMax[i],
                code = resp.daily.code[i],
                precipProbabilityMax = resp.daily.precipProbabilityMax.getOrNull(i),
                sunrise = LocalDateTime.parse(resp.daily.sunrise[i]).toLocalTime(),
                sunset = LocalDateTime.parse(resp.daily.sunset[i]).toLocalTime(),
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
}
