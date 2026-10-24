package ru.polkatv.launcher.wallpaper

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.random.Random

private class FakeBitmap : ImageBitmap {
    override val width = 1
    override val height = 1
    override val config = ImageBitmapConfig.Argb8888
    override val hasAlpha = false
    override val colorSpace: ColorSpace = ColorSpaces.Srgb
    override fun readPixels(buffer: IntArray, startX: Int, startY: Int, width: Int, height: Int, bufferOffset: Int, stride: Int) = Unit
    override fun prepareToDraw() = Unit
}

private class FakeSource(
    override val count: Int = 5,
    private val failFirst: Int = 0,
) : FrameSource {
    var calls = 0
    override suspend fun load(index: Int): ImageBitmap {
        calls++
        if (calls <= failFirst) error("cannot decode")
        return FakeBitmap()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WallpaperControllerTest {
    private val minute = 60_000L

    @Test fun `interval zero never swaps`() = runTest {
        val interval = MutableStateFlow(0)
        val c = WallpaperController(FakeSource(), interval, backgroundScope, Random(1))
        c.start(); runCurrent()
        val first = c.current.value
        assertNotNull(first)
        advanceTimeBy(600 * minute)
        assertSame(first, c.current.value)
    }

    @Test fun `changing interval from 60 to 1 swaps within about a minute`() = runTest {
        val interval = MutableStateFlow(60)
        val c = WallpaperController(FakeSource(), interval, backgroundScope, Random(1))
        c.start(); runCurrent()
        val first = c.current.value
        advanceTimeBy(5 * minute)
        assertSame(first, c.current.value)
        interval.value = 1
        advanceTimeBy(minute + 1_000); runCurrent()
        assertNotSame(first, c.current.value)
    }

    @Test fun `setting interval to zero mid wait cancels the pending swap`() = runTest {
        val interval = MutableStateFlow(1)
        val c = WallpaperController(FakeSource(), interval, backgroundScope, Random(1))
        c.start(); runCurrent()
        val first = c.current.value
        advanceTimeBy(30_000)
        interval.value = 0
        advanceTimeBy(600 * minute)
        assertSame(first, c.current.value)
    }

    @Test fun `going from zero to one resumes rotation promptly`() = runTest {
        val interval = MutableStateFlow(0)
        val c = WallpaperController(FakeSource(), interval, backgroundScope, Random(1))
        c.start(); runCurrent()
        val first = c.current.value
        advanceTimeBy(10 * minute)
        interval.value = 1
        advanceTimeBy(minute + 1_000); runCurrent()
        assertNotSame(first, c.current.value)
    }

    @Test fun `failing first load leaves current null and does not throw`() = runTest {
        val interval = MutableStateFlow(1)
        val c = WallpaperController(FakeSource(failFirst = 2), interval, backgroundScope, Random(1))
        c.start(); runCurrent()
        assertNull(c.current.value)
    }

    @Test fun `first load is retried once with another frame`() = runTest {
        val interval = MutableStateFlow(1)
        val c = WallpaperController(FakeSource(failFirst = 1), interval, backgroundScope, Random(1))
        c.start(); runCurrent()
        assertNotNull(c.current.value)
    }
}
