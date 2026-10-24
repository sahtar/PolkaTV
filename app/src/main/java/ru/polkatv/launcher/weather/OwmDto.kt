package ru.polkatv.launcher.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OwmCurrentDto(
    val dt: Long,
    val timezone: Int = 0,
    val main: OwmMain,
    val wind: OwmWind = OwmWind(),
    val weather: List<OwmCondition> = emptyList(),
    val sys: OwmSys = OwmSys(),
)

@Serializable
data class OwmMain(
    val temp: Double,
    @SerialName("feels_like") val feelsLike: Double = temp,
    val humidity: Int = 0,
    val pressure: Double = 1013.25,
    @SerialName("sea_level") val seaLevel: Double? = null,
    @SerialName("temp_min") val tempMin: Double? = null,
    @SerialName("temp_max") val tempMax: Double? = null,
)

@Serializable data class OwmWind(val speed: Double = 0.0, val deg: Double = 0.0)
@Serializable data class OwmCondition(val id: Int, val main: String = "", val description: String = "")
@Serializable data class OwmSys(val sunrise: Long? = null, val sunset: Long? = null, val pod: String? = null)

@Serializable
data class OwmForecastDto(val list: List<OwmForecastItem> = emptyList(), val city: OwmCity = OwmCity())

@Serializable
data class OwmForecastItem(
    val dt: Long,
    val main: OwmMain,
    val weather: List<OwmCondition> = emptyList(),
    val pop: Double = 0.0,
    val sys: OwmSys = OwmSys(),
)

@Serializable data class OwmCity(val timezone: Int = 0, val sunrise: Long? = null, val sunset: Long? = null)
