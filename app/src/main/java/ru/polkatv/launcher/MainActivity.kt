package ru.polkatv.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.polkatv.launcher.ui.PolkaApp
import ru.polkatv.launcher.ui.theme.PolkaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as PolkaApplication).container
        setContent { PolkaTheme { PolkaApp(container) } }
    }

    override fun onResume() { super.onResume(); (application as PolkaApplication).homeVisibility.onResumed() }
    override fun onPause() { (application as PolkaApplication).homeVisibility.onPaused(); super.onPause() }
    override fun onStart() { super.onStart(); (application as PolkaApplication).container.wallpapers.start(); (application as PolkaApplication).container.apps.start(); (application as PolkaApplication).container.network.start() }
    override fun onStop() { (application as PolkaApplication).container.wallpapers.stop(); (application as PolkaApplication).container.apps.stop(); (application as PolkaApplication).container.network.stop(); super.onStop() }
    override fun onTrimMemory(level: Int) { super.onTrimMemory(level); (application as PolkaApplication).container.wallpapers.trimMemory(); (application as PolkaApplication).container.tileArt.trim() }
}
