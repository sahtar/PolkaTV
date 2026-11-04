package ru.polkatv.launcher.weather

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class OwmApiTest {
    private lateinit var server: MockWebServer
    private val geocoder = object : WeatherApi {
        override suspend fun forecast(lat: Double, lon: Double): ForecastResponse = error("not used")
        override suspend fun search(query: String): List<GeoResultDto> = listOf(GeoResultDto("Москва", 55.75, 37.62, "Россия", "Москва", "Europe/Moscow"))
    }

    @Before fun setUp() { server = MockWebServer(); server.start() }
    @After fun tearDown() { server.shutdown() }

    private fun api(key: String = "k123") = OwmApi(OkHttpClient(), key, geocoder, base = server.url("/").toString().trimEnd('/'))

    @Test fun `forecast calls weather then forecast with key, metric units and russian`() = runTest {
        server.enqueue(MockResponse().setBody(javaClass.getResource("/openmeteo/owm_weather_sample.json")!!.readText()))
        server.enqueue(MockResponse().setBody(javaClass.getResource("/openmeteo/owm_forecast_sample.json")!!.readText()))
        val r = api().forecast(55.7558, 37.6173)
        val first = server.takeRequest().requestUrl!!; val second = server.takeRequest().requestUrl!!
        assertEquals("/data/2.5/weather", first.encodedPath); assertEquals("/data/2.5/forecast", second.encodedPath)
        for (u in listOf(first, second)) {
            assertEquals("k123", u.queryParameter("appid")); assertEquals("metric", u.queryParameter("units")); assertEquals("ru", u.queryParameter("lang"))
            assertEquals("55.7558", u.queryParameter("lat")); assertEquals("37.6173", u.queryParameter("lon"))
        }
        assertEquals(61, r.current.code); assertEquals(8, r.hourly.time.size)
    }

    @Test(expected = IOException::class)
    fun `blank key fails fast without a request`() = runTest {
        try { api(key = "").forecast(1.0, 2.0) } finally { assertEquals(0, server.requestCount) }
    }

    @Test
    fun `http 401 throws IOException that does not leak the key`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{\"cod\":401,\"message\":\"Invalid API key\"}"))
        try {
            api().forecast(1.0, 2.0)
            fail("expected IOException")
        } catch (e: IOException) {
            assertFalse(e.message!!.contains("k123"))
        }
    }

    @Test fun `key provider is read per request so a key saved later takes effect`() = runTest {
        var key = ""
        val api = OwmApi(OkHttpClient(), { key }, geocoder, base = server.url("/").toString().trimEnd('/'))
        try { api.forecast(1.0, 2.0); fail("expected IOException") } catch (e: IOException) { assertEquals(0, server.requestCount) }
        key = "later"
        server.enqueue(MockResponse().setBody(javaClass.getResource("/openmeteo/owm_weather_sample.json")!!.readText()))
        server.enqueue(MockResponse().setBody(javaClass.getResource("/openmeteo/owm_forecast_sample.json")!!.readText()))
        api.forecast(55.7558, 37.6173)
        assertEquals("later", server.takeRequest().requestUrl!!.queryParameter("appid"))
    }

    @Test fun `search delegates to the geocoder`() = runTest {
        assertEquals("Москва", api().search("Moskva").single().name)
        assertEquals(0, server.requestCount)
    }
}
