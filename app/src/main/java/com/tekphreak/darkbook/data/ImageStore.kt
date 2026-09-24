package com.tekphreak.darkbook.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.util.UUID

/**
 * Attached images are copied into app-private storage and encrypted with the
 * same Keystore-backed master key as the rest of the app's data — nothing
 * about an entry, image included, is readable outside this app's sandbox.
 */
object ImageStore {
    private const val IMAGES_DIR = "entry_images"

    // Entries only ever display their image at a 200dp thumbnail or, at most,
    // a full-screen zoom — decoding a 12MP+ camera photo at full resolution
    // for either wastes memory and risks OOM on lower-end devices.
    private const val MAX_DECODED_DIMENSION_PX = 1600

    fun saveImage(context: Context, sourceUri: Uri): String? {
        val filename = "${UUID.randomUUID()}.enc"
        val destFile = File(imagesDir(context), filename)
        return try {
            val encryptedFile = encryptedFile(context, destFile)
            val input = context.contentResolver.openInputStream(sourceUri) ?: return null
            input.use { source ->
                encryptedFile.openFileOutput().use { output -> source.copyTo(output) }
            }
            filename
        } catch (e: Exception) {
            // Leaves no partial/corrupt file behind on a failed copy (e.g. disk full).
            destFile.delete()
            null
        }
    }

    fun loadBitmap(context: Context, filename: String): Bitmap? {
        val file = File(imagesDir(context), filename)
        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        encryptedFile(context, file).openFileInput().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_DECODED_DIMENSION_PX)
        }
        return encryptedFile(context, file).openFileInput().use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        }
    }

    private fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    fun deleteImage(context: Context, filename: String) {
        File(imagesDir(context), filename).delete()
    }

    private fun imagesDir(context: Context): File =
        File(context.filesDir, IMAGES_DIR).apply { mkdirs() }

    private fun encryptedFile(context: Context, file: File): EncryptedFile {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedFile.Builder(
            context, file, masterKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }
}
