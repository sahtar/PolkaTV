package ru.polkatv.launcher.weather

import kotlin.math.roundToInt

object Units {
    fun hpaToMmHg(hpa: Double): Int = (hpa * 0.750062).roundToInt()

    fun windDir(degrees: Double): WindDir {
        val d = ((degrees % 360) + 360) % 360
        val sector = ((d + 22.5) / 45.0).toInt() % 8
        return WindDir.values()[sector]
    }
}
