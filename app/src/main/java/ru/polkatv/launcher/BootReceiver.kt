package ru.polkatv.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** Brings the home screen to the front after boot: the Xiaomi firmware otherwise leaves its PatchWall speedboot task on top. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val i = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        runCatching { context.startActivity(i) }
            .onFailure { Log.w("Polka", "boot start failed", it) }
        Log.i("Polka", "BootReceiver: requested home start")
    }
}
