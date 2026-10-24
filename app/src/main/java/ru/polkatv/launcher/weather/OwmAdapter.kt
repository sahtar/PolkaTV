package ru.polkatv.launcher.weather

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

/** Converts the two OpenWeatherMap responses into the canonical [ForecastResponse]. */
object OwmAdapter {
    private val iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun toForecastResponse(current: OwmCurrentDto, forecast: OwmForecastDto): ForecastResponse {
        val offset = ZoneOffset.ofTotalSeconds(current.timezone)
        fun local(epoch: Long): LocalDateTime = LocalDateTime.ofEpochSecond(epoch, 0, offset)

        val sunrise = current.sys.sunrise ?: forecast.city.sunrise
        val sunset = current.sys.sunset ?: forecast.city.sunset
        val isDayNow = if (sunrise != null && sunset != null) current.dt in sunrise..sunset else true
        val cur = CurrentDto(
            time = iso.format(local(current.dt)),
            temperature = current.main.temp,
            humidity = current.main.humidity,
            apparent = current.main.feelsLike,
            isDay = if (isDayNow) 1 else 0,
            precipitation = 0.0,
            code = OwmCodes.toWmo(current.weather.firstOrNull()?.id ?: 800),
            pressureMsl = current.main.seaLevel ?: current.main.pressure,
            windSpeed = current.wind.speed,
            windDirection = current.wind.deg,
        )

        val items = forecast.list.sortedBy { it.dt }
        val hourly = HourlyDto(
            time = items.map { iso.format(local(it.dt)) },
            temperature = items.map { it.main.temp },
            code = items.map { OwmCodes.toWmo(it.weather.firstOrNull()?.id ?: 800) },
            precipProbability = items.map { (it.pop * 100).roundToInt() },
            isDay = items.map { if (it.sys.pod == "n") 0 else 1 },
        )

        val sunriseTime: LocalTime? = sunrise?.let { local(it).toLocalTime() }
        val sunsetTime: LocalTime? = sunset?.let { local(it).toLocalTime() }
        val byDay: Map<LocalDate, List<OwmForecastItem>> = items.groupBy { local(it.dt).toLocalDate() }.toSortedMap()
        fun sunString(date: LocalDate, t: LocalTime?) = t?.let { iso.format(LocalDateTime.of(date, it)) } ?: ""
        val daily = DailyDto(
            time = byDay.keys.map { it.toString() },
            code = byDay.values.map { day -> OwmCodes.toWmo(representative(day, offset).weather.firstOrNull()?.id ?: 800) },
            tMax = byDay.values.map { day -> day.maxOf { it.main.tempMax ?: it.main.temp } },
            tMin = byDay.values.map { day -> day.minOf { it.main.tempMin ?: it.main.temp } },
            sunrise = byDay.keys.map { sunString(it, sunriseTime) },
            sunset = byDay.keys.map { sunString(it, sunsetTime) },
            precipProbabilityMax = byDay.values.map { day -> day.maxOf { (it.pop * 100).roundToInt() } },
        )
        return ForecastResponse(utcOffsetSeconds = current.timezone, timezone = offset.id, current = cur, hourly = hourly, daily = daily)
    }

    /** The item closest to 13:00 local time represents the day's weather. */
    private fun representative(day: List<OwmForecastItem>, offset: ZoneOffset): OwmForecastItem =
        day.minByOrNull { abs(LocalDateTime.ofEpochSecond(it.dt, 0, offset).hour - 13) } ?: day.first()
}
