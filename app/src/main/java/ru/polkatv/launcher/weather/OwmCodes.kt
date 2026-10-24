package ru.polkatv.launcher.weather

/** OpenWeatherMap condition id → WMO code used by [WmoCodes] and the UI. */
object OwmCodes {
    fun toWmo(owmId: Int): Int = when (owmId) {
        in 200..299 -> 95
        300, 310 -> 51
        301, 311, 313, 321 -> 53
        302, 312, 314 -> 55
        500 -> 61
        501 -> 63
        502, 503, 504 -> 65
        511 -> 66
        520 -> 80
        521 -> 81
        522, 531 -> 82
        600 -> 71
        601 -> 73
        602 -> 75
        611, 612, 613 -> 77
        615, 616, 620 -> 85
        621, 622 -> 86
        in 700..762 -> 45
        771 -> 82
        781 -> 95
        800 -> 0
        801 -> 1
        802, 803 -> 2
        804 -> 3
        else -> 3
    }
}
