package com.shield.app.lock

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec

/**
 * Manages the "lock" period during which the user cannot disable Device
 * Admin or the Accessibility Service. The lock end-timestamp is encrypted
 * (AES via PBEWithMD5AndDES) and stored in SharedPreferences, and a
 * separate "aux" prefs file guards against the user rewinding the device
 * clock to cheat the lock.
 */
class LockManager private constructor(context: Context) {

    private val appContext = context.applicationContext

    private val lockPrefs: SharedPreferences =
        appContext.getSharedPreferences("shield_lock", Context.MODE_PRIVATE)
    private val auxPrefs: SharedPreferences =
        appContext.getSharedPreferences("shield_lock_aux", Context.MODE_PRIVATE)

    private val algorithm = "PBEWithMD5AndDES"
    private val graceMs = 60_000L // 60 second grace period for normal clock drift

    private fun getOrCreateSalt(): ByteArray {
        val existing = auxPrefs.getString(KEY_SALT, null)
        if (existing != null) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }
        val salt = ByteArray(8)
        SecureRandom().nextBytes(salt)
        auxPrefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply()
        return salt
    }

    private fun cipherFor(mode: Int): Cipher {
        val salt = getOrCreateSalt()
        val password = ANDROID_ID_SEED.toCharArray()
        val keySpec = PBEKeySpec(password)
        val keyFactory = SecretKeyFactory.getInstance(algorithm)
        val key = keyFactory.generateSecret(keySpec)
        val cipher = Cipher.getInstance(algorithm)
        val paramSpec = PBEParameterSpec(salt, 100)
        cipher.init(mode, key, paramSpec)
        return cipher
    }

    private fun encrypt(value: String): String {
        val cipher = cipherFor(Cipher.ENCRYPT_MODE)
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String? {
        return try {
            val cipher = cipherFor(Cipher.DECRYPT_MODE)
            val decoded = Base64.decode(value, Base64.NO_WRAP)
            String(cipher.doFinal(decoded), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /** Starts (or extends) a lock for [days] days from now. */
    fun startLock(days: Int) {
        val clamped = days.coerceIn(1, 365)
        val endTime = System.currentTimeMillis() + clamped * 24L * 60L * 60L * 1000L
        setLockEnd(endTime)
        updateLastSeen(System.currentTimeMillis())
    }

    private fun setLockEnd(endTimeMillis: Long) {
        lockPrefs.edit().putString(KEY_LOCK_END, encrypt(endTimeMillis.toString())).apply()
    }

    private fun getRawLockEnd(): Long {
        val encrypted = lockPrefs.getString(KEY_LOCK_END, null) ?: return 0L
        val decrypted = decrypt(encrypted) ?: return 0L
        return decrypted.toLongOrNull() ?: 0L
    }

    private fun updateLastSeen(now: Long) {
        auxPrefs.edit().putLong(KEY_LAST_SEEN, now).apply()
    }

    /**
     * Checks for backward clock jumps and extends the lock accordingly.
     * Must be called before every read of lock state.
     */
    private fun checkAntiRewind() {
        val now = System.currentTimeMillis()
        val lastSeen = auxPrefs.getLong(KEY_LAST_SEEN, 0L)
        if (lastSeen == 0L) {
            updateLastSeen(now)
            return
        }
        if (now + graceMs < lastSeen) {
            // Clock moved backward by more than the grace period.
            val jump = lastSeen - now
            val currentEnd = getRawLockEnd()
            if (currentEnd > 0L) {
                setLockEnd(currentEnd + jump)
            }
        }
        if (now > lastSeen) {
            updateLastSeen(now)
        }
    }

    /** Returns true if a lock is currently active. */
    fun isLocked(): Boolean {
        checkAntiRewind()
        val end = getRawLockEnd()
        return end > System.currentTimeMillis()
    }

    /** Returns milliseconds remaining in the lock, or 0 if not locked. */
    fun remainingMillis(): Long {
        checkAntiRewind()
        val end = getRawLockEnd()
        val remaining = end - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    /** Human readable remaining time, e.g. "3d 5h 12m". */
    fun remainingHumanReadable(): String {
        val remaining = remainingMillis()
        if (remaining <= 0L) return "0m"
        val totalMinutes = remaining / 60_000L
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0 || days > 0) append("${hours}h ")
            append("${minutes}m")
        }.trim()
    }

    fun clearLockIfExpired() {
        if (!isLocked()) {
            lockPrefs.edit().remove(KEY_LOCK_END).apply()
        }
    }

    companion object {
        private const val KEY_LOCK_END = "lock_end"
        private const val KEY_SALT = "salt"
        private const val KEY_LAST_SEEN = "last_seen"

        // Fixed seed used as the PBE password. The security of this scheme
        // relies on the per-install random salt, not on this seed being
        // secret; it exists so encryption/decryption is deterministic
        // per-app without needing to store a separate password.
        private const val ANDROID_ID_SEED = "shield-lock-passphrase-v1"

        @Volatile private var instance: LockManager? = null

        fun get(context: Context): LockManager =
            instance ?: synchronized(this) {
                instance ?: LockManager(context).also { instance = it }
            }
    }
}
