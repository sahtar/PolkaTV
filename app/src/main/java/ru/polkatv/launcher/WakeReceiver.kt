package ru.polkatv.launcher

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * After the TV wakes from standby the Xiaomi firmware's PatchWall home task can end up on top of Polka.
 * On ACTION_SCREEN_ON, if the top task belongs to PatchWall, bring Polka (the HOME app) back to the front.
 * Other foreground apps (e.g. a video app the user was watching) are left alone.
 * Starting an activity from the background is allowed because the app holds SYSTEM_ALERT_WINDOW (see README).
 */
class WakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_SCREEN_ON) return
        val app = context.applicationContext
        // Give the system a moment to settle which task it restores.
        Handler(Looper.getMainLooper()).postDelayed({ bringHomeForwardIfPatchWallOnTop(app) }, 1500)
    }

    private fun bringHomeForwardIfPatchWallOnTop(context: Context) {
        val am = context.getSystemService(ActivityManager::class.java)
        val top = runCatching { @Suppress("DEPRECATION") am.getRunningTasks(1).firstOrNull()?.topActivity }.getOrNull()
        Log.i("Polka", "WakeReceiver: top=${top?.flattenToShortString()}")
        if (top?.packageName != PATCHWALL) return
        val i = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(i) }
            .onSuccess { Log.i("Polka", "WakeReceiver: brought home forward") }
            .onFailure { Log.w("Polka", "WakeReceiver: start failed", it) }
    }

    companion object {
        private const val PATCHWALL = "com.mitv.tvhome.atv"
        fun register(context: Context): WakeReceiver =
            WakeReceiver().also { context.registerReceiver(it, IntentFilter(Intent.ACTION_SCREEN_ON)) }
    }
}
