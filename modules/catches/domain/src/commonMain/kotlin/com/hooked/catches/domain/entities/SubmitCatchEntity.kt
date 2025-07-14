package com.hooked.catches.domain.entities

data class SubmitCatchEntity(
    val species: String,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val caughtAt: String?,
    val notes: String?,
    val imageBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SubmitCatchEntity

        if (species != other.species) return false
        if (location != other.location) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (caughtAt != other.caughtAt) return false
        if (notes != other.notes) return false
        if (imageBytes != null) {
            if (other.imageBytes == null) return false
            if (!imageBytes.contentEquals(other.imageBytes)) return false
        } else if (other.imageBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = species.hashCode()
        result = 31 * result + (location?.hashCode() ?: 0)
        result = 31 * result + (latitude?.hashCode() ?: 0)
        result = 31 * result + (longitude?.hashCode() ?: 0)
        result = 31 * result + (caughtAt?.hashCode() ?: 0)
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        return result
    }
}