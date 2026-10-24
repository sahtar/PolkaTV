package ru.polkatv.launcher.ui.weather

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.polkatv.launcher.R

class AgoFormatterTest {
    @Test fun `minutes under an hour`() { assertEquals(R.string.ago_minutes to 5, WeatherStrings.ago(5 * 60_000L)) }
    @Test fun `zero rounds up to one minute`() { assertEquals(R.string.ago_minutes to 1, WeatherStrings.ago(10_000L)) }
    @Test fun `hours under a day`() { assertEquals(R.string.ago_hours to 2, WeatherStrings.ago(2 * 3_600_000L + 5)) }
    @Test fun `days`() { assertEquals(R.string.ago_days to 3, WeatherStrings.ago(3 * 86_400_000L)) }
}
