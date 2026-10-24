package ru.polkatv.launcher.ui.weather

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import ru.polkatv.launcher.weather.WeatherIcon

fun WeatherIcon.vector(): ImageVector = when (this) {
    WeatherIcon.SUN -> Icons.Filled.WbSunny
    WeatherIcon.MOON -> Icons.Filled.NightsStay
    WeatherIcon.PARTLY_SUN -> Icons.Filled.WbCloudy
    WeatherIcon.PARTLY_MOON -> Icons.Filled.NightsStay
    WeatherIcon.CLOUD -> Icons.Filled.Cloud
    WeatherIcon.FOG -> Icons.Filled.BlurOn
    WeatherIcon.DRIZZLE -> Icons.Filled.Grain
    WeatherIcon.RAIN -> Icons.Filled.WaterDrop
    WeatherIcon.SNOW -> Icons.Filled.AcUnit
    WeatherIcon.THUNDER -> Icons.Filled.FlashOn
}
