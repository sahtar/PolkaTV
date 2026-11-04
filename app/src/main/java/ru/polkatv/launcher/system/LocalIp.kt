package ru.polkatv.launcher.system

import java.net.Inet4Address
import java.net.NetworkInterface

object LocalIp {
    /** First site-local IPv4 address of an interface that is up (Wi-Fi/Ethernet preferred); null when offline. */
    fun find(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .sortedBy { if (it.name.startsWith("wlan") || it.name.startsWith("eth")) 0 else 1 }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()
}
