package ru.polkatv.launcher.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.ui.common.PolkaDialog
import ru.polkatv.launcher.ui.theme.Dimens
import ru.polkatv.launcher.ui.theme.GlassBorder
import ru.polkatv.launcher.ui.theme.GlassFill
import ru.polkatv.launcher.ui.theme.TextPrimary
import ru.polkatv.launcher.ui.theme.TextSecondary
import ru.polkatv.launcher.ui.theme.TextTertiary
import ru.polkatv.launcher.ui.weather.WeatherStrings
import ru.polkatv.launcher.ui.weather.vector
import ru.polkatv.launcher.weather.CityWeather
import ru.polkatv.launcher.weather.WmoCodes
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun WeatherShelf(
    cities: List<CityWeather>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onOpenDetail: () -> Unit,
    onLongPress: () -> Unit,
    onAddCity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimens.cardCorner)
    val current = cities.getOrNull(selected)
    Card(
        onClick = { if (current == null) onAddCity() else onOpenDetail() },
        onLongClick = onLongPress,
        modifier = modifier.onPreviewKeyEvent { e ->
            if (e.type != KeyEventType.KeyDown || cities.size < 2) return@onPreviewKeyEvent false
            when (e.key) {
                Key.DirectionLeft -> { onSelect((selected - 1 + cities.size) % cities.size); true }
                Key.DirectionRight -> { onSelect((selected + 1) % cities.size); true }
                else -> false
            }
        },
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(containerColor = GlassFill, focusedContainerColor = GlassFill.copy(alpha = 0.6f)),
        scale = CardDefaults.scale(focusedScale = 1.01f),
        border = CardDefaults.border(border = Border(BorderStroke(1.dp, GlassBorder), shape = shape), focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = shape)),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 18.dp)) {
            when {
                current == null -> Text(stringResource(R.string.wx_add_city_prompt), fontSize = 26.sp, color = TextSecondary, modifier = Modifier.align(Alignment.Center))
                current.weather == null -> Column(Modifier.align(Alignment.CenterStart)) {
                    Text(current.city.displayName, fontSize = 30.sp, color = TextPrimary)
                    Text(stringResource(if (current.error) R.string.wx_no_data else R.string.wx_unknown), fontSize = 20.sp, color = TextSecondary)
                }
                else -> WeatherContent(current)
            }
            if (cities.size > 1) Dots(cities.size, selected, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun WeatherContent(cw: CityWeather) {
    val w = cw.weather!!
    val c = w.current
    // The default body line height (24sp) is taller than the small texts here and would not fit the shelf.
    ProvideTextStyle(LocalTextStyle.current.copy(lineHeight = TextUnit.Unspecified)) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(cw.city.displayName, fontSize = 28.sp, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(WmoCodes.icon(c.code, c.isDay).vector(), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(WeatherStrings.description(WmoCodes.kind(c.code))), fontSize = 18.sp, color = TextSecondary)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Detail(stringResource(R.string.wx_feels), stringResource(R.string.unit_temp, WeatherStrings.temp(c.feelsLikeC)))
                    Detail(stringResource(R.string.wx_wind), stringResource(R.string.unit_ms, String.format("%.0f", c.windMs)) + " " + stringResource(WeatherStrings.windDir(c.windDir)))
                    Detail(stringResource(R.string.wx_humidity), stringResource(R.string.unit_percent, c.humidity))
                    Detail(stringResource(R.string.wx_pressure), stringResource(R.string.unit_mmhg, c.pressureMmHg))
                    c.precipProbability?.let { Detail(stringResource(R.string.wx_precip), stringResource(R.string.unit_percent, it)) }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.unit_temp, WeatherStrings.temp(c.tempC)), fontSize = Dimens.tempSize, color = TextPrimary, lineHeight = Dimens.tempSize)
                if (cw.stale) {
                    val (res, qty) = WeatherStrings.ago(System.currentTimeMillis() - w.fetchedAtEpochMs)
                    Text(stringResource(R.string.wx_updated_ago, stringResource(res, qty)), fontSize = 16.sp, color = TextTertiary)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val fmt = DateTimeFormatter.ofPattern("HH:mm")
            w.hourly.take(8).forEach { h ->
                Column(
                    Modifier.weight(1f).background(Color(0x1AFFFFFF), RoundedCornerShape(12.dp)).padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(fmt.format(h.time), fontSize = 14.sp, color = TextTertiary)
                    Icon(WmoCodes.icon(h.code, h.isDay).vector(), contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp).padding(vertical = 2.dp))
                    Text(stringResource(R.string.unit_temp, WeatherStrings.temp(h.tempC)), fontSize = 17.sp, color = TextPrimary)
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Detail(label: String, value: String) {
    Column { Text(label, fontSize = 14.sp, color = TextTertiary); Text(value, fontSize = 19.sp, color = TextPrimary) }
}

@Composable
private fun Dots(count: Int, selected: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i -> Box(Modifier.size(8.dp).background(if (i == selected) Color.White else Color(0x4DFFFFFF), CircleShape)) }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CityMenuDialog(title: String, hasCity: Boolean, canAdd: Boolean, onAdd: () -> Unit, onRemove: () -> Unit, onMakeFirst: () -> Unit, onDismiss: () -> Unit) {
    PolkaDialog(onDismiss = onDismiss, title = title) {
        if (canAdd) ListItem(selected = false, onClick = onAdd, headlineContent = { Text(stringResource(R.string.city_menu_add)) })
        if (hasCity) ListItem(selected = false, onClick = onRemove, headlineContent = { Text(stringResource(R.string.city_menu_remove)) })
        if (hasCity) ListItem(selected = false, onClick = onMakeFirst, headlineContent = { Text(stringResource(R.string.city_menu_first)) })
        ListItem(selected = false, onClick = onDismiss, headlineContent = { Text(stringResource(R.string.action_cancel)) })
    }
}
