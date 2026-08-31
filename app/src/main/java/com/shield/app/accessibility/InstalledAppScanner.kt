package com.shield.app.accessibility

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val isBrowserCapable: Boolean
)

object InstalledAppScanner {

    /**
     * All launchable, non-Shield apps on the device, for the "Manage Apps"
     * add-picker. Browser-capable apps are flagged so the UI can surface
     * them first, but everything launchable is included since a
     * downloader-style app (Snaptube/Vidmate-alikes) may not register as
     * a link-opener and still needs to be pickable manually.
     */
    fun listLaunchableApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val browserCapable = BrowserDetection.queryBrowserCapablePackages(pm, context.packageName)

        return try {
            pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .mapNotNull { it.activityInfo }
                .filter { it.packageName != context.packageName }
                .distinctBy { it.packageName }
                .map { activityInfo ->
                    val label = try {
                        activityInfo.applicationInfo.loadLabel(pm).toString()
                    } catch (e: Exception) {
                        activityInfo.packageName
                    }
                    InstalledAppInfo(
                        packageName = activityInfo.packageName,
                        label = label,
                        isBrowserCapable = activityInfo.packageName in browserCapable
                    )
                }
                .sortedWith(compareByDescending<InstalledAppInfo> { it.isBrowserCapable }.thenBy { it.label.lowercase() })
        } catch (e: Exception) {
            emptyList()
        }
    }
}
