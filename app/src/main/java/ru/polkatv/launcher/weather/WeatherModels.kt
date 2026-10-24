package ru.polkatv.launcher.weather

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class WindDir { N, NE, E, SE, S, SW, W, NW }

data class CurrentWeather(
    val tempC: Double, val feelsLikeC: Double, val humidity: Int, val pressureMmHg: Int,
    val windMs: Double, val windDir: WindDir, val precipProbability: Int?, val code: Int, val isDay: Boolean,
)

data class HourForecast(val time: LocalDateTime, val tempC: Double, val code: Int, val isDay: Boolean, val precipProbability: Int?)

data class DayForecast(
    val date: LocalDate, val tMinC: Double, val tMaxC: Double, val code: Int,
    val precipProbabilityMax: Int?, val sunrise: LocalTime?, val sunset: LocalTime?,
)

data class Weather(
    val current: CurrentWeather,
    val hourly: List<HourForecast>,
    val daily: List<DayForecast>,
    val fetchedAtEpochMs: Long,
    val zoneId: String,
)
