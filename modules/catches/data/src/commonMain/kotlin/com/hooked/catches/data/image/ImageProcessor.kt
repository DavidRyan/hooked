package com.hooked.catches.data.image

expect class ImageProcessor {
    suspend fun loadImageFromUri(uri: String): ByteArray
}

fun ByteArray.encodeBase64(): String {
    return this.toBase64()
}

expect fun ByteArray.toBase64(): String