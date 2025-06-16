package core.photo

expect class ImageProcessor {
    suspend fun processImageWithExif(imageBytes: ByteArray): ByteArray
    suspend fun extractMetadata(imageBytes: ByteArray): PhotoMetadata?
    suspend fun loadImageFromUri(uri: String): ByteArray
}

fun ByteArray.encodeBase64(): String {
    return this.toBase64()
}

expect fun ByteArray.toBase64(): String