package ru.polkatv.launcher.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetState { NONE, WIFI, ETHERNET }

class NetworkStatus(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val _state = MutableStateFlow(current())
    val state: StateFlow<NetState> = _state.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { _state.value = current() }
        override fun onLost(network: Network) { _state.value = current() }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { _state.value = toState(caps) }
    }

    fun start() { runCatching { cm.registerDefaultNetworkCallback(callback) }; _state.value = current() }
    fun stop() { runCatching { cm.unregisterNetworkCallback(callback) } }

    private fun current(): NetState = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }?.let(::toState) ?: NetState.NONE

    private fun toState(caps: NetworkCapabilities): NetState = when {
        !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetState.NONE
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetState.ETHERNET
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetState.WIFI
        else -> NetState.WIFI
    }
}
