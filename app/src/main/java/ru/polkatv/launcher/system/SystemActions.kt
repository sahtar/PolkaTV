package ru.polkatv.launcher.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class SystemActions(private val context: Context) {
    /** Starts the activity if something handles the intent. Returns false otherwise (caller shows a message). */
    fun launch(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun openAppInfo(packageName: String): Boolean =
        launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
}
