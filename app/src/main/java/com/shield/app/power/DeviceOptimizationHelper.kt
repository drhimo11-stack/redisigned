package com.shield.app.power

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Helpers for the two most common reasons a background/accessibility
 * service silently stops working on real devices: system battery
 * optimization killing the app, and OEM-specific "autostart"/"protected
 * apps" lists blocking it from running after a reboot. Android has no
 * single official API for the second one — each manufacturer ships its
 * own settings screen — so this tries the known component names and
 * falls back gracefully if none resolve.
 */
object DeviceOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Opens the system dialog to request battery optimization exemption. */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            openAppSettings(context)
        }
    }

    private fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    // Known OEM "autostart" / "protected apps" screens, keyed by the
    // manufacturer string reported in Build.MANUFACTURER (lowercase).
    // These change across OEM software versions without notice, so every
    // attempt is verified with resolveActivity before use.
    private val OEM_AUTOSTART_COMPONENTS: Map<String, List<ComponentName>> = mapOf(
        "xiaomi" to listOf(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        ),
        "oppo" to listOf(
            ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            ),
            ComponentName(
                "com.oppo.safe",
                "com.oppo.safe.permission.startup.StartupAppListActivity"
            )
        ),
        "vivo" to listOf(
            ComponentName(
                "com.vivo.permissionmanager",
                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
            )
        ),
        "huawei" to listOf(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        ),
        "honor" to listOf(
            ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        ),
        "samsung" to listOf(
            ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.ui.battery.BatteryActivity"
            )
        ),
        "oneplus" to listOf(
            ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            )
        )
    )

    /**
     * Returns an Intent to the current manufacturer's autostart/protected-
     * apps settings screen, or null if this device's manufacturer isn't
     * known or the screen doesn't resolve (e.g. it moved in a newer OEM
     * software version).
     */
    fun autostartSettingsIntent(context: Context): Intent? {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: return null
        val candidates = OEM_AUTOSTART_COMPONENTS[manufacturer] ?: return null
        for (component in candidates) {
            val intent = Intent().apply {
                setComponent(component)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                return intent
            }
        }
        return null
    }

    /** True if this device's manufacturer is known to restrict autostart. */
    fun isKnownAutostartRestrictedOem(): Boolean =
        (Build.MANUFACTURER?.lowercase() ?: "") in OEM_AUTOSTART_COMPONENTS.keys
}
