package com.shield.app.accessibility

/**
 * Describes a supported browser: its package name and (optionally) the
 * resource id of its URL bar / omnibox view. Browsers with a null
 * [urlBarId] don't have a stable, documented resource id, so the
 * Accessibility Service falls back to walking the accessibility node
 * tree looking for anything that looks like a URL.
 */
data class SupportedBrowser(
    val packageName: String,
    val urlBarId: String?
)

object BrowserList {

    val SUPPORTED_BROWSERS: List<SupportedBrowser> = listOf(
        SupportedBrowser("com.android.chrome", "com.android.chrome:id/url_bar"),
        SupportedBrowser("com.chrome.canary", "com.chrome.canary:id/url_bar"),
        SupportedBrowser("com.chrome.beta", "com.chrome.beta:id/url_bar"),
        SupportedBrowser("org.mozilla.firefox", "org.mozilla.gecko:id/url_bar"),
        SupportedBrowser("org.mozilla.rocket", null),
        SupportedBrowser("com.brave.browser", "com.brave.browser:id/url_bar"),
        SupportedBrowser("com.opera.browser", "com.opera.browser:id/url_field"),
        SupportedBrowser("com.opera.mini.native", "com.opera.mini.native:id/url_field"),
        SupportedBrowser(
            "com.duckduckgo.mobile.android",
            "com.duckduckgo.mobile.android:id/omnibarTextInput"
        ),
        SupportedBrowser("com.vivaldi.browser", null),
        SupportedBrowser("org.torproject.torbrowser", null),
        SupportedBrowser("com.nationaledtech.spinbrowser", null),
        SupportedBrowser("com.ecosia.android", null),
        SupportedBrowser("com.hsv.freeadblockerbrowser", null),
        SupportedBrowser("idm.internet.download.manager.plus", null),
        SupportedBrowser("com.instantbits.cast.webvideo", null),
        // Mainstream browsers that were missing before. urlBarId left null
        // (no single stable, documented id across OEM builds/versions) so
        // these fall back to the generic node-walking heuristic, same as
        // Vivaldi/Tor/Ecosia/etc. above.
        SupportedBrowser("com.microsoft.emmx", null), // Microsoft Edge
        SupportedBrowser("com.sec.android.app.sbrowser", null), // Samsung Internet
        SupportedBrowser("com.UCMobile.intl", null), // UC Browser
        SupportedBrowser("com.mi.globalbrowser", null), // Mi / Xiaomi Browser
        SupportedBrowser("com.kiwibrowser.browser", null), // Kiwi Browser
        SupportedBrowser("com.yandex.browser", null), // Yandex Browser
        // "Downloader" apps with a built-in browser used to reach video
        // pages. Not traditional browsers, but they render a page and a
        // URL/search bar the same way, so the existing browser-event
        // scanning path (URL heuristic + full visible-text scan) applies.
        SupportedBrowser("com.snaptube.premium", null), // Snaptube
        SupportedBrowser("com.video.fun.app", null) // VidMate
    )

    private val PACKAGE_MAP: Map<String, SupportedBrowser> =
        SUPPORTED_BROWSERS.associateBy { it.packageName }

    fun isSupportedBrowser(packageName: String): Boolean = PACKAGE_MAP.containsKey(packageName)

    fun get(packageName: String): SupportedBrowser? = PACKAGE_MAP[packageName]

    const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    const val INSTAGRAM_PACKAGE = "com.instagram.android"
    const val SETTINGS_PACKAGE = "com.android.settings"

    val YOUTUBE_SHORTS_ACTIVITIES: Set<String> = setOf(
        "com.google.android.youtube.app.ShortsActivity",
        "com.google.android.youtube.ReelsActivity",
        "com.google.android.youtube.ShortsActivity"
    )

    val INSTAGRAM_REELS_ACTIVITIES: Set<String> = setOf(
        "com.instagram.android.activity.ReelActivity",
        "com.instagram.android.reels.ui.ReelActivity"
    )

    /** Matches activity class names for the "Device admin apps" settings screen. */
    fun isDeviceAdminSettingsClass(className: String): Boolean {
        val lower = className.lowercase()
        return lower.contains("deviceadmin") || lower.contains("device_admin")
    }

    /**
     * Matches activity class names for the Accessibility settings screen.
     * On stock/Pixel-style Settings apps this single Activity class stays
     * the same even when the user drills down into a specific service's
     * toggle screen (it's Fragment navigation within one Activity), so
     * this alone blocks the whole "Accessibility" section, not just its
     * top-level list.
     */
    fun isAccessibilitySettingsClass(className: String): Boolean {
        val lower = className.lowercase()
        return lower.contains("accessibility")
    }

    /**
     * Some OEMs (notably Samsung) route sub-screens through a generic host
     * Activity whose class name doesn't reveal which screen is showing.
     * Used only as a secondary signal, paired with on-screen text
     * matching, so it doesn't block unrelated settings pages that happen
     * to share the same host Activity.
     */
    fun isGenericSettingsHostClass(className: String): Boolean {
        val lower = className.lowercase()
        return lower.contains("subsettings") || lower.contains("settingactivity")
    }
}
