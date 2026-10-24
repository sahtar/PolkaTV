package ru.polkatv.launcher.wallpaper

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

/**
 * Keeps at most two decoded frames (current + preloaded next). intervalMinutes == 0 disables rotation.
 * Changes of intervalMinutes take effect immediately, also while waiting for the next swap.
 */
class WallpaperController(
    private val loader: FrameSource,
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
            // On failure try one more (different) frame, otherwise keep the dark background.
            if (_current.value == null) _current.value = loadFrame() ?: loadFrame()
            while (isActive) {
                val minutes = intervalMinutes.value
                if (minutes <= 0) {
                    intervalMinutes.first { it != minutes }
                    continue
                }
                if (next == null) next = loadFrame()
                val changed = withTimeoutOrNull(minutes * 60_000L) { intervalMinutes.first { it != minutes } }
                if (changed != null || intervalMinutes.value <= 0) continue
                val n = next ?: continue
                next = null
                _current.value = n
            }
        }
    }

    fun stop() { job?.cancel(); job = null }

    /** Called from Activity.onTrimMemory: drop the preloaded frame. */
    fun trimMemory() { next = null }

    private suspend fun loadFrame(): ImageBitmap? =
        try {
            loader.load(sequence.next())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            null
        }
}
