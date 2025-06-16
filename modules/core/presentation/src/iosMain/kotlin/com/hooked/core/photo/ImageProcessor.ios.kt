package com.hooked.core.photo

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.*
import platform.CoreFoundation.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)

actual class ImageProcessor {
    
    actual suspend fun processImageWithExif(imageBytes: ByteArray): ByteArray {
        return try {
            val nsData = imageBytes.toNSData()
            
            val image = UIImage.imageWithData(nsData)
            
            val compressedData = image?.let { UIImageJPEGRepresentation(it, 0.85) }
            
            compressedData?.toByteArray() ?: imageBytes
        } catch (e: Exception) {
            imageBytes
        }
    }
    
    actual suspend fun extractMetadata(imageBytes: ByteArray): PhotoMetadata? {
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
    return nsData.base64EncodedStringWithOptions(0UL)
}

@OptIn(ExperimentalForeignApi::class)
fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), this.size.convert<ULong>())
    }
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    return ByteArray(this.length.toInt()).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }
}