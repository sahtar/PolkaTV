package ru.polkatv.launcher.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Locale

/**
 * OpenWeatherMap forecast source (free tier: current weather + 5-day/3-hour forecast).
 * City search is delegated to [geocoder] (Open-Meteo geocoding, reachable from the TV's network).
 */
class OwmApi(
    private val client: OkHttpClient,
    /** Read on every request, so a key saved in settings takes effect without restarting the app. */
    private val apiKey: () -> String,
    private val geocoder: WeatherApi,
    private val base: String = "https://api.openweathermap.org",
) : WeatherApi {
    constructor(client: OkHttpClient, apiKey: String, geocoder: WeatherApi, base: String = "https://api.openweathermap.org") :
        this(client, { apiKey }, geocoder, base)

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun forecast(lat: Double, lon: Double): ForecastResponse {
        val key = apiKey().trim()
        if (key.isBlank()) throw IOException("OpenWeatherMap API key is not set (Polka settings → Cities)")
        val current = json.decodeFromString<OwmCurrentDto>(get(url("/data/2.5/weather", lat, lon, key)))
        val forecast = json.decodeFromString<OwmForecastDto>(get(url("/data/2.5/forecast", lat, lon, key)))
        return OwmAdapter.toForecastResponse(current, forecast)
    }

    override suspend fun search(query: String): List<GeoResultDto> = geocoder.search(query)

    private fun url(path: String, lat: Double, lon: Double, key: String): String =
        "$base$path".toHttpUrl().newBuilder()
            .addQueryParameter("lat", String.format(Locale.US, "%.4f", lat))
            .addQueryParameter("lon", String.format(Locale.US, "%.4f", lon))
            .addQueryParameter("units", "metric")
            .addQueryParameter("lang", "ru")
            .addQueryParameter("appid", key)
            .build().toString()

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        client.newCall(Request.Builder().url(url).header("User-Agent", "PolkaTV/0.1").build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} from OpenWeatherMap ${url.substringBefore("?")}")
            resp.body?.string() ?: throw IOException("Empty body from OpenWeatherMap")
        }
    }
}
