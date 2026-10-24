package ru.polkatv.launcher.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WmoCodesTest {
    @Test fun `codes map to kinds`() {
        assertEquals(WmoKind.CLEAR, WmoCodes.kind(0))
        assertEquals(WmoKind.MAINLY_CLEAR, WmoCodes.kind(1))
        assertEquals(WmoKind.PARTLY_CLOUDY, WmoCodes.kind(2))
        assertEquals(WmoKind.OVERCAST, WmoCodes.kind(3))
        assertEquals(WmoKind.FOG, WmoCodes.kind(45)); assertEquals(WmoKind.FOG, WmoCodes.kind(48))
        assertEquals(WmoKind.DRIZZLE_LIGHT, WmoCodes.kind(51)); assertEquals(WmoKind.DRIZZLE, WmoCodes.kind(53)); assertEquals(WmoKind.DRIZZLE_DENSE, WmoCodes.kind(55))
        assertEquals(WmoKind.FREEZING_DRIZZLE, WmoCodes.kind(56)); assertEquals(WmoKind.FREEZING_DRIZZLE, WmoCodes.kind(57))
        assertEquals(WmoKind.RAIN_LIGHT, WmoCodes.kind(61)); assertEquals(WmoKind.RAIN, WmoCodes.kind(63)); assertEquals(WmoKind.RAIN_HEAVY, WmoCodes.kind(65))
        assertEquals(WmoKind.FREEZING_RAIN, WmoCodes.kind(66)); assertEquals(WmoKind.FREEZING_RAIN, WmoCodes.kind(67))
        assertEquals(WmoKind.SNOW_LIGHT, WmoCodes.kind(71)); assertEquals(WmoKind.SNOW, WmoCodes.kind(73)); assertEquals(WmoKind.SNOW_HEAVY, WmoCodes.kind(75))
        assertEquals(WmoKind.SNOW_GRAINS, WmoCodes.kind(77))
        assertEquals(WmoKind.SHOWERS_LIGHT, WmoCodes.kind(80)); assertEquals(WmoKind.SHOWERS, WmoCodes.kind(81)); assertEquals(WmoKind.SHOWERS_VIOLENT, WmoCodes.kind(82))
        assertEquals(WmoKind.SNOW_SHOWERS_LIGHT, WmoCodes.kind(85)); assertEquals(WmoKind.SNOW_SHOWERS_HEAVY, WmoCodes.kind(86))
        assertEquals(WmoKind.THUNDERSTORM, WmoCodes.kind(95))
        assertEquals(WmoKind.THUNDERSTORM_HAIL, WmoCodes.kind(96)); assertEquals(WmoKind.THUNDERSTORM_HAIL, WmoCodes.kind(99))
        assertEquals(WmoKind.UNKNOWN, WmoCodes.kind(42))
    }

    @Test fun `icons respect day and night for clear and partly cloudy`() {
        assertEquals(WeatherIcon.SUN, WmoCodes.icon(0, isDay = true))
        assertEquals(WeatherIcon.MOON, WmoCodes.icon(0, isDay = false))
        assertEquals(WeatherIcon.PARTLY_SUN, WmoCodes.icon(2, isDay = true))
        assertEquals(WeatherIcon.PARTLY_MOON, WmoCodes.icon(1, isDay = false))
        assertEquals(WeatherIcon.CLOUD, WmoCodes.icon(3, isDay = true))
        assertEquals(WeatherIcon.FOG, WmoCodes.icon(45, isDay = false))
        assertEquals(WeatherIcon.DRIZZLE, WmoCodes.icon(53, isDay = true))
        assertEquals(WeatherIcon.RAIN, WmoCodes.icon(65, isDay = true)); assertEquals(WeatherIcon.RAIN, WmoCodes.icon(81, isDay = true))
        assertEquals(WeatherIcon.SNOW, WmoCodes.icon(73, isDay = true)); assertEquals(WeatherIcon.SNOW, WmoCodes.icon(86, isDay = true))
        assertEquals(WeatherIcon.THUNDER, WmoCodes.icon(99, isDay = true))
        assertEquals(WeatherIcon.CLOUD, WmoCodes.icon(42, isDay = true))
    }
}
