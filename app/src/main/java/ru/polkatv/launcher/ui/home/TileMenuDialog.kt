package ru.polkatv.launcher.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.ui.common.PolkaDialog

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TileMenuDialog(slot: TileSlot.App, tileCount: Int, onReplace: () -> Unit, onMove: (Int) -> Unit, onDismiss: () -> Unit) {
    PolkaDialog(onDismiss = onDismiss, title = slot.app.label) {
        ListItem(selected = false, onClick = onReplace, headlineContent = { Text(stringResource(R.string.tile_menu_replace)) })
        if (slot.index > 0) ListItem(selected = false, onClick = { onMove(-1) }, headlineContent = { Text(stringResource(R.string.tile_menu_move_left)) })
        if (slot.index < tileCount - 1) ListItem(selected = false, onClick = { onMove(+1) }, headlineContent = { Text(stringResource(R.string.tile_menu_move_right)) })
        ListItem(selected = false, onClick = onDismiss, headlineContent = { Text(stringResource(R.string.action_cancel)) })
    }
}
