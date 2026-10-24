package ru.polkatv.launcher.wallpaper

import androidx.compose.ui.graphics.ImageBitmap

/** Source of decoded wallpaper frames; lets the controller be tested without Android classes. */
interface FrameSource {
    val count: Int
    suspend fun load(index: Int): ImageBitmap
}
