package ru.polkatv.launcher.weather

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class WeatherCache(private val dir: File) {
    private val json = Json { ignoreUnknownKeys = true }

    private fun file(key: String) = File(dir, "weather_$key.json")

    fun read(key: String): CachedForecast? {
        val f = file(key)
        if (!f.exists()) return null
        return runCatching { json.decodeFromString<CachedForecast>(f.readText()) }
            .onFailure { f.delete() }
            .getOrNull()
    }

    fun write(key: String, value: CachedForecast) {
        dir.mkdirs()
        val tmp = File(dir, "weather_$key.tmp")
        tmp.writeText(json.encodeToString(value))
        tmp.renameTo(file(key))
    }

    fun delete(key: String) { file(key).delete() }
}
