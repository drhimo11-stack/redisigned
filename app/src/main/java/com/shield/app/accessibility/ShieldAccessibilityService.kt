package com.shield.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.shield.app.blocklist.BlocklistManager
import com.shield.app.blocklist.ManagedAppManager
import com.shield.app.lock.LockManager
import com.shield.app.lock.SettingsRepository

class ShieldAccessibilityService : AccessibilityService() {

    private lateinit var blocklistManager: BlocklistManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var lockManager: LockManager
    private lateinit var managedAppManager: ManagedAppManager

    // Debounce state: avoid re-showing the overlay for the same match
    // within a short window (e.g. while the user is still typing).
    private var lastBlockedText: String = ""
    private var lastBlockedAt: Long = 0L
    private val debounceMs = 2000L

    // Cache of package names that resolve as able to open web links
    // (i.e. behave like a browser), used to catch browsers we don't
    // explicitly know about when "Block unsupported browsers" is on.
    // Refreshed periodically since the user can install a new browser
    // at any time while the service is running.
    private var genericBrowserPackages: Set<String> = emptySet()
    private var genericBrowserPackagesUpdatedAt: Long = 0L
    private val genericBrowserRefreshIntervalMs = 10 * 60 * 1000L

    override fun onServiceConnected() {
        super.onServiceConnected()
        blocklistManager = BlocklistManager.get(applicationContext)
        settingsRepository = SettingsRepository.get(applicationContext)
        lockManager = LockManager.get(applicationContext)
        managedAppManager = ManagedAppManager.get(applicationContext)
        managedAppManager.initialLoad()

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        when {
            packageName == BrowserList.SETTINGS_PACKAGE -> handleSettingsEvent(className)
            packageName == BrowserList.YOUTUBE_PACKAGE -> handleScheduledEvent { handleYoutubeEvent(className) }
            packageName == BrowserList.INSTAGRAM_PACKAGE -> handleScheduledEvent { handleInstagramEvent(className) }
            BrowserList.isSupportedBrowser(packageName) -> handleScheduledEvent {
                BrowserList.get(packageName)?.let { handleBrowserEvent(packageName, it) }
            }
            // Apps the user explicitly added via the "Manage Apps" screen
            // (or that were auto-detected on install) are always scanned,
            // independent of the "Block unsupported browsers" toggle below
            // — an explicit user choice shouldn't depend on a separate
            // generic-catch-all setting.
            managedAppManager.isBlocked(packageName) -> handleScheduledEvent {
                handleBrowserEvent(packageName, SupportedBrowser(packageName, urlBarId = null))
            }
            else -> handleScheduledEvent { handleUnsupportedPackage(packageName, className) }
        }
    }

    // Content blocking (YouTube Shorts/Reels, Instagram Reels, and all
    // browser/downloader scanning) only runs inside the configured
    // schedule window, if one is set. This is separate from the Device
    // Admin lock, which always protects Shield itself regardless of
    // schedule.
    private inline fun handleScheduledEvent(action: () -> Unit) {
        if (!settingsRepository.getSync().isBlockingActiveNow()) return
        action()
    }

    // --- Settings: block the Device Admin management screen and the
    // Accessibility settings screen, only while a lock is active. Without
    // this, someone could keep Device Admin enabled (so uninstalling is
    // blocked) but simply turn the Accessibility Service off instead,
    // which disables all content blocking just as completely. ---
    private fun handleSettingsEvent(className: String) {
        if (!lockManager.isLocked()) return
        if (BrowserList.isDeviceAdminSettingsClass(className) ||
            BrowserList.isAccessibilitySettingsClass(className)
        ) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            return
        }
        // Some OEMs (Samsung, etc.) host the per-service toggle screen in
        // a generic Activity that doesn't reveal which screen is showing
        // via its class name alone, so fall back to a precise text check:
        // our own service's name together with accessibility wording is
        // effectively only ever true on Shield's own toggle screen.
        if (BrowserList.isGenericSettingsHostClass(className) && isShieldAccessibilityScreenVisible()) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun isShieldAccessibilityScreenVisible(): Boolean {
        val root = rootInActiveWindow ?: return false
        val text = collectVisibleText(root, maxNodes = 200).lowercase()
        // The service's label (shown in Settings) is now "حصن" rather
        // than the English word "Shield", and Android's own Accessibility
        // settings label may itself render in Arabic ("سهولة الوصول")
        // depending on the device's system language — check both so this
        // still works regardless of locale.
        val mentionsApp = text.contains("حصن") || text.contains("shield")
        val mentionsAccessibility = text.contains("accessibility") || text.contains("الوصول")
        return mentionsApp && mentionsAccessibility
    }

