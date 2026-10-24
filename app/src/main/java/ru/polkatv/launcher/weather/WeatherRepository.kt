package ru.polkatv.launcher.weather

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.polkatv.launcher.settings.City
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class CityWeather(
    val city: City,
    val weather: Weather? = null,
    val stale: Boolean = false,
    val error: Boolean = false,
    val loading: Boolean = false,
)

class WeatherRepository(
    private val api: WeatherApi,
    private val cache: WeatherCache,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _states = MutableStateFlow<Map<String, CityWeather>>(emptyMap())
    val states: StateFlow<Map<String, CityWeather>> = _states.asStateFlow()
    private val mutex = Mutex()

    suspend fun refresh(city: City, force: Boolean = false) = mutex.withLock {
        val now = clock()
        val cached = cache.read(city.key)
        if (!force && cached != null && now - cached.fetchedAtEpochMs < FRESH_MS) {
            publish(city, cached, now, error = false); return@withLock
        }
        _states.update { it + (city.key to (it[city.key] ?: CityWeather(city)).copy(city = city, loading = true)) }
        val fresh = runCatching { api.forecast(city.latitude, city.longitude) }
        fresh.onSuccess { resp ->
            val entry = CachedForecast(now, resp)
            cache.write(city.key, entry)
            publish(city, entry, now, error = false)
        }.onFailure {
            publish(city, cached, now, error = true)
        }
    }

    suspend fun refreshAll(cities: List<City>, force: Boolean = false) {
        cities.forEach { refresh(it, force) }
        val keep = cities.map { it.key }.toSet()
        _states.update { m -> m.filterKeys { it in keep } }
    }

    fun remove(city: City) {
        cache.delete(city.key)
        _states.update { it - city.key }
    }

    suspend fun search(query: String): List<City> =
        api.search(query).map { City(it.name, it.admin1, it.country, it.latitude, it.longitude, it.timezone) }

    private fun publish(city: City, entry: CachedForecast?, now: Long, error: Boolean) {
        val weather = entry?.let { e ->
            val zone = runCatching { ZoneId.of(e.response.timezone) }.getOrDefault(ZoneId.systemDefault())
            WeatherMapper.map(e.response, e.fetchedAtEpochMs, LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone))
        }
        val stale = error || (entry != null && now - entry.fetchedAtEpochMs > STALE_MS)
        _states.update { it + (city.key to CityWeather(city, weather, stale, error, loading = false)) }
    }

    companion object {
        const val FRESH_MS = 30 * 60_000L
        const val STALE_MS = 60 * 60_000L
    }
}
