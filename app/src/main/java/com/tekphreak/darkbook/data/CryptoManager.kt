package com.tekphreak.darkbook.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Generates and stores the SQLCipher database passphrase.
 * The passphrase itself is random per-install; it's persisted only inside
 * EncryptedSharedPreferences, whose master key lives in the Android Keystore
 * and never leaves the device.
 */
object CryptoManager {
    private const val PREFS_NAME = "darkbook_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    fun getOrCreateDbPassphrase(context: Context): CharArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) return existing.toCharArray()

        val random = ByteArray(32)
        SecureRandom().nextBytes(random)
        val generated = random.joinToString("") { "%02x".format(it) }
        prefs.edit().putString(KEY_DB_PASSPHRASE, generated).apply()
        return generated.toCharArray()
    }
}