    // --- YouTube: only block the Shorts/Reels feed activity, not the app. ---
    private fun handleYoutubeEvent(className: String) {
        if (className in BrowserList.YOUTUBE_SHORTS_ACTIVITIES) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    // --- Instagram: only block the Reels activity, not the app. ---
    private fun handleInstagramEvent(className: String) {
        if (className in BrowserList.INSTAGRAM_REELS_ACTIVITIES) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    // --- Unsupported browser handling (optional toggle) ---
    // Catches browsers we don't explicitly know about (any package that
    // resolves as able to open http/https links) so the app isn't limited
    // to the hardcoded list. Explicitly-listed browsers/downloaders are
    // always scanned regardless of this toggle; this only covers the rest.
    private fun handleUnsupportedPackage(packageName: String, className: String) {
        val settings = settingsRepository.getSync()
        if (!settings.blockUnsupportedBrowsers) return
        if (!isLikelyBrowserPackage(packageName)) return
        handleBrowserEvent(packageName, SupportedBrowser(packageName, urlBarId = null))
    }

    private fun isLikelyBrowserPackage(packageName: String): Boolean {
        refreshGenericBrowserPackagesIfNeeded()
        return packageName in genericBrowserPackages
    }

    private fun refreshGenericBrowserPackagesIfNeeded() {
        val now = System.currentTimeMillis()
        if (genericBrowserPackages.isNotEmpty() &&
            (now - genericBrowserPackagesUpdatedAt) < genericBrowserRefreshIntervalMs
        ) {
            return
        }
        genericBrowserPackagesUpdatedAt = now
        genericBrowserPackages = BrowserDetection.queryBrowserCapablePackages(packageManager, packageName)
    }

    // --- Supported browser: read URL bar + visible text, check blocklist ---
    private fun handleBrowserEvent(packageName: String, browser: SupportedBrowser) {
        val root = rootInActiveWindow ?: return

        val urlBarNode = findUrlBarNode(root, browser)
        val urlText = urlBarNode?.text?.toString()
            ?: findUrlLikeText(root, depth = 0)
            ?: ""

        val isEditingUrlBar = urlBarNode?.isFocused == true

        if (isEditingUrlBar) {
            // The user is actively typing/editing the address bar. At this
            // point autocomplete/suggestion dropdowns are often on screen
            // (history, popular searches, etc.) and scanning the full tree
            // would pick up unrelated suggestion text that the user hasn't
            // navigated to. Only check what they've actually typed so far.
            if (urlText.isBlank()) return
            if (blocklistManager.checkBlock(urlText)) {
                triggerBlock(urlText)
            }
            return
        }

        // Address bar is not being edited: the page has committed/loaded,
        // so it's safe to scan the visible page content too.
        val combinedText = buildString {
            append(urlText)
            append(' ')
            append(collectVisibleText(root, maxNodes = 200))
        }

        if (combinedText.isBlank()) return

        if (blocklistManager.checkBlock(combinedText)) {
            triggerBlock(combinedText)
        }
    }

    private fun findUrlBarNode(root: AccessibilityNodeInfo, browser: SupportedBrowser): AccessibilityNodeInfo? {
        val urlBarId = browser.urlBarId ?: return null
        val nodes = root.findAccessibilityNodeInfosByViewId(urlBarId)
        return nodes?.firstOrNull { it != null && !it.text.isNullOrBlank() }
            ?: nodes?.firstOrNull { it != null }
    }

    private fun findUrlLikeText(node: AccessibilityNodeInfo?, depth: Int): String? {
        if (node == null || depth > 12) return null
        val text = node.text?.toString()
        if (!text.isNullOrBlank() && looksLikeUrl(text)) {
            return text
        }
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            val result = findUrlLikeText(child, depth + 1)
            if (result != null) return result
        }
        return null
    }

    private fun looksLikeUrl(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return true
        // Bare domain heuristic: contains a dot, no spaces, reasonable length.
        return !trimmed.contains(' ') &&
            trimmed.contains('.') &&
            trimmed.length in 4..256
    }

    private fun collectVisibleText(node: AccessibilityNodeInfo?, maxNodes: Int): String {
        val builder = StringBuilder()
        var remaining = maxNodes
        collectVisibleTextInternal(node, builder, intArrayOf(remaining))
        return builder.toString()
    }

    private fun collectVisibleTextInternal(
        node: AccessibilityNodeInfo?,
        builder: StringBuilder,
        remaining: IntArray
    ) {
        if (node == null || remaining[0] <= 0) return
        remaining[0]--

        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            builder.append(text)
            builder.append(' ')
        }
        val desc = node.contentDescription?.toString()
        if (!desc.isNullOrBlank()) {
            builder.append(desc)
            builder.append(' ')
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            if (remaining[0] <= 0) return
            val child = node.getChild(i) ?: continue
            collectVisibleTextInternal(child, builder, remaining)
        }
    }

    private fun triggerBlock(matchedText: String) {
        val now = System.currentTimeMillis()
        val truncated = matchedText.take(64)
        if (truncated == lastBlockedText && (now - lastBlockedAt) < debounceMs) {
            return
        }
        lastBlockedText = truncated
        lastBlockedAt = now

        val settings = settingsRepository.getSync()
        val intent = Intent(this, BlockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(BlockOverlayActivity.EXTRA_MESSAGE, settings.customMessage)
            putExtra(BlockOverlayActivity.EXTRA_COUNTDOWN, settings.countdownSeconds)
            putExtra(BlockOverlayActivity.EXTRA_REDIRECT_URL, settings.redirectUrl)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // No-op: nothing to clean up when the system interrupts the service.
    }
}
