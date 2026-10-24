package ru.polkatv.launcher.ui.common

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val f24 = DateTimeFormatter.ofPattern("HH:mm")
    private val f12 = DateTimeFormatter.ofPattern("h:mm", Locale.US)

    fun clock(t: LocalTime, use24h: Boolean): String = if (use24h) f24.format(t) else f12.format(t)

    fun date(d: LocalDate, locale: Locale): String = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale).format(d)
}
