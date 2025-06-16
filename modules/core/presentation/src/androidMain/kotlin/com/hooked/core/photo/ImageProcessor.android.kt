package com.hooked.core.photo

import android.content.Context
import android.net.Uri
import android.util.Base64

actual class ImageProcessor(private val context: Context) {
    
    actual suspend fun processImageWithExif(imageBytes: ByteArray): ByteArray {
        return imageBytes
    }
    
    actual suspend fun extractMetadata(imageBytes: ByteArray): PhotoMetadata? {
        return null
    }
    
    actual suspend fun loadImageFromUri(uri: String): ByteArray {
        return try {
            val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
            inputStream?.readBytes() ?: byteArrayOf()
        } catch (e: Exception) {
            throw Exception("Failed to load image from URI: ${e.message}")
        }
    }
    
    fun loadImageFromUriSync(uri: String): ByteArray {
        return try {
            val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
            inputStream?.readBytes() ?: byteArrayOf()
        } catch (e: Exception) {
            throw Exception("Failed to load image from URI: ${e.message}")
        }
    }
}

actual fun ByteArray.toBase64(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
}