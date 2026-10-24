package ru.polkatv.launcher.weather

enum class WmoKind {
    CLEAR, MAINLY_CLEAR, PARTLY_CLOUDY, OVERCAST, FOG,
    DRIZZLE_LIGHT, DRIZZLE, DRIZZLE_DENSE, FREEZING_DRIZZLE,
    RAIN_LIGHT, RAIN, RAIN_HEAVY, FREEZING_RAIN,
    SNOW_LIGHT, SNOW, SNOW_HEAVY, SNOW_GRAINS,
    SHOWERS_LIGHT, SHOWERS, SHOWERS_VIOLENT, SNOW_SHOWERS_LIGHT, SNOW_SHOWERS_HEAVY,
    THUNDERSTORM, THUNDERSTORM_HAIL, UNKNOWN,
}

enum class WeatherIcon { SUN, MOON, PARTLY_SUN, PARTLY_MOON, CLOUD, FOG, DRIZZLE, RAIN, SNOW, THUNDER }

object WmoCodes {
    fun kind(code: Int): WmoKind = when (code) {
        0 -> WmoKind.CLEAR
        1 -> WmoKind.MAINLY_CLEAR
        2 -> WmoKind.PARTLY_CLOUDY
        3 -> WmoKind.OVERCAST
        45, 48 -> WmoKind.FOG
        51 -> WmoKind.DRIZZLE_LIGHT
        53 -> WmoKind.DRIZZLE
        55 -> WmoKind.DRIZZLE_DENSE
        56, 57 -> WmoKind.FREEZING_DRIZZLE
        61 -> WmoKind.RAIN_LIGHT
        63 -> WmoKind.RAIN
        65 -> WmoKind.RAIN_HEAVY
        66, 67 -> WmoKind.FREEZING_RAIN
        71 -> WmoKind.SNOW_LIGHT
        73 -> WmoKind.SNOW
        75 -> WmoKind.SNOW_HEAVY
        77 -> WmoKind.SNOW_GRAINS
        80 -> WmoKind.SHOWERS_LIGHT
        81 -> WmoKind.SHOWERS
        82 -> WmoKind.SHOWERS_VIOLENT
        85 -> WmoKind.SNOW_SHOWERS_LIGHT
        86 -> WmoKind.SNOW_SHOWERS_HEAVY
        95 -> WmoKind.THUNDERSTORM
        96, 99 -> WmoKind.THUNDERSTORM_HAIL
        else -> WmoKind.UNKNOWN
    }

    fun icon(code: Int, isDay: Boolean): WeatherIcon = when (kind(code)) {
        WmoKind.CLEAR -> if (isDay) WeatherIcon.SUN else WeatherIcon.MOON
        WmoKind.MAINLY_CLEAR, WmoKind.PARTLY_CLOUDY -> if (isDay) WeatherIcon.PARTLY_SUN else WeatherIcon.PARTLY_MOON
        WmoKind.OVERCAST, WmoKind.UNKNOWN -> WeatherIcon.CLOUD
        WmoKind.FOG -> WeatherIcon.FOG
        WmoKind.DRIZZLE_LIGHT, WmoKind.DRIZZLE, WmoKind.DRIZZLE_DENSE, WmoKind.FREEZING_DRIZZLE -> WeatherIcon.DRIZZLE
        WmoKind.RAIN_LIGHT, WmoKind.RAIN, WmoKind.RAIN_HEAVY, WmoKind.FREEZING_RAIN,
        WmoKind.SHOWERS_LIGHT, WmoKind.SHOWERS, WmoKind.SHOWERS_VIOLENT -> WeatherIcon.RAIN
        WmoKind.SNOW_LIGHT, WmoKind.SNOW, WmoKind.SNOW_HEAVY, WmoKind.SNOW_GRAINS,
        WmoKind.SNOW_SHOWERS_LIGHT, WmoKind.SNOW_SHOWERS_HEAVY -> WeatherIcon.SNOW
        WmoKind.THUNDERSTORM, WmoKind.THUNDERSTORM_HAIL -> WeatherIcon.THUNDER
    }
}
