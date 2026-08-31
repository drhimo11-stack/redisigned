package com.shield.app.blocklist

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocklist_items",
    indices = [Index(value = ["pattern"], unique = true)]
)
data class BlocklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "whitelist_items",
    indices = [Index(value = ["pattern"], unique = true)]
)
data class WhitelistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pattern: String,
    val createdAt: Long = System.currentTimeMillis()
)
