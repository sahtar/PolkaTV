package ru.polkatv.launcher.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

class AppsRepository(private val context: Context, private val scope: CoroutineScope) {
    private val pm: PackageManager = context.packageManager
    private val _apps = MutableStateFlow<List<TvApp>>(emptyList())
    val apps: StateFlow<List<TvApp>> = _apps.asStateFlow()

    @Volatile private var refreshJob: Job? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { refresh() }
    }

    fun start() {
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, f)
        refresh()
    }

    fun stop() {
        refreshJob?.cancel()
        runCatching { context.unregisterReceiver(receiver) }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            try {
                _apps.value = query()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("Polka", "apps query failed", e)
            }
        }
    }

    private suspend fun query(): List<TvApp> = withContext(Dispatchers.IO) {
        val self = context.packageName
        val leanback = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
        val launcher = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        val seen = HashSet<String>()
        val result = ArrayList<TvApp>()
        fun add(ri: ResolveInfo) {
            val pkg = ri.activityInfo.packageName
            if (pkg == self || !seen.add(pkg)) return
            val hasBanner = ri.activityInfo.banner != 0 || ri.activityInfo.applicationInfo.banner != 0
            result += TvApp(pkg, ri.loadLabel(pm).toString(), ri.activityInfo.let { android.content.ComponentName(it.packageName, it.name) }, hasBanner)
        }
        leanback.forEach(::add)
        launcher.forEach(::add)
        val collator = Collator.getInstance(Locale("ru"))
        result.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun launchIntent(packageName: String): Intent? =
        pm.getLeanbackLaunchIntentForPackage(packageName) ?: pm.getLaunchIntentForPackage(packageName)
}
