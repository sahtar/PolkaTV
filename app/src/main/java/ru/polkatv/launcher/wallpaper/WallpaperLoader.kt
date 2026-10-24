package ru.polkatv.launcher.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WallpaperLoader(private val context: Context, private val targetWidth: Int = 1920, private val targetHeight: Int = 1080) {
    private val names: List<String> =
        (context.assets.list("wallpapers") ?: emptyArray()).filter { it.endsWith(".jpg") }.sorted()

    val count: Int get() = names.size

    suspend fun load(index: Int): ImageBitmap = withContext(Dispatchers.IO) {
        val name = names[index]
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open("wallpapers/$name").use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetWidth && bounds.outHeight / (sample * 2) >= targetHeight) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bmp = context.assets.open("wallpapers/$name").use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("cannot decode $name")
        bmp.asImageBitmap()
    }
}
