package ru.polkatv.launcher.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface TileArt {
    data class Banner(val image: ImageBitmap) : TileArt
    data class Icon(val image: ImageBitmap, val start: Color, val end: Color) : TileArt
}

class TileArtLoader(private val context: Context) {
    private val pm = context.packageManager
    private val cache = LruCache<String, TileArt>(32)

    suspend fun load(app: TvApp): TileArt {
        cache.get(app.packageName)?.let { return it }
        val art = withContext(Dispatchers.IO) { build(app) }
        cache.put(app.packageName, art)
        return art
    }

    fun trim() { cache.evictAll() }

    private fun build(app: TvApp): TileArt {
        val banner: Drawable? = runCatching { pm.getActivityInfo(app.component, 0).loadBanner(pm) }.getOrNull()
            ?: runCatching { pm.getApplicationBanner(app.packageName) }.getOrNull()
        if (banner != null && banner.intrinsicWidth > 0) {
            return TileArt.Banner(banner.toBitmap(640, 360, Bitmap.Config.ARGB_8888).asImageBitmap())
        }
        val icon = runCatching { pm.getActivityIcon(app.component) }.getOrElse { pm.getApplicationIcon(app.packageName) }
        val bmp = icon.toBitmap(128, 128, Bitmap.Config.ARGB_8888)
        val palette = Palette.from(bmp).clearFilters().generate()
        val start = palette.vibrantSwatch ?: palette.dominantSwatch
        val end = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.mutedSwatch
        val c1 = start?.let { Color(it.rgb) } ?: Color(0xFF3A4A6B)
        val c2 = end?.let { Color(it.rgb) } ?: Color(c1.red * 0.45f, c1.green * 0.45f, c1.blue * 0.45f)
        return TileArt.Icon(bmp.asImageBitmap(), c1, c2)
    }
}
