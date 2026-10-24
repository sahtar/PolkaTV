package ru.polkatv.launcher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val GlassFill = Color(0x73141828)      // rgba(20,24,40,0.45)
val GlassBorder = Color(0x2EFFFFFF)    // white 18%
val TextPrimary = Color.White
val TextSecondary = Color(0xCCFFFFFF)  // white 80%
val TextTertiary = Color(0x99FFFFFF)   // white 60%

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PolkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color(0xFF111111),
            surface = Color(0xFF0B1020),
            onSurface = TextPrimary,
            background = Color(0xFF060912),
            onBackground = TextPrimary,
        ),
        content = content,
    )
}
