package ru.polkatv.launcher.ui.apps

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ListItem
import androidx.tv.material3.Text
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.R
import ru.polkatv.launcher.apps.TileArt
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.apps.TvApp
import ru.polkatv.launcher.ui.common.PolkaDialog
import ru.polkatv.launcher.ui.home.TileCard
import ru.polkatv.launcher.ui.theme.Dimens
import ru.polkatv.launcher.ui.theme.TextPrimary
import ru.polkatv.launcher.ui.theme.TextSecondary

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AllAppsScreen(vm: AllAppsViewModel, container: AppContainer, onBack: () -> Unit) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val tiles by vm.tiles.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var menuFor by remember { mutableStateOf<TvApp?>(null) }
    var replaceFor by remember { mutableStateOf<TvApp?>(null) }

    Box(Modifier.fillMaxSize().background(Color(0xFF060912))) {
        Column(Modifier.fillMaxSize().padding(horizontal = Dimens.safeH, vertical = Dimens.safeV)) {
            Text(stringResource(R.string.all_apps_title), fontSize = 36.sp, color = TextPrimary)
            Spacer(Modifier.height(24.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GridTile(app, container, onClick = { vm.launch(app) }, onLongClick = { menuFor = app })
                        Spacer(Modifier.height(8.dp))
                        Text(app.label, fontSize = 15.sp, color = TextSecondary, maxLines = 1)
                    }
                }
            }
        }
    }

    menuFor?.let { app ->
        PolkaDialog(onDismiss = { menuFor = null }, title = app.label) {
            ListItem(selected = false, onClick = {
                menuFor = null
                if (!vm.addToFirstEmpty(app.packageName)) replaceFor = app
            }, headlineContent = { Text(stringResource(R.string.app_menu_add_home)) })
            ListItem(selected = false, onClick = {
                menuFor = null
                if (!vm.openAppInfo(app.packageName)) Toast.makeText(context, R.string.unavailable_on_tv, Toast.LENGTH_SHORT).show()
            }, headlineContent = { Text(stringResource(R.string.app_menu_info)) })
            ListItem(selected = false, onClick = { menuFor = null }, headlineContent = { Text(stringResource(R.string.action_cancel)) })
        }
    }
    replaceFor?.let { app ->
        PolkaDialog(onDismiss = { replaceFor = null }, title = stringResource(R.string.replace_which_title)) {
            tiles.forEach { slot ->
                val label = when (slot) {
                    is TileSlot.App -> stringResource(R.string.tile_n, slot.index + 1, slot.app.label)
                    is TileSlot.Empty -> stringResource(R.string.tile_n_empty, slot.index + 1)
                }
                ListItem(selected = false, onClick = { vm.replaceTile(slot.index, app.packageName); replaceFor = null }, headlineContent = { Text(label) })
            }
        }
    }
}

@Composable
private fun GridTile(app: TvApp, container: AppContainer, onClick: () -> Unit, onLongClick: () -> Unit) {
    var art by remember(app.packageName) { mutableStateOf<TileArt?>(null) }
    LaunchedEffect(app.packageName) { art = runCatching { container.tileArt.load(app) }.getOrNull() }
    TileCard(Modifier.fillMaxWidth(), onClick, onLongClick) {
        when (val a = art) {
            is TileArt.Banner -> Image(a.image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            is TileArt.Icon -> Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(a.start, a.end)))) {
                Image(a.image, contentDescription = null, modifier = Modifier.size(56.dp).align(Alignment.Center))
            }
            null -> Box(Modifier.fillMaxSize().background(Color(0x22FFFFFF)))
        }
    }
}
