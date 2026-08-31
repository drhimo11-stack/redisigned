package com.shield.app.blocklist

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A specific installed app (browser, downloader, or anything else the user
 * picked) that should be scanned/blocked the same way a built-in browser
 * is. Distinct from [BlocklistItem]/[WhitelistItem], which are text
 * patterns — this is a whole *app* the user has opted into blocking,
 * either manually or via auto-detection on install.
 */
@Entity(tableName = "managed_apps")
data class ManagedAppItem(
    @PrimaryKey val packageName: String,
    val appLabel: String,
    val blocked: Boolean = true,
    val autoDetected: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
