package ru.polkatv.launcher.settings

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class KeyEntryServerTest {
    private val received = mutableListOf<String>()
    private lateinit var server: KeyEntryServer
    private var port = 0
    private val client = OkHttpClient()
    private val form = "application/x-www-form-urlencoded".toMediaType()

    @Before fun up() { server = KeyEntryServer("abc234") { received += it }; port = server.start(preferredPort = 0) }
    @After fun down() { server.stop() }

    private fun get(path: String) = client.newCall(Request.Builder().url("http://127.0.0.1:$port$path").build()).execute()
    private fun post(path: String, body: String) = client.newCall(Request.Builder().url("http://127.0.0.1:$port$path").post(body.toRequestBody(form)).build()).execute()

    @Test fun `GET with the token serves the one-field form`() = get("/abc234").use {
        assertEquals(200, it.code)
        val html = it.body!!.string()
        assertTrue(html.contains("<form")); assertTrue(html.contains("name=\"key\"")); assertTrue(html.contains("action=\"/abc234\""))
    }

    @Test fun `POST with the token delivers the decoded, trimmed key`() {
        post("/abc234", "key=0123abcd%20&other=x").use { assertEquals(200, it.code); assertTrue(it.body!!.string().contains("сохранён")) }
        assertEquals(listOf("0123abcd"), received)
    }

    @Test fun `wrong token is 404 and nothing is saved`() {
        post("/zzzzzz", "key=0123abcd").use { assertEquals(404, it.code) }
        get("/").use { assertEquals(404, it.code) }
        assertTrue(received.isEmpty())
    }

    @Test fun `empty or non-printable key is 400`() {
        post("/abc234", "key=").use { assertEquals(400, it.code) }
        post("/abc234", "key=%D0%BA%D0%BB%D1%8E%D1%87").use { assertEquals(400, it.code) }
        assertTrue(received.isEmpty())
    }

    @Test fun `stop closes the port`() {
        server.stop()
        try { get("/abc234").close(); fail("expected a connection failure") } catch (e: IOException) { /* refused: good */ }
    }

    @Test fun `tokens are six characters from the look-alike-free alphabet`() {
        repeat(20) { val t = KeyEntryServer.newToken(); assertEquals(6, t.length); assertTrue(t.all { it in "abcdefghjkmnpqrstuvwxyz23456789" }) }
    }

    @Test fun `formField handles missing names and url encoding`() {
        assertEquals(null, KeyEntryServer.formField("a=1&b=2", "key"))
        assertEquals("x y", KeyEntryServer.formField("key=x+y", "key"))
        assertEquals("", KeyEntryServer.formField("key=", "key"))
    }
}
