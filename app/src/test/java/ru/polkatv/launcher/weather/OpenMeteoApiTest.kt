package ru.polkatv.launcher.weather

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class OpenMeteoApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: OpenMeteoApi

    @Before fun setUp() {
        server = MockWebServer(); server.start()
        val base = server.url("/").toString().trimEnd('/')
        api = OpenMeteoApi(OkHttpClient(), forecastBase = base, geoBase = base)
    }
    @After fun tearDown() { server.shutdown() }

    @Test fun `forecast requests the documented parameters and parses`() = runTest {
        server.enqueue(MockResponse().setBody(javaClass.getResource("/openmeteo/forecast_sample.json")!!.readText()))
        val r = api.forecast(55.75, 37.62)
        val req = server.takeRequest()
        assertEquals("/v1/forecast", req.requestUrl!!.encodedPath)
        val q = req.requestUrl!!
        assertEquals("55.75", q.queryParameter("latitude")); assertEquals("37.62", q.queryParameter("longitude"))
        assertEquals("ms", q.queryParameter("wind_speed_unit")); assertEquals("auto", q.queryParameter("timezone")); assertEquals("7", q.queryParameter("forecast_days"))
        assertTrue(q.queryParameter("current")!!.contains("pressure_msl"))
        assertTrue(q.queryParameter("hourly")!!.contains("precipitation_probability"))
        assertTrue(q.queryParameter("daily")!!.contains("sunrise"))
        assertEquals(61, r.current.code); assertEquals(12, r.hourly.time.size)
    }

    @Test fun `search uses russian language and parses results`() = runTest {
        server.enqueue(MockResponse().setBody(javaClass.getResource("/openmeteo/geocoding_sample.json")!!.readText()))
        val results = api.search("Moskva")
        val q = server.takeRequest().requestUrl!!
        assertEquals("/v1/search", q.encodedPath); assertEquals("Moskva", q.queryParameter("name")); assertEquals("ru", q.queryParameter("language")); assertEquals("8", q.queryParameter("count"))
        assertEquals(2, results.size); assertEquals("Москва", results[0].name); assertEquals("Москва", results[0].admin1)
    }

    @Test fun `empty results object parses to empty list`() = runTest {
        server.enqueue(MockResponse().setBody("{\"generationtime_ms\":0.1}"))
        assertEquals(0, api.search("zzzz").size)
    }

    @Test(expected = IOException::class)
    fun `http error throws IOException`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        api.forecast(1.0, 2.0)
    }
}
