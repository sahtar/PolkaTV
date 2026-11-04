package ru.polkatv.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import ru.polkatv.launcher.BuildConfig
import ru.polkatv.launcher.R
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.settings.ClockFormat
import ru.polkatv.launcher.settings.DimLevel
import ru.polkatv.launcher.ui.apps.AppPickerDialog
import ru.polkatv.launcher.ui.common.PolkaDialog
import ru.polkatv.launcher.ui.theme.Dimens
import ru.polkatv.launcher.ui.theme.TextPrimary
import ru.polkatv.launcher.ui.theme.TextTertiary
import ru.polkatv.launcher.ui.weather.AddCityDialog

private enum class Section(val titleRes: Int) {
    TILES(R.string.settings_tiles), CITIES(R.string.settings_cities), BACKGROUND(R.string.settings_background),
    CLOCK(R.string.settings_clock), ABOUT(R.string.settings_about),
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val tiles by vm.tiles.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf(Section.TILES) }
    var pickFor by remember { mutableStateOf<Int?>(null) }
    var addCity by remember { mutableStateOf(false) }
    var cityMenu by remember { mutableStateOf<Int?>(null) }
    var apiKeyDialog by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxSize().background(Color(0xFF0B1020)).padding(horizontal = Dimens.safeH, vertical = Dimens.safeV)) {
        Column(Modifier.width(360.dp).fillMaxHeight()) {
            Text(stringResource(R.string.settings_title), fontSize = 32.sp, color = TextPrimary)
            Spacer(Modifier.height(20.dp))
            Section.values().forEach { s ->
                ListItem(selected = section == s, onClick = { section = s }, headlineContent = { Text(stringResource(s.titleRes)) })
            }
        }
        Spacer(Modifier.width(48.dp))
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            when (section) {
                Section.TILES -> {
                    Text(stringResource(R.string.settings_tile_count, prefs.tiles.size), fontSize = 18.sp, color = TextTertiary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 5, 6).forEach { n ->
                            ListItem(selected = prefs.tiles.size == n, onClick = { vm.setTileCount(n) }, headlineContent = { Text("$n") },
                                leadingContent = { RadioButton(selected = prefs.tiles.size == n, onClick = null) }, modifier = Modifier.width(120.dp))
                        }
                    }
                    tiles.forEach { slot ->
                        val label = when (slot) {
                            is TileSlot.App -> stringResource(R.string.tile_n, slot.index + 1, slot.app.label)
                            is TileSlot.Empty -> stringResource(R.string.tile_n_empty, slot.index + 1)
                        }
                        ListItem(selected = false, onClick = { pickFor = slot.index }, headlineContent = { Text(label) })
                    }
                }
                Section.CITIES -> {
                    prefs.cities.forEachIndexed { i, c ->
                        ListItem(selected = false, onClick = { cityMenu = i }, headlineContent = { Text(c.displayName) }, supportingContent = { Text(c.country ?: "") })
                    }
                    if (prefs.cities.size < 6) ListItem(selected = false, onClick = { addCity = true }, headlineContent = { Text(stringResource(R.string.settings_city_add)) })
                    Spacer(Modifier.height(16.dp))
                    val keySet = prefs.owmApiKey.isNotBlank() || BuildConfig.OWM_API_KEY.isNotBlank()
                    ListItem(selected = false, onClick = { apiKeyDialog = true }, headlineContent = { Text(stringResource(R.string.settings_api_key)) },
                        supportingContent = { Text(stringResource(if (keySet) R.string.settings_api_key_set else R.string.settings_api_key_missing)) })
                }
                Section.BACKGROUND -> {
                    Text(stringResource(R.string.settings_interval), fontSize = 18.sp, color = TextTertiary)
                    listOf(0, 1, 5, 15, 30).forEach { m ->
                        val label = if (m == 0) stringResource(R.string.settings_interval_off) else stringResource(R.string.settings_interval_min, m)
                        ListItem(selected = prefs.wallpaperIntervalMinutes == m, onClick = { vm.setWallpaperInterval(m) }, headlineContent = { Text(label) },
                            leadingContent = { RadioButton(selected = prefs.wallpaperIntervalMinutes == m, onClick = null) })
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.settings_dim), fontSize = 18.sp, color = TextTertiary)
                    listOf(DimLevel.LIGHT to R.string.settings_dim_light, DimLevel.MEDIUM to R.string.settings_dim_medium).forEach { (lvl, res) ->
                        ListItem(selected = prefs.dimLevel == lvl, onClick = { vm.setDim(lvl) }, headlineContent = { Text(stringResource(res)) },
                            leadingContent = { RadioButton(selected = prefs.dimLevel == lvl, onClick = null) })
                    }
                }
                Section.CLOCK -> {
                    Text(stringResource(R.string.settings_clock_format), fontSize = 18.sp, color = TextTertiary)
                    listOf(ClockFormat.SYSTEM to R.string.settings_clock_system, ClockFormat.H24 to R.string.settings_clock_24, ClockFormat.H12 to R.string.settings_clock_12).forEach { (f, res) ->
                        ListItem(selected = prefs.clockFormat == f, onClick = { vm.setClockFormat(f) }, headlineContent = { Text(stringResource(res)) },
                            leadingContent = { RadioButton(selected = prefs.clockFormat == f, onClick = null) })
                    }
                    Spacer(Modifier.height(16.dp))
                    ListItem(selected = false, onClick = { vm.setShowDate(!prefs.showDate) }, headlineContent = { Text(stringResource(R.string.settings_show_date)) },
                        trailingContent = { Switch(checked = prefs.showDate, onCheckedChange = null) })
                }
                Section.ABOUT -> {
                    Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), fontSize = 20.sp, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.settings_set_home_hint), fontSize = 16.sp, color = TextTertiary)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_restore_hint), fontSize = 16.sp, color = TextTertiary)
                }
            }
        }
    }

    pickFor?.let { index ->
        AppPickerDialog(
            apps = apps,
            onPick = { vm.setTile(index, it.packageName); pickFor = null },
            onDismiss = { pickFor = null },
            extraAction = stringResource(R.string.settings_tile_clear) to { vm.setTile(index, null); pickFor = null },
        )
    }
    if (apiKeyDialog) ApiKeyDialog(current = prefs.owmApiKey, onSave = { vm.setOwmApiKey(it); apiKeyDialog = false }, onDismiss = { apiKeyDialog = false })
    if (addCity) AddCityDialog(search = { vm.searchCities(it) }, onPick = { vm.addCity(it); addCity = false }, onDismiss = { addCity = false })
    cityMenu?.let { i ->
        PolkaDialog(onDismiss = { cityMenu = null }, title = prefs.cities.getOrNull(i)?.displayName ?: "") {
            if (i > 0) ListItem(selected = false, onClick = { vm.moveCity(i, -1); cityMenu = null }, headlineContent = { Text(stringResource(R.string.settings_city_up)) })
            if (i < prefs.cities.size - 1) ListItem(selected = false, onClick = { vm.moveCity(i, +1); cityMenu = null }, headlineContent = { Text(stringResource(R.string.settings_city_down)) })
            ListItem(selected = false, onClick = { vm.removeCity(i); cityMenu = null }, headlineContent = { Text(stringResource(R.string.settings_city_remove)) })
            ListItem(selected = false, onClick = { cityMenu = null }, headlineContent = { Text(stringResource(R.string.action_cancel)) })
        }
    }
}
