package ir.hooshamoozan.chatyar.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.nekohasekai.libbox.*
import ir.hooshamoozan.chatyar.ChatyarApplication
import ir.hooshamoozan.chatyar.MainActivity
import ir.hooshamoozan.chatyar.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetSocketAddress
import java.net.NetworkInterface as JavaNetworkInterface

/**
 * Real in-process VPN service: Android TUN FD is handed to the libbox core.
 * Only Chatyar traffic enters the VPN; no system-wide routing is enabled.
 * Requires VpnService.prepare() consent in the Activity before start.
 */
class ChatyarVpnService : VpnService() {
    companion object {
        const val ACTION_START = "ir.hooshamoozan.chatyar.vpn.START"
        const val ACTION_STOP = "ir.hooshamoozan.chatyar.vpn.STOP"
        const val EXTRA_PROFILE = "profile_id"
        const val CHANNEL = "chatyar_vpn"
        private const val NOTIFICATION_ID = 2119
        private const val MAX_CONFIG_LENGTH = 256 * 1024
        @Volatile private var initialized = false

        fun start(context: Context, profileId: String) {
            val intent = Intent(context, ChatyarVpnService::class.java)
                .setAction(ACTION_START).putExtra(EXTRA_PROFILE, profileId)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, ChatyarVpnService::class.java).setAction(ACTION_STOP))
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private var server: CommandServer? = null
    private var tun: ParcelFileDescriptor? = null
    private var connectivity: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var currentListener: InterfaceUpdateListener? = null
    @Volatile private var stopping = false
    private val lock = Any()

