package com.shield.app.blocklist

import android.content.Context
import com.shield.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Keeps an in-memory, thread-safe list of compiled [Keyword] objects so the
 * Accessibility Service can check text synchronously without touching Room
 * on the main event-dispatch path. The list is (re)loaded from the bundled
 * starter list plus the Room-backed user blocklist/whitelist whenever the
 * app starts or the user edits their lists.
 */
class BlocklistManager private constructor(private val appContext: Context) {

    private val repository = BlocklistRepository(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var compiledBlocklist: List<Keyword> = emptyList()
    @Volatile private var compiledWhitelist: List<Keyword> = emptyList()

    fun initialLoad() {
        scope.launch {
            reload()
        }
    }

    suspend fun reload() {
        val builtIn = loadBuiltInPatterns()
        val (userBlock, userWhite) = repository.initialLoad()
        compiledBlocklist = (builtIn + userBlock).distinct().map { Keyword(it) }
        compiledWhitelist = userWhite.map { Keyword(it) }
    }

    private fun loadBuiltInPatterns(): List<String> {
        val patterns = mutableListOf<String>()
        try {
            val inputStream = appContext.resources.openRawResource(R.raw.blocked_keywords)
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        patterns.add(trimmed)
                    }
                }
            }
        } catch (e: Exception) {
            // If the raw resource is somehow missing, fall back to an
            // empty built-in list rather than crashing the service.
        }
        return patterns
    }

    /** Synchronous check used by the Accessibility Service's event callback. */
    fun checkBlock(text: String): Boolean {
        if (text.isBlank()) return false
        val whitelist = compiledWhitelist
        if (whitelist.any { it.matches(text) }) return false
        val blocklist = compiledBlocklist
        return blocklist.any { it.matches(text) }
    }

    suspend fun addUserBlock(pattern: String) {
        repository.addUserBlock(pattern)
        reload()
    }

    suspend fun removeUserBlock(pattern: String) {
        repository.removeUserBlock(pattern)
        reload()
    }

    suspend fun addWhitelist(pattern: String) {
        repository.addWhitelist(pattern)
        reload()
    }

    suspend fun removeWhitelist(pattern: String) {
        repository.removeWhitelist(pattern)
        reload()
    }

    fun getRepository(): BlocklistRepository = repository

    companion object {
        @Volatile private var instance: BlocklistManager? = null

        fun get(context: Context): BlocklistManager =
            instance ?: synchronized(this) {
                instance ?: BlocklistManager(context.applicationContext)
                    .also { instance = it }
            }
    }
}
