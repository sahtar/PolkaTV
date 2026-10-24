package ru.polkatv.launcher.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.system.NetState
import ru.polkatv.launcher.ui.apps.AppPickerDialog
import ru.polkatv.launcher.ui.theme.Dimens

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    container: AppContainer,
    headerActions: HeaderActions,
    onOpenAllApps: () -> Unit,
    weatherShelf: @Composable (Modifier) -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val wallpaper by container.wallpapers.current.collectAsStateWithLifecycle()
    val firstTileFocus = remember { FocusRequester() }
    val tiers = remember { TierFocus() }
    var menuFor by remember { mutableStateOf<TileSlot.App?>(null) }
    var pickFor by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state.tiles.isNotEmpty()) {
        if (state.tiles.isNotEmpty()) runCatching { firstTileFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        WallpaperBackground(bitmap = wallpaper, dimStrength = state.prefs.dimLevel.strength)
        Column(Modifier.fillMaxSize().padding(horizontal = Dimens.safeH, vertical = Dimens.safeV)) {
            Box(Modifier.tierExits(up = null, down = tiers.row)) {
                Header(
                    clockFormat = state.prefs.clockFormat,
                    showDate = state.prefs.showDate,
                    net = NetState.WIFI,                       // Task 9 replaces with the live flow
                    actions = headerActions,
                    iconsFocus = tiers.header,
                )
            }
            Spacer(Modifier.height(Dimens.tierGap))
            weatherShelf(Modifier.weight(1f).fillMaxWidth().focusRequester(tiers.shelf))
            Spacer(Modifier.height(Dimens.tierGap))
            Box(Modifier.tierExits(up = tiers.header, down = null)) {
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
}
