package ru.polkatv.launcher.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastResponse(
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    val timezone: String = "UTC",
    val current: CurrentDto,
    val hourly: HourlyDto,
    val daily: DailyDto,
)

@Serializable
data class CurrentDto(
    val time: String,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Int,
    @SerialName("apparent_temperature") val apparent: Double,
    @SerialName("is_day") val isDay: Int,
    val precipitation: Double = 0.0,
    @SerialName("weather_code") val code: Int,
    @SerialName("pressure_msl") val pressureMsl: Double,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("wind_direction_10m") val windDirection: Double,
)

@Serializable
data class HourlyDto(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double>,
    @SerialName("weather_code") val code: List<Int>,
    @SerialName("precipitation_probability") val precipProbability: List<Int?>,
    @SerialName("is_day") val isDay: List<Int>,
)

@Serializable
data class DailyDto(
    val time: List<String>,
    @SerialName("weather_code") val code: List<Int>,
    @SerialName("temperature_2m_max") val tMax: List<Double>,
    @SerialName("temperature_2m_min") val tMin: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>,
    @SerialName("precipitation_probability_max") val precipProbabilityMax: List<Int?>,
)

@Serializable
data class GeoResponse(val results: List<GeoResultDto> = emptyList())

@Serializable
data class GeoResultDto(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
    val timezone: String? = null,
)

@Serializable
data class CachedForecast(val fetchedAtEpochMs: Long, val response: ForecastResponse)
