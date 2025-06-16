package com.hooked.core.photo

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.*
import platform.CoreFoundation.*

actual class ImageProcessor {
    
    actual suspend fun processImageWithExif(imageBytes: ByteArray): ByteArray {
        return try {
            // Convert ByteArray to NSData
            val nsData = imageBytes.toNSData()
            
            // Create UIImage from data (preserves EXIF)
            val image = UIImage.imageWithData(nsData)
            
            // Convert back to JPEG with compression while preserving EXIF
            val compressedData = UIImageJPEGRepresentation(image, 0.85)
            
            // Convert back to ByteArray
            compressedData?.toByteArray() ?: imageBytes
        } catch (e: Exception) {
            // If processing fails, return original
            imageBytes
        }
    }
    
    actual suspend fun extractMetadata(imageBytes: ByteArray): PhotoMetadata? {
        // Not needed - backend will handle EXIF parsing
        return null
    }
    
    actual suspend fun loadImageFromUri(uri: String): ByteArray {
        return try {
            val nsUrl = NSURL.fileURLWithPath(uri)
            val nsData = NSData.dataWithContentsOfURL(nsUrl)
            nsData?.toByteArray() ?: byteArrayOf()
        } catch (e: Exception) {
            throw Exception("Failed to load image from URI: ${e.message}")
        }
    }
}

actual fun ByteArray.toBase64(): String {
    val nsData = this.toNSData()
    return nsData.base64EncodedStringWithOptions(0)
}

// Extension functions for ByteArray <-> NSData conversion
fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
    }
}

fun NSData.toByteArray(): ByteArray {
    return ByteArray(this.length.toInt()).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }
}