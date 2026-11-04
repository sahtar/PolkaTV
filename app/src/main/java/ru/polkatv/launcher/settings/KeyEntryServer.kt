package ru.polkatv.launcher.settings

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tiny LAN-only HTTP server through which a phone hands the OpenWeatherMap key to the TV: the settings dialog shows a
 * QR code with `http://<tv-ip>:<port>/<token>`, the page has one field, POST saves the key via [onKey].
 * It lives only while the dialog is open; the random token keeps other LAN devices from guessing the URL,
 * and nothing else is served. Pure JVM so it is unit-tested with a real socket.
 */
class KeyEntryServer(private val token: String, private val onKey: (String) -> Unit) {
    private var socket: ServerSocket? = null
    private val running = AtomicBoolean(false)

    /** Binds [preferredPort] (any free port if it is taken) on all interfaces; returns the bound port. */
    fun start(preferredPort: Int = DEFAULT_PORT): Int {
        val any = InetAddress.getByName("0.0.0.0")
        val s = runCatching { ServerSocket(preferredPort, 8, any) }.getOrElse { ServerSocket(0, 8, any) }
        socket = s
        running.set(true)
        Thread({ acceptLoop(s) }, "polka-key-entry").apply { isDaemon = true }.start()
        return s.localPort
    }

    fun stop() {
        running.set(false)
        runCatching { socket?.close() }
        socket = null
    }

    private fun acceptLoop(s: ServerSocket) {
        while (running.get()) {
            val c = try { s.accept() } catch (e: Exception) { if (s.isClosed) return else continue }
            runCatching { c.use { handle(it) } }
        }
    }

    private fun handle(c: Socket) {
        c.soTimeout = 5_000
        val input = c.getInputStream()
        val head = readHead(input) ?: return
        val lines = head.split("\r\n")
        val request = lines[0].split(" ")
        if (request.size < 2) return
        val method = request[0]
        val path = request[1].substringBefore('?')
        val headers = lines.drop(1).mapNotNull { l ->
            l.indexOf(':').takeIf { it > 0 }?.let { l.substring(0, it).trim().lowercase() to l.substring(it + 1).trim() }
        }.toMap()
        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        val body = if (contentLength in 1..MAX_BODY) readBody(input, contentLength) else ""

        val (status, html) = when {
            path != "/$token" -> 404 to page("Не найдено / Not found", "")
            method == "GET" -> 200 to formPage()
            method == "POST" -> {
                val key = formField(body, "key")
                if (key.isNullOrEmpty() || key.length > MAX_KEY || !key.all { it.code in 33..126 }) {
                    400 to page("Ключ не принят / Key rejected", "<p>Пустой или недопустимый ключ. / Empty or invalid key.</p><p><a href=\"/$token\">← Назад / Back</a></p>")
                } else {
                    onKey(key)
                    200 to page("Ключ сохранён / Key saved", "<p>Страницу можно закрыть, телевизор обновит погоду. / You can close this page; the TV will refresh the weather.</p>")
                }
            }
            else -> 405 to page("Метод не поддерживается / Method not allowed", "")
        }
        val bytes = html.toByteArray(Charsets.UTF_8)
        val reason = when (status) { 200 -> "OK"; 400 -> "Bad Request"; 404 -> "Not Found"; else -> "Method Not Allowed" }
        val out = c.getOutputStream()
        out.write("HTTP/1.1 $status $reason\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n".toByteArray(Charsets.ISO_8859_1))
        out.write(bytes)
        out.flush()
    }

    /** Reads up to the blank line that ends the request head; null on EOF or an oversized head. */
    private fun readHead(input: InputStream): String? {
        val buf = ByteArrayOutputStream()
        var last4 = 0
        while (buf.size() < MAX_HEAD) {
            val b = input.read()
            if (b < 0) return null
            buf.write(b)
            last4 = (last4 shl 8) or b
            if (last4 == 0x0D0A0D0A) return buf.toString(Charsets.ISO_8859_1.name()).removeSuffix("\r\n\r\n")
        }
        return null
    }

    private fun readBody(input: InputStream, n: Int): String {
        val b = ByteArray(n)
        var off = 0
        while (off < n) { val r = input.read(b, off, n - off); if (r < 0) break; off += r }
        return String(b, 0, off, Charsets.UTF_8)
    }

    private fun formPage() = page(
        "Ключ OpenWeatherMap / OpenWeatherMap key",
        """<form method="post" action="/$token">
<p>Вставьте ключ с openweathermap.org (My API keys) и нажмите «Отправить».<br>Paste your key from openweathermap.org and tap Send.</p>
<input name="key" autocomplete="off" autocapitalize="off" spellcheck="false" maxlength="128" autofocus placeholder="ключ / key">
<button type="submit">Отправить / Send</button>
</form>""",
    )

    private fun page(title: String, body: String) = """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>$title</title>
<style>body{font:18px -apple-system,Roboto,sans-serif;margin:0;padding:24px;background:#0b1020;color:#fff}h1{font-size:22px}input{width:100%;box-sizing:border-box;font:18px monospace;padding:12px;border-radius:10px;border:1px solid #556;background:#141a30;color:#fff;margin:12px 0}button{width:100%;font:18px sans-serif;padding:14px;border:0;border-radius:10px;background:#3b82f6;color:#fff}a{color:#9cf}</style></head>
<body><h1>$title</h1>$body</body></html>"""

    companion object {
        const val DEFAULT_PORT = 8765
        private const val MAX_HEAD = 16 * 1024
        private const val MAX_BODY = 4 * 1024
        private const val MAX_KEY = 128
        private const val TOKEN_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"

        /** Value of [name] in an `application/x-www-form-urlencoded` body, URL-decoded and trimmed; null when absent. */
        fun formField(body: String, name: String): String? = body.split('&')
            .map { it.substringBefore('=') to it.substringAfter('=', "") }
            .firstOrNull { it.first == name }?.second
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull()?.trim() }

        /** Six characters without look-alikes (no 0/o, 1/l/i): easy to read off the screen if the QR cannot be scanned. */
        fun newToken(): String { val r = SecureRandom(); return (1..6).map { TOKEN_ALPHABET[r.nextInt(TOKEN_ALPHABET.length)] }.joinToString("") }
    }
}
