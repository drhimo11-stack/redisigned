package com.shield.app.accessibility

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.shield.app.R
import com.shield.app.blocklist.ManagedAppManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires whenever a new app is installed on the device. If the new app can
 * open web links (i.e. behaves like a browser — this also catches most
 * "downloader with a built-in browser" apps such as Snaptube/Vidmate-style
 * apps, since they typically register an ACTION_VIEW handler for their own
 * in-app browsing), it's added to the managed apps list and blocked by
 * default, the same as a built-in supported browser. Apps that don't
 * register as link-openers (rare for a genuine downloader) aren't
 * detectable this way — those can still be added manually from the
 * "Manage Apps" screen.
 */
class NewAppInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_ADDED) return
        // Ignore package updates/replacements — this should only react to
        // genuinely new installs, not the app being updated in place.
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleNewPackage(context, packageName)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleNewPackage(context: Context, packageName: String) {
        val pm = context.packageManager

        // Already explicitly known (built-in browser list) — no need to
        // duplicate it into the user-managed table.
        if (BrowserList.isSupportedBrowser(packageName)) return

        val managedAppManager = ManagedAppManager.get(context)
        if (managedAppManager.repository.exists(packageName)) return

        if (!BrowserDetection.isBrowserCapable(pm, packageName)) return

        val label = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        managedAppManager.add(packageName, label, autoDetected = true)
        notifyAutoAdded(context, label)
    }

    private fun notifyAutoAdded(context: Context, appLabel: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "New App Detected",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts when a newly installed browser or downloader is added to blocking"
                }
            )
            val contentIntent = Intent(Intent.ACTION_MAIN).apply {
                setPackage(context.packageName)
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 2, contentIntent, PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("New browser detected")
                .setContentText("\"$appLabel\" was installed and added to blocking automatically.")
                .setSmallIcon(R.drawable.ic_shield)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            manager.notify(NOTIF_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "shield_new_app_detected"
        private const val NOTIF_ID = 1003
    }
}
