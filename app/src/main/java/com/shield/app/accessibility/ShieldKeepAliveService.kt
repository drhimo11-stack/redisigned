package com.shield.app.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationCompat
import com.shield.app.MainActivity
import com.shield.app.R
import com.shield.app.lock.LockManager

class ShieldKeepAliveService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastKnownAccessibilityEnabled: Boolean? = null

    private val watchdogTick = object : Runnable {
        override fun run() {
            checkProtectionStatus()
            handler.postDelayed(this, WATCHDOG_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
        setWasActive(true)
        handler.post(watchdogTick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(watchdogTick)
        setWasActive(false)
    }

    private fun setWasActive(active: Boolean) {
        getSharedPreferences("shield_state", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WAS_ACTIVE, active)
            .apply()
    }

    /**
     * Watches for the Accessibility Service getting disabled — by the OS
     * killing it, or by the user turning it off — since that silently
     * stops all content blocking even while a lock is "active" and Device
     * Admin is still enabled. Only alerts on the transition (enabled ->
     * disabled) while locked, so it doesn't spam a notification every
     * check interval.
     */
    private fun checkProtectionStatus() {
        val enabled = isAccessibilityServiceEnabled()
        val previouslyEnabled = lastKnownAccessibilityEnabled
        lastKnownAccessibilityEnabled = enabled

        val locked = LockManager.get(this).isLocked()

        if (!enabled && locked && previouslyEnabled != false) {
            showProtectionAlert()
        } else if (enabled) {
            // Protection is back — clear any standing alert.
            val manager = getSystemService(NotificationManager::class.java)
            manager?.cancel(ALERT_NOTIF_ID)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName &&
                it.resolveInfo.serviceInfo.name == ShieldAccessibilityService::class.java.name
        }
    }

    private fun showProtectionAlert() {
        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            1,
            settingsIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("حصن protection turned off")
            .setContentText("Accessibility was disabled while a lock is active. Tap to re-enable.")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(ALERT_NOTIF_ID, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "حصن Protection",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setShowBadge(false)
                    description = "Keeps حصن's protection running in the background"
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "Protection Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts you if protection is turned off while locked"
                }
            )
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("حصن is active")
            .setContentText("Protection is running")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "shield_protection"
        const val ALERT_CHANNEL_ID = "shield_protection_alerts"
        const val NOTIF_ID = 1001
        const val ALERT_NOTIF_ID = 1002
        const val KEY_WAS_ACTIVE = "was_active"
        private const val WATCHDOG_INTERVAL_MS = 30_000L

        fun start(context: Context) {
            val intent = Intent(context, ShieldKeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ShieldKeepAliveService::class.java))
        }

        fun wasActive(context: Context): Boolean {
            return context.getSharedPreferences("shield_state", Context.MODE_PRIVATE)
                .getBoolean(KEY_WAS_ACTIVE, false)
        }
    }
}
