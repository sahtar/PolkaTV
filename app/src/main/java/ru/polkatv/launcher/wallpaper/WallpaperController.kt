package ru.polkatv.launcher.wallpaper

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Keeps at most two decoded frames (current + preloaded next). intervalMinutes == 0 disables rotation.
 */
class WallpaperController(
    private val loader: WallpaperLoader,
    private val intervalMinutes: StateFlow<Int>,
    private val scope: CoroutineScope,
    random: Random = Random.Default,
) {
    private val sequence = WallpaperSequence(loader.count.coerceAtLeast(1), random)
    private val _current = MutableStateFlow<ImageBitmap?>(null)
    val current: StateFlow<ImageBitmap?> = _current.asStateFlow()

    private var next: ImageBitmap? = null
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true || loader.count == 0) return
        job = scope.launch {
            if (_current.value == null) _current.value = loader.load(sequence.next())
            while (isActive) {
                val minutes = intervalMinutes.value
                if (minutes <= 0) { delay(30_000); continue }
                if (next == null) next = runCatching { loader.load(sequence.next()) }.getOrNull()
                delay(minutes * 60_000L)
                val n = next ?: continue
                next = null
                _current.value = n
            }
        }
    }

    fun stop() { job?.cancel(); job = null }

    /** Called from Activity.onTrimMemory: drop the preloaded frame. */
    fun trimMemory() { next = null }
}