    override fun onCreate() {
        super.onCreate()
        connectivity = getSystemService(ConnectivityManager::class.java)
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL, "Chatyar VPN", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopEngine(); stopSelf(); return START_NOT_STICKY
        }
        // Android requires foreground promotion promptly after startForegroundService().
        showNotification("در حال اتصال…")
        val id = intent?.getStringExtra(EXTRA_PROFILE)
        if (id.isNullOrBlank() || VpnService.prepare(this) != null) {
            VpnStatus.setError("مجوز VPN دریافت نشده یا پروفایل نامعتبر است")
            stopEngine(); stopSelf(); return START_NOT_STICKY
        }
        scope.launch {
            try {
                if (server != null || tun != null) stopEngine()
                stopping = false
                VpnStatus.setConnecting(id)
                val config = (application as ChatyarApplication).container.vpnProfiles.config(id)
                require(config.isNotBlank() && config.length <= MAX_CONFIG_LENGTH) { "پروفایل موجود نیست یا نامعتبر است" }
                setupCore()
                val json = VpnConfigParser.singBoxConfig(config)
                Libbox.checkConfig(json)
                val s = CommandServer(object : CommandServerHandler {
                    override fun serviceStop() {
                        if (!stopping) VpnStatus.setError("تونل متوقف شد")
                    }
                    override fun serviceReload() = Unit
                }, PlatformBridge())
                synchronized(lock) { server = s }
                s.start()
                s.startOrReloadService(json, OverrideOptions())
                if (tun == null) error("هسته VPN رابط TUN را راه‌اندازی نکرد")
                VpnStatus.setConnected(id)
                showNotification("تونل چتیار متصل است")
            } catch (e: Exception) {
                // Do not echo config, URLs or secrets in exception messages.
                VpnStatus.setError("اتصال ناموفق بود. ساختار کانفیگ و دسترسی سرور را بررسی کن.")
                android.util.Log.w("ChatyarVPN", "VPN start failed (${e.javaClass.simpleName})")
                stopEngine(); stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun setupCore() {
        // Process-wide once, safe on reconnect.
        synchronized(ChatyarVpnService::class.java) {
            if (initialized) return
            val base = File(filesDir, "sing-box").apply { mkdirs() }
            val work = File(cacheDir, "sing-box").apply { mkdirs() }
            val temp = File(cacheDir, "sing-box-tmp").apply { mkdirs() }
            Libbox.setup(SetupOptions().apply {
                basePath = base.absolutePath
                workingPath = work.absolutePath
                tempPath = temp.absolutePath
                commandServerListenPort = 0 // local Unix-domain control, no public port
                logMaxLines = 60
                debug = false
                fixAndroidStack = true
                appVersion = "3"
                appMarketingVersion = "0.3.1"
            })
            initialized = true
        }
    }

    private fun showNotification(message: String) {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, ChatyarVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_chatyar_logo)
            .setContentTitle("Chatyar VPN")
            .setContentText(message)
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "قطع اتصال", stop)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopEngine() {
        stopping = true
        synchronized(lock) {
            runCatching { server?.closeService() }
            runCatching { server?.close() }
            server = null
            runCatching { tun?.close() }
            tun = null
        }
        stopMonitor()
        if (VpnStatus.state.value !is TunnelStatus.Failed) VpnStatus.setDisconnected()
    }

    override fun onRevoke() { stopEngine(); stopSelf(); super.onRevoke() }
    override fun onDestroy() { stopEngine(); scope.cancel(); super.onDestroy() }

    private fun activePhysicalNetwork(): Network? {
        val cm = connectivity ?: return null
        val networks = cm.allNetworks.filter { network ->
            cm.getNetworkCapabilities(network)?.let { caps ->
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    !caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            } ?: false
        }
        return networks.firstOrNull { cm.getNetworkCapabilities(it)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true } ?: networks.firstOrNull()
    }

    private fun updateInterface() {
        val listener = currentListener ?: return
        val cm = connectivity ?: return
        val link = activePhysicalNetwork()?.let { cm.getLinkProperties(it) }
        val name = link?.interfaceName.orEmpty()
        val index = runCatching { JavaNetworkInterface.getByName(name)?.index ?: -1 }.getOrDefault(-1)
        runCatching { listener.updateDefaultInterface(name, index, false, false) }
    }

    private fun startMonitor(listener: InterfaceUpdateListener) {
        currentListener = listener
        val cm = connectivity ?: return
        if (networkCallback == null) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = updateInterface()
                override fun onLost(network: Network) = updateInterface()
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = updateInterface()
            }
            cm.registerNetworkCallback(NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build(), callback)
            networkCallback = callback
        }
        updateInterface()
    }

    private fun stopMonitor() {
        networkCallback?.let { callback ->
            runCatching { connectivity?.unregisterNetworkCallback(callback) }
        }
        networkCallback = null
        currentListener = null
    }

    private class Strings(private val values: Iterator<String>) : StringIterator {
        override fun len(): Int = 0
        override fun hasNext(): Boolean = values.hasNext()
        override fun next(): String = values.next()
    }
    private class Interfaces(private val values: Iterator<io.nekohasekai.libbox.NetworkInterface>) : NetworkInterfaceIterator {
        override fun hasNext(): Boolean = values.hasNext()
        override fun next(): io.nekohasekai.libbox.NetworkInterface = values.next()
    }
    private fun RoutePrefixIterator?.entries(): List<Pair<String, Int>> {
        if (this == null) return emptyList()
        val list = mutableListOf<Pair<String, Int>>()
        while (hasNext()) { val p = next(); list.add(p.address() to p.prefix()) }
        return list
    }

    private inner class PlatformBridge : PlatformInterface {
        override fun openTun(options: TunOptions): Int {
            val builder = Builder()
                .setSession("Chatyar • Internal VPN")
                .setMtu(options.mtu.coerceIn(1280, 9000))
                .addAddress("172.19.0.1", 30)
                .addAddress("fdfe:dcba:9876::1", 126)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("2606:4700:4700::1111")
            // App-only: no traffic from other applications enters the tunnel.
            builder.addAllowedApplication(packageName)
            val fd = builder.establish() ?: throw IllegalStateException("VpnService.establish failed")
            synchronized(lock) {
                tun?.close(); tun = fd
            }
            return fd.fd
        }
        override fun usePlatformAutoDetectInterfaceControl(): Boolean = true
        override fun autoDetectInterfaceControl(fd: Int) {
            if (!protect(fd)) throw IllegalStateException("Socket protect failed")
        }
        override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) = startMonitor(listener)
        override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) = stopMonitor()
        override fun getInterfaces(): NetworkInterfaceIterator {
            val cm = connectivity ?: return Interfaces(emptyList<io.nekohasekai.libbox.NetworkInterface>().iterator())
            val result = cm.allNetworks.mapNotNull { network ->
                val link = cm.getLinkProperties(network) ?: return@mapNotNull null
                val caps = cm.getNetworkCapabilities(network) ?: return@mapNotNull null
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return@mapNotNull null
                val sys = JavaNetworkInterface.getByName(link.interfaceName) ?: return@mapNotNull null
                io.nekohasekai.libbox.NetworkInterface().apply {
                    name = link.interfaceName
                    index = sys.index
                    type = when {
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Libbox.InterfaceTypeWIFI
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Libbox.InterfaceTypeCellular
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Libbox.InterfaceTypeEthernet
                        else -> Libbox.InterfaceTypeOther
                    }
                    dnsServer = Strings(link.dnsServers.mapNotNull { it.hostAddress }.iterator())
                    gateway = Strings(link.routes.filter { it.destination.prefixLength == 0 }
                        .mapNotNull { it.gateway?.hostAddress }.iterator())
                    addresses = Strings(sys.interfaceAddresses.map { "${it.address.hostAddress}/${it.networkPrefixLength}" }.iterator())
                    runCatching { mtu = sys.mtu }
                    flags = OsConstants.IFF_UP or OsConstants.IFF_RUNNING
                    metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                }
            }
            return Interfaces(result.iterator())
        }
        override fun useProcFS(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        override fun findConnectionOwner(ipProtocol: Int, sourceAddress: String, sourcePort: Int,
                                         destinationAddress: String, destinationPort: Int): ConnectionOwner {
            if (Build.VERSION.SDK_INT < 29) error("not available on Android 8/9")
            val uid = connectivity?.getConnectionOwnerUid(ipProtocol,
                InetSocketAddress(sourceAddress, sourcePort), InetSocketAddress(destinationAddress, destinationPort))
                ?: Process.INVALID_UID
            if (uid == Process.INVALID_UID) error("owner unavailable")
            return ConnectionOwner().apply { userId = uid; userName = "" }
        }
        override fun localDNSTransport(): LocalDNSTransport = object : LocalDNSTransport {
            override fun raw(): Boolean = false
            override fun exchange(ctx: ExchangeContext, message: ByteArray) { ctx.errorCode(2) }
            override fun lookup(ctx: ExchangeContext, network: String, domain: String) {
                try {
                    val underlying = activePhysicalNetwork() ?: error("no underlying network")
                    val result = underlying.getAllByName(domain)
                    ctx.success(result.mapNotNull { it.hostAddress }.joinToString("\n"))
                } catch (_: Exception) { ctx.errorCode(2) }
            }
        }
        override fun underNetworkExtension(): Boolean = false
        override fun includeAllNetworks(): Boolean = false
        override fun clearDNSCache() = Unit
        override fun readWIFIState(): WIFIState? = null
        override fun sendNotification(notification: io.nekohasekai.libbox.Notification) = Unit
        override fun cancelNotification(identifier: String, typeID: Int) = Unit
        override fun registerMyInterface(name: String?) = Unit
        override fun usePlatformBridge(): Boolean = false
        override fun createBridge(options: BridgeOptions?): BridgeSession = error("unsupported")
        override fun usePlatformShell(): Boolean = false
        override fun checkPlatformShell() = error("unsupported")
        override fun openShellSession(user: PlatformUser?, command: String?, environ: StringIterator?, term: String?,
                                      rows: Int, cols: Int): ShellSession = error("unsupported")
        override fun lookupUser(username: String?): PlatformUser = error("unsupported")
        override fun lookupSFTPServer(): String = error("unsupported")
        override fun readSystemSSHHostKey(): String = error("unsupported")
        override fun tailscaleHostname(): String = error("unsupported")
        override fun startNeighborMonitor(listener: NeighborUpdateListener?) = Unit
        override fun closeNeighborMonitor(listener: NeighborUpdateListener?) = Unit
    }

}
