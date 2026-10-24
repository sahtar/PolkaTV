package ru.polkatv.launcher.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale

/** dimStrength: 1.0 = medium (0.35/0.20/0.65 black), 0.5 = light. */
@Composable
fun WallpaperBackground(bitmap: ImageBitmap?, dimStrength: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color(0xFF060912))) {
        Crossfade(targetState = bitmap, animationSpec = tween(1500), label = "wallpaper") { bmp ->
            if (bmp != null) {
                Image(bitmap = bmp, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.35f * dimStrength),
                    0.4f to Color.Black.copy(alpha = 0.20f * dimStrength),
                    1f to Color.Black.copy(alpha = 0.65f * dimStrength),
                )
            )
        )
    }
}
