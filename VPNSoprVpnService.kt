package com.vpnsopr.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.OverrideOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VPNSoprVpnService : VpnService() {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    @Volatile private var commandServer: CommandServer? = null
    @Volatile private var tunDescriptor: ParcelFileDescriptor? = null
    @Volatile private var running = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            executor.execute { stopTunnel() }
            return START_NOT_STICKY
        }

        val config = intent?.getStringExtra(EXTRA_CONFIG)
        if (config.isNullOrBlank()) {
            publishState("ERROR", "هیچ کانفیگی انتخاب نشده است")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("در حال آماده‌سازی اتصال…"))
        publishState("CONNECTING", "در حال اتصال به سرور")

        executor.execute {
            try {
                stopTunnelInternal(publish = false)
                val platform = LibboxProxyFactory.platform(this) { descriptor -> tunDescriptor = descriptor }
                val handler = LibboxProxyFactory.commandHandler { executor.execute { stopTunnel() } }
                val server = CommandServer(handler, platform)
                commandServer = server
                server.start()
                server.startOrReloadService(config, OverrideOptions())
                running = true
                publishState("CONNECTED", "اتصال برقرار شد")
                updateNotification("اتصال VPN برقرار است")
            } catch (error: Throwable) {
                Log.e(TAG, "VPN start failed", error)
                stopTunnelInternal(publish = false)
                val message = error.message?.take(180) ?: "خطای نامشخص در راه‌اندازی هسته VPN"
                publishState("ERROR", message)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onRevoke() {
        executor.execute { stopTunnel() }
        super.onRevoke()
    }

    override fun onDestroy() {
        runCatching { stopTunnelInternal(publish = false) }
        executor.shutdown()
        super.onDestroy()
    }

    private fun stopTunnel() {
        stopTunnelInternal(publish = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @Synchronized
    private fun stopTunnelInternal(publish: Boolean) {
        running = false
        val descriptor = tunDescriptor
        tunDescriptor = null
        runCatching { descriptor?.close() }
        val server = commandServer
        commandServer = null
        if (server != null) {
            runCatching { server.closeService() }
                .onFailure { Log.w(TAG, "closeService: ${it.message}") }
        }
        if (server != null) {
            runCatching { server.close() }
                .onFailure { Log.w(TAG, "commandServer.close: ${it.message}") }
        }
        if (publish) publishState("DISCONNECTED", "اتصال قطع شد")
    }

    private fun publishState(state: String, message: String) {
        getSharedPreferences("vpnsopr_runtime", MODE_PRIVATE).edit().putString("state", state).putString("message", message).apply()
        sendBroadcast(Intent(ACTION_STATE).setPackage(packageName)
            .putExtra(EXTRA_STATE, state)
            .putExtra(EXTRA_MESSAGE, message))
    }

    private fun buildNotification(text: String): Notification {
        val stopIntent = Intent(this, VPNSoprVpnService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this, 21, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this, 22, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vpn)
            .setContentTitle("VPNSOPR")
            .setContentText(text)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_stat_vpn, "قطع اتصال", stopPending)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "VPNSOPR VPN", NotificationManager.IMPORTANCE_LOW).apply {
                description = "وضعیت اتصال امن VPNSOPR"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "VPNSOPR-VPN"
        private const val CHANNEL_ID = "vpnsopr_vpn_status"
        private const val NOTIFICATION_ID = 7401
        const val ACTION_START = "com.vpnsopr.client.action.START"
        const val ACTION_STOP = "com.vpnsopr.client.action.STOP"
        const val ACTION_STATE = "com.vpnsopr.client.action.STATE"
        const val EXTRA_CONFIG = "config_json"
        const val EXTRA_STATE = "vpn_state"
        const val EXTRA_MESSAGE = "vpn_message"

        fun stop(context: android.content.Context) {
            val intent = Intent(context, VPNSoprVpnService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
