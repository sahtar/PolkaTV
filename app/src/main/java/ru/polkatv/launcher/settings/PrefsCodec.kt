package ru.polkatv.launcher.settings

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PrefsCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun encode(p: Prefs): String = json.encodeToString(p)
    fun decode(s: String?): Prefs = s?.let { runCatching { json.decodeFromString<Prefs>(it) }.getOrNull() } ?: Prefs()
}
