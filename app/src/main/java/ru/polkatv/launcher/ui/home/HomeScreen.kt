package ru.polkatv.launcher.ui.home

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.R
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.system.TvInput
import ru.polkatv.launcher.ui.apps.AppPickerDialog
import ru.polkatv.launcher.ui.inputs.InputsDialog
import ru.polkatv.launcher.ui.theme.Dimens
import ru.polkatv.launcher.ui.weather.AddCityDialog

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    container: AppContainer,
    onOpenAllApps: () -> Unit,
    onOpenWeather: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val wallpaper by container.wallpapers.current.collectAsStateWithLifecycle()
    val firstTileFocus = remember { FocusRequester() }
    val tiers = remember { TierFocus() }
    var menuFor by remember { mutableStateOf<TileSlot.App?>(null) }
    var pickFor by remember { mutableStateOf<Int?>(null) }
    var cityMenu by remember { mutableStateOf(false) }
    var addCity by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val net by container.network.state.collectAsStateWithLifecycle()
    var inputs by remember { mutableStateOf<List<TvInput>?>(null) }
    fun orToast(ok: Boolean) { if (!ok) Toast.makeText(context, R.string.unavailable_on_tv, Toast.LENGTH_SHORT).show() }
    val headerActions = HeaderActions(
        onInputs = { inputs = container.inputs.list() },
        onNotifications = { orToast(container.system.openNotifications()) },
        onNetwork = { orToast(container.system.openNetworkSettings()) },
        onTvSettings = { orToast(container.system.openTvSettings()) },
        onPolkaSettings = onOpenSettings,
    )

    LaunchedEffect(Unit) { vm.startWeatherLoop() }
    DisposableEffect(Unit) { onDispose { vm.stopWeatherLoop() } }

    LaunchedEffect(state.tiles.isNotEmpty()) {
        if (state.tiles.isNotEmpty()) runCatching { firstTileFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        WallpaperBackground(bitmap = wallpaper, dimStrength = state.prefs.dimLevel.strength)
        Column(Modifier.fillMaxSize().padding(horizontal = Dimens.safeH, vertical = Dimens.safeV)) {
            Box(Modifier.tierExits(up = null, down = tiers.shelf)) {
                Header(
                    clockFormat = state.prefs.clockFormat,
                    showDate = state.prefs.showDate,
                    net = net,
                    actions = headerActions,
                    iconsFocus = tiers.header,
                )
            }
            Spacer(Modifier.height(Dimens.tierGap))
            Box(Modifier.weight(1f).fillMaxWidth().tierExits(up = tiers.header, down = tiers.row)) {
                WeatherShelf(
                    cities = state.cities, selected = state.selectedCity,
                    onSelect = vm::selectCity,
                    onOpenDetail = { onOpenWeather(state.selectedCity) },
                    onLongPress = { cityMenu = true },
                    onAddCity = { addCity = true },
                    modifier = Modifier.fillMaxSize().focusRequester(tiers.shelf),
                )
            }
            Spacer(Modifier.height(Dimens.tierGap))
            Box(Modifier.tierExits(up = tiers.shelf, down = null)) {
                AppRow(
                    tiles = state.tiles,
                    artLoader = container.tileArt,
                    firstTileFocus = firstTileFocus,
                    onOpen = { vm.launchApp(it.app) },
                    onPickFor = { pickFor = it },
                    onMenu = { menuFor = it },
                    onAllApps = onOpenAllApps,
                    modifier = Modifier.focusRequester(tiers.row),
                )
            }
        }
    }

    menuFor?.let { slot ->
        TileMenuDialog(
            slot = slot, tileCount = state.tiles.size,
            onReplace = { menuFor = null; pickFor = slot.index },
            onMove = { dir -> vm.moveTile(slot.index, dir); menuFor = null },
            onDismiss = { menuFor = null },
        )
    }
    pickFor?.let { index ->
        AppPickerDialog(apps = state.apps, onPick = { vm.replaceTile(index, it.packageName); pickFor = null }, onDismiss = { pickFor = null })
    }
    if (cityMenu) {
        val current = state.cities.getOrNull(state.selectedCity)
        CityMenuDialog(
            title = current?.city?.displayName ?: stringResource(R.string.add_city_title),
            hasCity = current != null, canAdd = state.cities.size < 6,
            onAdd = { cityMenu = false; addCity = true },
            onRemove = { vm.removeCity(state.selectedCity); cityMenu = false },
            onMakeFirst = { vm.makeCityFirst(state.selectedCity); cityMenu = false },
            onDismiss = { cityMenu = false },
        )
    }
    if (addCity) AddCityDialog(search = { vm.searchCities(it) }, onPick = { vm.addCity(it); addCity = false }, onDismiss = { addCity = false })
    inputs?.let { list -> InputsDialog(inputs = list, onPick = { inputs = null; orToast(container.inputs.switchTo(it)) }, onDismiss = { inputs = null }) }
}
