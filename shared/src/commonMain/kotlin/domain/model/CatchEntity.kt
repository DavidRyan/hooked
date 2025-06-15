package domain.model

import data.model.CatchDto

data class CatchEntity(
    val id: Long,
    val name: String,
    val description: String,
    val dateCaught: String,
    val location: String,
    val imageUrl: String? = null,
    val weight: Double? = null,
    val length: Double? = null
)

fun CatchDto.toEntity(): CatchEntity {
    return CatchEntity(
        id = id,
        name = species,
        description = "Caught a $species weighing $weight kg and measuring $length cm",
        dateCaught = "2023-10-01", // Placeholder, replace with actual date logic
        location = "Unknown", // Placeholder, replace with actual location logic
        imageUrl = photoUrl,
        weight = weight,
        length = length
    )
}