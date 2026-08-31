package com.shield.app.blocklist

import android.content.Context
import kotlinx.coroutines.flow.Flow

class BlocklistRepository(context: Context) {

    private val db = BlocklistDatabase.get(context)
    private val blocklistDao = db.blocklistDao()
    private val whitelistDao = db.whitelistDao()

    suspend fun addUserBlock(pattern: String) {
        val trimmed = pattern.trim()
        if (trimmed.isEmpty()) return
        blocklistDao.insert(BlocklistItem(pattern = trimmed))
    }

    suspend fun removeUserBlock(pattern: String) {
        blocklistDao.deleteByPattern(pattern)
    }

    suspend fun addWhitelist(pattern: String) {
        val trimmed = pattern.trim()
        if (trimmed.isEmpty()) return
        whitelistDao.insert(WhitelistItem(pattern = trimmed))
    }

    suspend fun removeWhitelist(pattern: String) {
        whitelistDao.deleteByPattern(pattern)
    }

    fun observeBlocklist(): Flow<List<BlocklistItem>> = blocklistDao.observeAll()

    fun observeWhitelist(): Flow<List<WhitelistItem>> = whitelistDao.observeAll()

    suspend fun initialLoad(): Pair<List<String>, List<String>> {
        return Pair(blocklistDao.getAllPatterns(), whitelistDao.getAllPatterns())
    }

    suspend fun checkBlock(text: String, keywords: List<Keyword>, whitelist: List<Keyword>): Boolean {
        if (whitelist.any { it.matches(text) }) return false
        return keywords.any { it.matches(text) }
    }
}
