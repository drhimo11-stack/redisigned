package com.shield.app.blocklist

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Keeps an in-memory, thread-safe set of user-managed "block this whole
 * app" package names, so the Accessibility Service can check membership
 * synchronously on its hot event-dispatch path without touching Room. This
 * is what backs both the manual "Manage Apps" screen and the auto-detected
 * newly-installed browsers/downloaders.
 */
class ManagedAppManager private constructor(private val appContext: Context) {

    val repository = ManagedAppRepository(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var blockedPackages: Set<String> = emptySet()

    fun initialLoad() {
        scope.launch { reload() }
    }

    suspend fun reload() {
        blockedPackages = repository.getBlockedPackageNames().toSet()
    }

    /** Synchronous check used by the Accessibility Service's event callback. */
    fun isBlocked(packageName: String): Boolean = packageName in blockedPackages

    suspend fun add(packageName: String, appLabel: String, autoDetected: Boolean) {
        repository.add(packageName, appLabel, autoDetected)
        reload()
    }

    suspend fun setBlocked(packageName: String, blocked: Boolean) {
        repository.setBlocked(packageName, blocked)
        reload()
    }

    suspend fun remove(packageName: String) {
        repository.remove(packageName)
        reload()
    }

    companion object {
        @Volatile private var instance: ManagedAppManager? = null

        fun get(context: Context): ManagedAppManager =
            instance ?: synchronized(this) {
                instance ?: ManagedAppManager(context.applicationContext)
                    .also { instance = it }
            }
    }
}
