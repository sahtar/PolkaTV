package ru.polkatv.launcher.weather

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.polkatv.launcher.settings.City
import java.io.IOException

class WeatherRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val resp = Json { ignoreUnknownKeys = true }.decodeFromString<ForecastResponse>(javaClass.getResource("/openmeteo/forecast_sample.json")!!.readText())
    private val city = City("Москва", "Москва", "Россия", 55.75, 37.625, "Europe/Moscow")

    private class FakeApi(var fail: Boolean = false, var hang: Boolean = false, var resp: ForecastResponse, var geo: List<GeoResultDto>? = null) : WeatherApi {
        var calls = 0
        override suspend fun forecast(lat: Double, lon: Double): ForecastResponse { calls++; if (hang) awaitCancellation(); if (fail) throw IOException("offline"); return resp }
        override suspend fun search(query: String): List<GeoResultDto> = geo ?: listOf(GeoResultDto("Москва", 55.75, 37.62, "Россия", "Москва", "Europe/Moscow"))
    }

    @Test fun `first refresh fetches, second within 30 minutes uses cache`() = runTest {
        var now = 1_000_000L
        val api = FakeApi(resp = resp)
        val repo = WeatherRepository(api, WeatherCache(tmp.root), clock = { now })
        repo.refresh(city); assertEquals(1, api.calls)
        now += 10 * 60_000L
        repo.refresh(city); assertEquals(1, api.calls)
        now += 25 * 60_000L
        repo.refresh(city); assertEquals(2, api.calls)
        val s = repo.states.value[city.key]!!
        assertNotNull(s.weather); assertFalse(s.stale); assertFalse(s.error)
    }

    @Test fun `failure keeps cached weather and marks stale and error`() = runTest {
        var now = 1_000_000L
        val api = FakeApi(resp = resp)
        val repo = WeatherRepository(api, WeatherCache(tmp.root), clock = { now })
        repo.refresh(city)
        now += 61 * 60_000L; api.fail = true
        repo.refresh(city)
        val s = repo.states.value[city.key]!!
        assertNotNull(s.weather); assertTrue(s.stale); assertTrue(s.error)
    }

    @Test fun `failure without cache yields error and null weather`() = runTest {
        val repo = WeatherRepository(FakeApi(fail = true, resp = resp), WeatherCache(tmp.root), clock = { 5L })
        repo.refresh(city)
        val s = repo.states.value[city.key]!!
        assertNull(s.weather); assertTrue(s.error)
    }

    @Test fun `force refresh ignores freshness`() = runTest {
        val api = FakeApi(resp = resp)
        val repo = WeatherRepository(api, WeatherCache(tmp.root), clock = { 1L })
        repo.refresh(city); repo.refresh(city, force = true); assertEquals(2, api.calls)
    }

    @Test fun `remove drops state and cache`() = runTest {
        val repo = WeatherRepository(FakeApi(resp = resp), WeatherCache(tmp.root), clock = { 1L })
        repo.refresh(city); repo.remove(city)
        assertNull(repo.states.value[city.key]); assertNull(WeatherCache(tmp.root).read(city.key))
    }

    @Test fun `search maps geo results to cities`() = runTest {
        val repo = WeatherRepository(FakeApi(resp = resp), WeatherCache(tmp.root))
        val cities = repo.search("Moskva")
        assertEquals(City("Москва", "Москва", "Россия", 55.75, 37.62, "Europe/Moscow"), cities.single())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun `cancelled refresh does not publish an error and clears loading`() = runTest {
        val api = FakeApi(hang = true, resp = resp)
        val repo = WeatherRepository(api, WeatherCache(tmp.root), clock = { 1L }, io = StandardTestDispatcher(testScheduler))
        val job = launch { repo.refresh(city) }
        runCurrent()
        assertTrue(repo.states.value[city.key]!!.loading)
        job.cancelAndJoin()
        val s = repo.states.value[city.key]!!
        assertFalse(s.error); assertFalse(s.loading); assertNull(s.weather)
    }

    @Test fun `unmappable response yields error and is not cached`() = runTest {
        val broken = resp.copy(daily = resp.daily.copy(time = listOf("not-a-date") + resp.daily.time.drop(1)))
        val cache = WeatherCache(tmp.root)
        val repo = WeatherRepository(FakeApi(resp = broken), cache, clock = { 1L })
        repo.refresh(city)
        val s = repo.states.value[city.key]!!
        assertTrue(s.error); assertFalse(s.loading); assertNull(s.weather)
        assertNull(cache.read(city.key))
    }

    @Test fun `refreshAll keeps listed cities, drops others from state but not from cache`() = runTest {
        val other = City("Казань", "Татарстан", "Россия", 55.79, 49.12, "Europe/Moscow")
        val cache = WeatherCache(tmp.root)
        val repo = WeatherRepository(FakeApi(resp = resp), cache, clock = { 1L })
        repo.refreshAll(listOf(city, other))
        assertNotNull(repo.states.value[city.key]); assertNotNull(repo.states.value[other.key])
        repo.refreshAll(listOf(city))
        assertNotNull(repo.states.value[city.key]); assertNull(repo.states.value[other.key])
        assertNotNull(cache.read(other.key))
    }

    @Test fun `search drops results with identical coordinate keys`() = runTest {
        val dup = listOf(
            GeoResultDto("Москва", 55.75001, 37.62, "Россия", "Москва", "Europe/Moscow"),
            GeoResultDto("Moscow", 55.75002, 37.62, "Russia", "Moscow", "Europe/Moscow"),
        )
        val repo = WeatherRepository(FakeApi(resp = resp, geo = dup), WeatherCache(tmp.root))
        assertEquals(1, repo.search("Mosk").size)
    }
}
