package ru.polkatv.launcher.apps

import android.content.ComponentName

data class TvApp(
    val packageName: String,
    val label: String,
    val component: ComponentName,
    val hasBanner: Boolean,
)
