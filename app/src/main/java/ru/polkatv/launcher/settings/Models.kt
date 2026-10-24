package ru.polkatv.launcher.settings

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class City(
    val name: String,
    val region: String? = null,
    val country: String? = null,
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
) {
    val key: String get() = String.format(Locale.US, "%.4f_%.4f", latitude, longitude)
    val displayName: String get() = if (!region.isNullOrBlank() && region != name) "$name, $region" else name
}

@Serializable
enum class DimLevel(val strength: Float) { LIGHT(0.5f), MEDIUM(1.0f) }

@Serializable
enum class ClockFormat { SYSTEM, H24, H12 }

val DEFAULT_TILES: List<String?> = listOf(
    "com.google.android.youtube.tv",
    "ru.kinopoisk.tv",
    "ru.ivi.client",
    "ru.mts.mtstv",
    "ru.more.play",
)

@Serializable
data class Prefs(
    val tiles: List<String?> = DEFAULT_TILES,
    val cities: List<City> = emptyList(),
    val selectedCity: Int = 0,
    val wallpaperIntervalMinutes: Int = 5,
    val dimLevel: DimLevel = DimLevel.MEDIUM,
    val showDate: Boolean = true,
    val clockFormat: ClockFormat = ClockFormat.SYSTEM,
)
