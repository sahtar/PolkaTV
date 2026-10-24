package ru.polkatv.launcher.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import ru.polkatv.launcher.apps.TileArtLoader
import ru.polkatv.launcher.apps.TileSlot

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppRow(
    tiles: List<TileSlot>,
    artLoader: TileArtLoader,
    firstTileFocus: FocusRequester,
    onOpen: (TileSlot.App) -> Unit,
    onPickFor: (Int) -> Unit,
    onMenu: (TileSlot.App) -> Unit,
    onAllApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().focusRestorer { if (tiles.isEmpty()) FocusRequester.Default else firstTileFocus }.focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        tiles.forEachIndexed { i, slot ->
            val m = Modifier.weight(1f).let { if (i == 0) it.focusRequester(firstTileFocus) else it }
            when (slot) {
                is TileSlot.App -> AppTile(slot, artLoader, m, onClick = { onOpen(slot) }, onLongClick = { onMenu(slot) })
                is TileSlot.Empty -> EmptyTile(m, onClick = { onPickFor(slot.index) })
            }
        }
        AllAppsTile(Modifier.weight(1f), onClick = onAllApps)
    }
}
