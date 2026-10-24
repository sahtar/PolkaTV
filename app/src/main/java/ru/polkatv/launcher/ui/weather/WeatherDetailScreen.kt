package ru.polkatv.launcher.ui.weather

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.ui.theme.Dimens
import ru.polkatv.launcher.ui.theme.TextPrimary
import ru.polkatv.launcher.ui.theme.TextSecondary
import ru.polkatv.launcher.ui.theme.TextTertiary
import ru.polkatv.launcher.weather.CityWeather
import ru.polkatv.launcher.weather.HourForecast
import ru.polkatv.launcher.weather.WmoCodes
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WeatherDetailScreen(cityWeather: CityWeather?, onBack: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Box(Modifier.fillMaxSize().background(Color(0xFF0B1020))) {
        val w = cityWeather?.weather
        if (cityWeather == null || w == null) {
            Text(stringResource(R.string.wx_no_data), fontSize = 26.sp, color = TextSecondary, modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        // The default body line height (24sp) would not let seven forecast days fit the 540dp screen height.
        ProvideTextStyle(LocalTextStyle.current.copy(lineHeight = TextUnit.Unspecified)) {
        Column(Modifier.fillMaxSize().padding(horizontal = Dimens.safeH, vertical = Dimens.safeV)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(cityWeather.city.displayName, fontSize = 34.sp, color = TextPrimary)
                    Text(stringResource(WeatherStrings.description(WmoCodes.kind(w.current.code))), fontSize = 20.sp, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    val sunrise = w.daily.firstOrNull()?.sunrise
                    val sunset = w.daily.firstOrNull()?.sunset
                    if (sunrise != null && sunset != null) {
                        val tf = DateTimeFormatter.ofPattern("HH:mm")
                        Text(stringResource(R.string.wx_sunrise, tf.format(sunrise)) + "   " + stringResource(R.string.wx_sunset, tf.format(sunset)), fontSize = 16.sp, color = TextTertiary)
                    }
                }
                Text(stringResource(R.string.unit_temp, WeatherStrings.temp(w.current.tempC)), fontSize = 72.sp, color = TextPrimary)
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.wx_detail_hourly), fontSize = 18.sp, color = TextTertiary)
            Spacer(Modifier.height(6.dp))
            val hf = DateTimeFormatter.ofPattern("HH:mm")
            val firstChip = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { firstChip.requestFocus() } }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(2.dp)) {
                itemsIndexed(w.hourly, key = { _, h -> h.time.toString() }) { i, h ->
                    HourChip(h, hf, if (i == 0) Modifier.focusRequester(firstChip) else Modifier)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.wx_detail_daily), fontSize = 18.sp, color = TextTertiary)
            Spacer(Modifier.height(6.dp))
            val df = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
            w.daily.forEach { d ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (d.date == LocalDate.now()) stringResource(R.string.wx_today) else df.format(d.date), fontSize = 18.sp, color = TextPrimary, modifier = Modifier.width(280.dp))
                    Icon(WmoCodes.icon(d.code, true).vector(), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(WeatherStrings.description(WmoCodes.kind(d.code))), fontSize = 17.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                    Text(d.precipProbabilityMax?.let { stringResource(R.string.unit_percent, it) } ?: "", fontSize = 17.sp, color = TextTertiary, textAlign = TextAlign.End, modifier = Modifier.width(70.dp))
                    Text(stringResource(R.string.unit_temp, WeatherStrings.temp(d.tMinC)) + " … " + stringResource(R.string.unit_temp, WeatherStrings.temp(d.tMaxC)), fontSize = 18.sp, color = TextPrimary, textAlign = TextAlign.End, modifier = Modifier.width(160.dp))
                }
            }
        }
        }
    }
}

/** Focusable so the 24-hour strip can be scrolled with the D-pad. */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HourChip(h: HourForecast, fmt: DateTimeFormatter, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.width(92.dp)
            .background(Color(0x1AFFFFFF), shape)
            .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(fmt.format(h.time), fontSize = 14.sp, color = TextTertiary)
        Icon(WmoCodes.icon(h.code, h.isDay).vector(), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp).padding(vertical = 2.dp))
        Text(stringResource(R.string.unit_temp, WeatherStrings.temp(h.tempC)), fontSize = 17.sp, color = TextPrimary)
        h.precipProbability?.let { if (it > 0) Text(stringResource(R.string.unit_percent, it), fontSize = 12.sp, color = TextTertiary) }
    }
}
