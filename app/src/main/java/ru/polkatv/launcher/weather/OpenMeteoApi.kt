package ru.polkatv.launcher.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale

class OpenMeteoApi(
    private val client: OkHttpClient,
    private val forecastBase: String = "https://api.open-meteo.com",
    private val geoBase: String = "https://geocoding-api.open-meteo.com",
) : WeatherApi {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun forecast(lat: Double, lon: Double): ForecastResponse {
        val url = "$forecastBase/v1/forecast".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", String.format(Locale.US, "%.4f", lat).trimZeros())
            .addQueryParameter("longitude", String.format(Locale.US, "%.4f", lon).trimZeros())
            .addQueryParameter("current", "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,pressure_msl,wind_speed_10m,wind_direction_10m")
            .addQueryParameter("hourly", "temperature_2m,weather_code,precipitation_probability,is_day")
            .addQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max")
            .addQueryParameter("wind_speed_unit", "ms")
            .addQueryParameter("timezone", "auto")
            .addQueryParameter("forecast_days", "7")
            .build()
        return json.decodeFromString(get(url.toString()))
    }

    override suspend fun search(query: String): List<GeoResultDto> {
        val url = "$geoBase/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", query)
            .addQueryParameter("count", "8")
            .addQueryParameter("language", "ru")
            .addQueryParameter("format", "json")
            .build()
        return json.decodeFromString<GeoResponse>(get(url.toString())).results
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).header("User-Agent", "PolkaTV/0.1").build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} for $url")
            resp.body?.string() ?: throw IOException("Empty body for $url")
        }
    }

    private fun String.trimZeros(): String = if (contains('.')) trimEnd('0').trimEnd('.') else this
}
