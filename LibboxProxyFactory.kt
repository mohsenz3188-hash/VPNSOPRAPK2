package com.vpnsopr.client

import android.net.VpnService
import android.net.IpPrefix
import android.net.ProxyInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.SystemProxyStatus
import io.nekohasekai.libbox.TunOptions
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

internal object LibboxProxyFactory {
    private const val TAG = "VPNSOPR-Libbox"

    fun platform(service: VpnService, onTunEstablished: (ParcelFileDescriptor) -> Unit): PlatformInterface {
        val handler = InvocationHandler { _, method, args ->
            when (method.name) {
                "usePlatformAutoDetectInterfaceControl" -> true
                "autoDetectInterfaceControl" -> {
                    val fd = (args?.getOrNull(0) as? Number)?.toInt() ?: -1
                    if (fd >= 0) service.protect(fd)
                    null
                }
                "openTun" -> openTun(service, args?.getOrNull(0) as TunOptions, onTunEstablished)
                "getInterfaces" -> emptyNetworkInterfaces()
                "underNetworkExtension", "includeAllNetworks" -> false
                "usePlatformShell", "usePlatformBridge", "usePlatformAutoRedirect" -> false
                "tailscaleHostname" -> "VPNSOPR-Android"
                "lookupSFTPServer", "readSystemSSHHostKey" -> ""
                "readWIFIState", "localDNSTransport", "findConnectionOwner", "lookupUser", "createBridge", "createAutoRedirect", "openShellSession" -> null
                "checkPlatformShell", "clearDNSCache", "registerMyInterface", "startNeighborMonitor", "closeNeighborMonitor", "startDefaultInterfaceMonitor", "closeDefaultInterfaceMonitor", "sendNotification", "cancelNotification" -> null
                "toString" -> "VPNSOPRPlatformInterface"
                "hashCode" -> System.identityHashCode(service)
                "equals" -> service === args?.getOrNull(0)
                else -> defaultValue(method.returnType)
            }
        }
        return Proxy.newProxyInstance(
            PlatformInterface::class.java.classLoader,
            arrayOf(PlatformInterface::class.java),
            handler,
        ) as PlatformInterface
    }

    fun commandHandler(onStop: () -> Unit): CommandServerHandler {
        val handler = InvocationHandler { _, method, args ->
            when (method.name) {
                "serviceStop" -> { onStop(); null }
                "serviceReload" -> null
                "getSystemProxyStatus" -> SystemProxyStatus()
                "setSystemProxyEnabled", "triggerNativeCrash" -> null
                "writeDebugMessage" -> {
                    Log.d(TAG, args?.getOrNull(0)?.toString().orEmpty())
                    null
                }
                "connectSSHAgent" -> -1
                "toString" -> "VPNSOPRCommandServerHandler"
                "hashCode" -> System.identityHashCode(onStop)
                "equals" -> this === args?.getOrNull(0)
                else -> defaultValue(method.returnType)
            }
        }
        return Proxy.newProxyInstance(
            CommandServerHandler::class.java.classLoader,
            arrayOf(CommandServerHandler::class.java),
            handler,
        ) as CommandServerHandler
    }

    private fun openTun(
        service: VpnService,
        options: TunOptions,
        onTunEstablished: (ParcelFileDescriptor) -> Unit,
    ): Int {
        val builder = service.Builder()
            .setSession("VPNSOPR")
            .setMtu(options.mtu)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) builder.setMetered(false)

        val ipv4 = options.inet4Address
        while (ipv4.hasNext()) {
            val address = ipv4.next()
            builder.addAddress(address.address(), address.prefix())
        }

        val ipv6 = options.inet6Address
        while (ipv6.hasNext()) {
            val address = ipv6.next()
            builder.addAddress(address.address(), address.prefix())
        }

        if (options.autoRoute) {
            if (options.dnsMode.value != Libbox.DNSModeDisabled) {
                val dns = options.dnsServerAddress
                while (dns.hasNext()) builder.addDnsServer(dns.next())
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val route4 = options.inet4RouteAddress
                if (route4.hasNext()) {
                    while (route4.hasNext()) builder.addRoute(IpPrefix(route4.next()))
                } else if (options.inet4Address.hasNext()) {
                    builder.addRoute("0.0.0.0", 0)
                }

                val route6 = options.inet6RouteAddress
                if (route6.hasNext()) {
                    while (route6.hasNext()) builder.addRoute(IpPrefix(route6.next()))
                } else if (options.inet6Address.hasNext()) {
                    builder.addRoute("::", 0)
                }

                val exclude4 = options.inet4RouteExcludeAddress
                while (exclude4.hasNext()) builder.excludeRoute(IpPrefix(exclude4.next()))
                val exclude6 = options.inet6RouteExcludeAddress
                while (exclude6.hasNext()) builder.excludeRoute(IpPrefix(exclude6.next()))
            } else {
                val route4 = options.inet4RouteRange
                var hasRoute4 = false
                while (route4.hasNext()) {
                    val address = route4.next()
                    builder.addRoute(address.address(), address.prefix())
                    hasRoute4 = true
                }
                if (!hasRoute4 && options.inet4Address.hasNext()) builder.addRoute("0.0.0.0", 0)

                val route6 = options.inet6RouteRange
                var hasRoute6 = false
                while (route6.hasNext()) {
                    val address = route6.next()
                    builder.addRoute(address.address(), address.prefix())
                    hasRoute6 = true
                }
                if (!hasRoute6 && options.inet6Address.hasNext()) builder.addRoute("::", 0)
            }

            val include = options.includePackage
            while (include.hasNext()) {
                val packageName = include.next()
                try {
                    builder.addAllowedApplication(packageName)
                } catch (_: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "Allowed application not found: $packageName")
                }
            }
            val exclude = options.excludePackage
            while (exclude.hasNext()) {
                val packageName = exclude.next()
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (_: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "Disallowed application not found: $packageName")
                }
            }
        }

        if (options.isHTTPProxyEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bypass = mutableListOf<String>()
            val bypassIterator = options.httpProxyBypassDomain
            while (bypassIterator.hasNext()) bypass.add(bypassIterator.next())
            builder.setHttpProxy(ProxyInfo.buildDirectProxy(options.httpProxyServer, options.httpProxyServerPort, bypass))
        }

        val descriptor = builder.establish() ?: error("Android did not establish the VPN interface")
        onTunEstablished(descriptor)
        return descriptor.fd
    }

    private fun emptyNetworkInterfaces(): NetworkInterfaceIterator {
        val handler = InvocationHandler { _, method, _ ->
            when (method.name) {
                "hasNext" -> false
                "next" -> null
                "toString" -> "EmptyNetworkInterfaceIterator"
                "hashCode" -> System.identityHashCode(this)
                "equals" -> false
                else -> defaultValue(method.returnType)
            }
        }
        return Proxy.newProxyInstance(
            NetworkInterfaceIterator::class.java.classLoader,
            arrayOf(NetworkInterfaceIterator::class.java),
            handler,
        ) as NetworkInterfaceIterator
    }

    private fun defaultValue(type: Class<*>): Any? = when (type) {
        java.lang.Boolean.TYPE -> false
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Short.TYPE -> 0.toShort()
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        java.lang.Character.TYPE -> '\u0000'
        java.lang.Void.TYPE -> null
        else -> null
    }
}
