package ru.polkatv.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * After the TV wakes from standby the Xiaomi firmware can put its PatchWall home task on top of Polka.
 * Third-party apps cannot see other apps' tasks, so the check is based on Polka's own visibility:
 * if the home screen was visible when the screen went off and is still not visible shortly after the
 * screen comes back on, something else (PatchWall) covered it — bring Polka back to the front.
 * If the user was in another app (YouTube, …) before standby, Polka was not visible and nothing happens.
 * Starting an activity from the background is allowed because the app holds SYSTEM_ALERT_WINDOW (see README).
 */
class WakeReceiver(private val visibility: HomeVisibility) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                visibility.visibleAtScreenOff = visibility.wasVisibleWithin(RECENT_MS)
                Log.i("Polka", "WakeReceiver: screen off, homeVisible=${visibility.visibleAtScreenOff}")
            }
            Intent.ACTION_SCREEN_ON -> {
                val app = context.applicationContext
                Handler(Looper.getMainLooper()).postDelayed({ bringHomeForwardIfCovered(app) }, SETTLE_MS)
            }
        }
    }

    private fun bringHomeForwardIfCovered(context: Context) {
        val covered = visibility.visibleAtScreenOff && !visibility.resumed
        Log.i("Polka", "WakeReceiver: screen on, visibleAtOff=${visibility.visibleAtScreenOff} resumedNow=${visibility.resumed}")
        if (!covered) return
        val i = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(i) }
            .onSuccess { Log.i("Polka", "WakeReceiver: brought home forward") }
            .onFailure { Log.w("Polka", "WakeReceiver: start failed", it) }
    }

    companion object {
        private const val SETTLE_MS = 2000L
        private const val RECENT_MS = 3000L

        fun register(context: Context, visibility: HomeVisibility): WakeReceiver {
            val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
            return WakeReceiver(visibility).also { context.registerReceiver(it, filter) }
        }
    }
}

/** Tracks whether [MainActivity] is resumed; updated from its lifecycle callbacks. */
class HomeVisibility {
    @Volatile var resumed: Boolean = false
        private set
    @Volatile private var lastPausedAt: Long = 0L
    @Volatile var visibleAtScreenOff: Boolean = false

    fun onResumed() { resumed = true }
    fun onPaused() { resumed = false; lastPausedAt = System.currentTimeMillis() }

    /** True if the home screen is resumed now or was paused less than [withinMs] ago (screen-off ordering varies). */
    fun wasVisibleWithin(withinMs: Long): Boolean = resumed || System.currentTimeMillis() - lastPausedAt < withinMs
}
