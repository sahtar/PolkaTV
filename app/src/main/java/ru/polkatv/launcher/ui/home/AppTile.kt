package ru.polkatv.launcher.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import ru.polkatv.launcher.R
import ru.polkatv.launcher.apps.TileArt
import ru.polkatv.launcher.apps.TileArtLoader
import ru.polkatv.launcher.apps.TileSlot
import ru.polkatv.launcher.ui.theme.Dimens

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TileCard(modifier: Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(Dimens.tileCorner)
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.aspectRatio(16f / 9f),
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(containerColor = Color(0x12FFFFFF)),
        scale = CardDefaults.scale(focusedScale = 1.10f),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = shape)),
        glow = CardDefaults.glow(focusedGlow = Glow(Color.Black.copy(alpha = 0.6f), 24.dp)),
    ) { Box(Modifier.fillMaxSize()) { content() } }
}

@Composable
fun AppTile(slot: TileSlot.App, artLoader: TileArtLoader, modifier: Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    var art by remember(slot.app.packageName) { mutableStateOf<TileArt?>(null) }
    LaunchedEffect(slot.app.packageName) { art = runCatching { artLoader.load(slot.app) }.getOrNull() }
    TileCard(modifier, onClick, onLongClick) {
        when (val a = art) {
            is TileArt.Banner -> Image(a.image, contentDescription = slot.app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            is TileArt.Icon -> Column(
                Modifier.fillMaxSize().background(Brush.linearGradient(listOf(a.start, a.end))),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(a.image, contentDescription = null, modifier = Modifier.size(36.dp))
                }
                Text(slot.app.label, fontSize = 14.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp).padding(bottom = 6.dp))
            }
            null -> Box(Modifier.fillMaxSize().background(Color(0x22FFFFFF)))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EmptyTile(modifier: Modifier, onClick: () -> Unit) {
    TileCard(modifier, onClick) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
            Text(stringResource(R.string.tile_empty), fontSize = 14.sp, lineHeight = 16.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AllAppsTile(modifier: Modifier, onClick: () -> Unit) {
    TileCard(modifier, onClick) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Apps, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Text(stringResource(R.string.tile_all_apps), fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
        }
    }
}
