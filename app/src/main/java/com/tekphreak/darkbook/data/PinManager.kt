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
            .apply()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val saltHex = p.getString(KEY_SALT, null) ?: return false
        val storedHash = p.getString(KEY_HASH, null) ?: return false
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val matches = hash(pin, salt) == storedHash
        p.edit().putInt(KEY_FAILED_ATTEMPTS, if (matches) 0 else failedAttempts(context) + 1).apply()
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
