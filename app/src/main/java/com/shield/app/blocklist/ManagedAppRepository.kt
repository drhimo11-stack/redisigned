package com.shield.app.blocklist

import android.content.Context
import kotlinx.coroutines.flow.Flow

class ManagedAppRepository(context: Context) {

    private val dao = BlocklistDatabase.get(context).managedAppDao()

    fun observeAll(): Flow<List<ManagedAppItem>> = dao.observeAll()

    suspend fun add(packageName: String, appLabel: String, autoDetected: Boolean) {
        dao.upsert(
            ManagedAppItem(
                packageName = packageName,
                appLabel = appLabel,
                blocked = true,
                autoDetected = autoDetected
            )
        )
    }

    suspend fun setBlocked(packageName: String, blocked: Boolean) {
        dao.setBlocked(packageName, blocked)
    }

    suspend fun remove(packageName: String) {
        dao.delete(packageName)
    }

    suspend fun exists(packageName: String): Boolean = dao.exists(packageName)

    suspend fun getBlockedPackageNames(): List<String> = dao.getBlockedPackageNames()
}
