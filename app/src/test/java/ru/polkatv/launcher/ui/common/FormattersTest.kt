package ru.polkatv.launcher.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class FormattersTest {
    @Test fun `clock 24h and 12h`() {
        assertEquals("21:47", Formatters.clock(LocalTime.of(21, 47), use24h = true))
        assertEquals("09:05", Formatters.clock(LocalTime.of(9, 5), use24h = true))
        assertEquals("9:47", Formatters.clock(LocalTime.of(21, 47), use24h = false))
        assertEquals("12:00", Formatters.clock(LocalTime.of(0, 0), use24h = false))
    }

    @Test fun `russian date is weekday comma day month in genitive`() {
        assertEquals("суббота, 24 октября", Formatters.date(LocalDate.of(2026, 10, 24), Locale("ru")))
        assertEquals("понедельник, 2 марта", Formatters.date(LocalDate.of(2026, 3, 2), Locale("ru")))
    }

    @Test fun `english date`() {
        assertEquals("Saturday, 24 October", Formatters.date(LocalDate.of(2026, 10, 24), Locale.ENGLISH))
    }
}
