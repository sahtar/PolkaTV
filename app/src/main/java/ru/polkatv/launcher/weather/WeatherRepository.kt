package ru.polkatv.launcher.weather

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.polkatv.launcher.settings.City
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

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
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _states = MutableStateFlow<Map<String, CityWeather>>(emptyMap())
    val states: StateFlow<Map<String, CityWeather>> = _states.asStateFlow()
    private val mutex = Mutex()

    suspend fun refresh(city: City, force: Boolean = false) = mutex.withLock {
        try {
            val now = clock()
            val cached = withContext(io) { cache.read(city.key) }
            if (!force && cached != null && now - cached.fetchedAtEpochMs < FRESH_MS) {
                publish(city, cached, now, error = false); return@withLock
            }
            _states.update { it + (city.key to (it[city.key] ?: CityWeather(city)).copy(city = city, loading = true)) }
            try {
                val resp = api.forecast(city.latitude, city.longitude)
                val entry = CachedForecast(now, resp)
                // Validate by mapping before caching so a malformed response never poisons the cache.
                val weather = withContext(io) { toWeather(entry, now).also { cache.write(city.key, entry) } }
                put(city, weather, entry, now, error = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                publish(city, cached, now, error = true)
            }
        } catch (e: CancellationException) {
            _states.update { m -> m[city.key]?.let { m + (city.key to it.copy(loading = false)) } ?: m }
            throw e
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

    private fun toWeather(entry: CachedForecast, now: Long): Weather {
        val r = entry.response
        val zone = runCatching { ZoneId.of(r.timezone) }
            .getOrElse { runCatching { ZoneOffset.ofTotalSeconds(r.utcOffsetSeconds) }.getOrDefault(ZoneOffset.UTC) }
        return WeatherMapper.map(r, entry.fetchedAtEpochMs, LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone))
    }

    private suspend fun publish(city: City, entry: CachedForecast?, now: Long, error: Boolean) {
        if (entry == null) { put(city, null, null, now, error); return }
        val weather = try {
            withContext(io) { toWeather(entry, now) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Unusable cache entry: drop it so it cannot keep failing.
            withContext(io) { cache.delete(city.key) }
            put(city, null, null, now, error = true)
            return
        }
        put(city, weather, entry, now, error)
    }

    private fun put(city: City, weather: Weather?, entry: CachedForecast?, now: Long, error: Boolean) {
        val stale = error || (entry != null && now - entry.fetchedAtEpochMs > STALE_MS)
        _states.update { it + (city.key to CityWeather(city, weather, stale, error, loading = false)) }
    }

    companion object {
        const val FRESH_MS = 30 * 60_000L
        const val STALE_MS = 60 * 60_000L
    }
}
