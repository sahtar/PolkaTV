package ru.polkatv.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import ru.polkatv.launcher.ui.home.WallpaperBackground
import ru.polkatv.launcher.ui.theme.PolkaTheme

@OptIn(ExperimentalTvMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as PolkaApplication).container
        setContent {
            PolkaTheme {
                val bmp by container.wallpapers.current.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize()) {
                    WallpaperBackground(bitmap = bmp, dimStrength = 1f)
                    Text(text = stringResource(R.string.hello_placeholder), fontSize = 64.sp, color = Color.White, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }

    override fun onStart() { super.onStart(); (application as PolkaApplication).container.wallpapers.start() }
    override fun onStop() { (application as PolkaApplication).container.wallpapers.stop(); super.onStop() }
    override fun onTrimMemory(level: Int) { super.onTrimMemory(level); (application as PolkaApplication).container.wallpapers.trimMemory() }
}
