package com.tekphreak.darkbook.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Stores the diary PIN as a salted SHA-256 hash inside EncryptedSharedPreferences.
 * The raw PIN is never persisted. There is no recovery by design — see darkbook.md.
 */
object PinManager {
    private const val PREFS_NAME = "darkbook_pin_prefs"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_HASH = "pin_hash"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "lockout_until"

    // After this many consecutive wrong PINs in a row, entry is locked out for
    // LOCKOUT_MS to slow down brute-forcing — no wipe, per darkbook.md's
    // no-recovery-by-design stance (wiping the PIN on failure would only make
    // that worse, not better).
    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_MS = 30_000L

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun hasPin(context: Context): Boolean = prefs(context).contains(KEY_HASH)

    fun setPin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt)
        prefs(context).edit()
            .putString(KEY_SALT, salt.joinToString("") { "%02x".format(it) })
            .putString(KEY_HASH, hash)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    /** Millis-since-epoch until which PIN entry is locked out, or 0 if not locked out. */
    fun lockedOutUntil(context: Context): Long = prefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L)

    fun isLockedOut(context: Context): Boolean = System.currentTimeMillis() < lockedOutUntil(context)

    fun verifyPin(context: Context, pin: String): Boolean {
        if (isLockedOut(context)) return false

        val p = prefs(context)
        val saltHex = p.getString(KEY_SALT, null) ?: return false
        val storedHash = p.getString(KEY_HASH, null) ?: return false
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val matches = hash(pin, salt) == storedHash

        if (matches) {
            p.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCKOUT_UNTIL, 0L).apply()
        } else {
            val attempts = failedAttempts(context) + 1
            val editor = p.edit()
            if (attempts >= MAX_ATTEMPTS) {
                editor.putInt(KEY_FAILED_ATTEMPTS, 0)
                editor.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_MS)
            } else {
                editor.putInt(KEY_FAILED_ATTEMPTS, attempts)
            }
            editor.apply()
        }
        return matches
    }

    fun failedAttempts(context: Context): Int = prefs(context).getInt(KEY_FAILED_ATTEMPTS, 0)

    fun resetForWipe(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
