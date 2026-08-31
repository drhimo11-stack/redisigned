package com.shield.app.accessibility

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Shared logic for asking Android "which installed apps can open a web
 * link". Used both by [com.shield.app.accessibility.ShieldAccessibilityService]'s
 * generic unsupported-browser fallback and by the new-install detector that
 * auto-adds newly installed browsers to the block list.
 */
object BrowserDetection {

    /** All installed packages (other than [selfPackage]) that resolve as able to open http(s) links. */
    fun queryBrowserCapablePackages(pm: PackageManager, selfPackage: String): Set<String> {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .mapNotNull { it.activityInfo?.packageName }
                .filter { it != selfPackage }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /** Whether a single package resolves as able to open http(s) links. */
    fun isBrowserCapable(pm: PackageManager, packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
                setPackage(packageName)
            }
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
