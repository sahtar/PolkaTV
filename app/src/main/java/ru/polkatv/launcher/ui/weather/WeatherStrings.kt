package ru.polkatv.launcher.ui.weather

import androidx.annotation.StringRes
import ru.polkatv.launcher.R
import ru.polkatv.launcher.weather.WindDir
import ru.polkatv.launcher.weather.WmoKind

object WeatherStrings {
    @StringRes fun description(kind: WmoKind): Int = when (kind) {
        WmoKind.CLEAR -> R.string.wx_clear
        WmoKind.MAINLY_CLEAR -> R.string.wx_mainly_clear
        WmoKind.PARTLY_CLOUDY -> R.string.wx_partly_cloudy
        WmoKind.OVERCAST -> R.string.wx_overcast
        WmoKind.FOG -> R.string.wx_fog
        WmoKind.DRIZZLE_LIGHT -> R.string.wx_drizzle_light
        WmoKind.DRIZZLE -> R.string.wx_drizzle
        WmoKind.DRIZZLE_DENSE -> R.string.wx_drizzle_dense
        WmoKind.FREEZING_DRIZZLE -> R.string.wx_freezing_drizzle
        WmoKind.RAIN_LIGHT -> R.string.wx_rain_light
        WmoKind.RAIN -> R.string.wx_rain
        WmoKind.RAIN_HEAVY -> R.string.wx_rain_heavy
        WmoKind.FREEZING_RAIN -> R.string.wx_freezing_rain
        WmoKind.SNOW_LIGHT -> R.string.wx_snow_light
        WmoKind.SNOW -> R.string.wx_snow
        WmoKind.SNOW_HEAVY -> R.string.wx_snow_heavy
        WmoKind.SNOW_GRAINS -> R.string.wx_snow_grains
        WmoKind.SHOWERS_LIGHT -> R.string.wx_showers_light
        WmoKind.SHOWERS -> R.string.wx_showers
        WmoKind.SHOWERS_VIOLENT -> R.string.wx_showers_violent
        WmoKind.SNOW_SHOWERS_LIGHT -> R.string.wx_snow_showers_light
        WmoKind.SNOW_SHOWERS_HEAVY -> R.string.wx_snow_showers_heavy
        WmoKind.THUNDERSTORM -> R.string.wx_thunderstorm
        WmoKind.THUNDERSTORM_HAIL -> R.string.wx_thunderstorm_hail
        WmoKind.UNKNOWN -> R.string.wx_unknown
    }

    @StringRes fun windDir(dir: WindDir): Int = when (dir) {
        WindDir.N -> R.string.wind_n; WindDir.NE -> R.string.wind_ne; WindDir.E -> R.string.wind_e; WindDir.SE -> R.string.wind_se
        WindDir.S -> R.string.wind_s; WindDir.SW -> R.string.wind_sw; WindDir.W -> R.string.wind_w; WindDir.NW -> R.string.wind_nw
    }

    /** (resource, quantity) for the "updated … ago" label. */
    fun ago(ageMs: Long): Pair<Int, Int> {
        val minutes = (ageMs / 60_000L).toInt().coerceAtLeast(1)
        return when {
            minutes < 60 -> R.string.ago_minutes to minutes
            minutes < 24 * 60 -> R.string.ago_hours to minutes / 60
            else -> R.string.ago_days to minutes / (24 * 60)
        }
    }

    fun temp(t: Double): String { val r = Math.round(t).toInt(); return if (r > 0) "+$r" else "$r" }
}
