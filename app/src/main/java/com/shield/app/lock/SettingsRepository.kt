package com.shield.app.lock

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private val Context.settingsDataStore by preferencesDataStore("shield_settings")

data class SettingsState(
    val customMessage: String = "You are Protected",
    val countdownSeconds: Int = 3,
    val redirectUrl: String = "",
    val blockUnsupportedBrowsers: Boolean = false,
    val scheduleEnabled: Boolean = false,
    // Minutes since midnight, [0, 1440).
    val scheduleStartMinute: Int = 0,
    val scheduleEndMinute: Int = 1440,
    // Bit i (0=Sunday .. 6=Saturday, matching Calendar.DAY_OF_WEEK - 1) set
    // means blocking is scheduled on that day. Defaults to every day.
    val scheduleDaysMask: Int = 0b1111111
) {
    /**
     * Whether content blocking should be actively enforced right now.
     * When the schedule is off, blocking is always active (the previous,
     * default behavior). This only gates content matching/overlay
     * triggers — the Device Admin lock (which stops Shield itself from
     * being disabled/uninstalled) is independent of this schedule.
     */
    fun isBlockingActiveNow(calendar: java.util.Calendar = java.util.Calendar.getInstance()): Boolean {
        if (!scheduleEnabled) return true
        val dayBit = 1 shl (calendar.get(java.util.Calendar.DAY_OF_WEEK) - 1)
        if (scheduleDaysMask and dayBit == 0) return false
        val minuteOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
            calendar.get(java.util.Calendar.MINUTE)
        return if (scheduleStartMinute <= scheduleEndMinute) {
            minuteOfDay in scheduleStartMinute until scheduleEndMinute
        } else {
            // Overnight window, e.g. 22:00 -> 06:00.
            minuteOfDay >= scheduleStartMinute || minuteOfDay < scheduleEndMinute
        }
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val CUSTOM_MESSAGE = stringPreferencesKey("custom_message")
        val COUNTDOWN = intPreferencesKey("countdown")
        val REDIRECT_URL = stringPreferencesKey("redirect_url")
        val BLOCK_UNSUPPORTED = booleanPreferencesKey("block_unsupported_browsers")
        val SCHEDULE_ENABLED = booleanPreferencesKey("schedule_enabled")
        val SCHEDULE_START_MINUTE = intPreferencesKey("schedule_start_minute")
        val SCHEDULE_END_MINUTE = intPreferencesKey("schedule_end_minute")
        val SCHEDULE_DAYS_MASK = intPreferencesKey("schedule_days_mask")
    }

    val settingsFlow: Flow<SettingsState> = context.settingsDataStore.data.map { prefs ->
        SettingsState(
            customMessage = prefs[Keys.CUSTOM_MESSAGE] ?: "You are Protected",
            countdownSeconds = prefs[Keys.COUNTDOWN] ?: 3,
            redirectUrl = prefs[Keys.REDIRECT_URL] ?: "",
            blockUnsupportedBrowsers = prefs[Keys.BLOCK_UNSUPPORTED] ?: false,
            scheduleEnabled = prefs[Keys.SCHEDULE_ENABLED] ?: false,
            scheduleStartMinute = prefs[Keys.SCHEDULE_START_MINUTE] ?: 0,
            scheduleEndMinute = prefs[Keys.SCHEDULE_END_MINUTE] ?: 1440,
            scheduleDaysMask = prefs[Keys.SCHEDULE_DAYS_MASK] ?: 0b1111111
        )
    }

    // Writes are wrapped in NonCancellable: these are launched from
    // viewModelScope, which gets cancelled as soon as the hosting
    // Activity/ViewModel is torn down (e.g. the user backs out right
    // after tapping Save). Without this, an in-flight DataStore write can
    // be cancelled before it reaches disk, silently discarding the value
    // the user just entered. NonCancellable lets the write finish even
    // if the caller's coroutine scope has already been cancelled.
    suspend fun setCustomMessage(message: String) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { it[Keys.CUSTOM_MESSAGE] = message }
        }
    }

    suspend fun setCountdown(seconds: Int) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { it[Keys.COUNTDOWN] = seconds.coerceIn(0, 30) }
        }
    }

    suspend fun setRedirectUrl(url: String) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { it[Keys.REDIRECT_URL] = normalizeRedirectUrl(url) }
        }
    }

    suspend fun setBlockUnsupportedBrowsers(enabled: Boolean) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { it[Keys.BLOCK_UNSUPPORTED] = enabled }
        }
    }

    suspend fun setScheduleEnabled(enabled: Boolean) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { it[Keys.SCHEDULE_ENABLED] = enabled }
        }
    }

    suspend fun setScheduleWindow(startMinute: Int, endMinute: Int) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit {
                it[Keys.SCHEDULE_START_MINUTE] = startMinute.coerceIn(0, 1439)
                it[Keys.SCHEDULE_END_MINUTE] = endMinute.coerceIn(0, 1440)
            }
        }
    }

    suspend fun setScheduleDaysMask(mask: Int) {
        withContext(NonCancellable) {
            context.settingsDataStore.edit { it[Keys.SCHEDULE_DAYS_MASK] = mask and 0b1111111 }
        }
    }

    /**
     * Synchronous getter for callers that aren't coroutine-based, such as
     * the Accessibility Service's event callback which needs settings
     * immediately when building the overlay intent.
     */
    fun getSync(): SettingsState = runBlocking {
        settingsFlow.first()
    }

    /**
     * A bare redirect URL like "google.com" (no scheme) fails to resolve
     * with ACTION_VIEW and silently falls back to Home — from the user's
     * point of view "redirection just doesn't work". Normalize it once
     * here at save time so every reader gets a URL that actually resolves.
     */
    private fun normalizeRedirectUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return trimmed
        return if (trimmed.contains("://")) trimmed else "https://$trimmed"
    }

    companion object {
        @Volatile private var instance: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext)
                    .also { instance = it }
            }
    }
}
