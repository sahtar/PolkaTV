package ru.polkatv.launcher

import android.app.Application

class PolkaApplication : Application() {
    lateinit var container: AppContainer
        private set

    val homeVisibility = HomeVisibility()

    // Lives as long as the process; intentionally never unregistered.
    private var wakeReceiver: WakeReceiver? = null

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        wakeReceiver = WakeReceiver.register(this, homeVisibility)
    }
}
