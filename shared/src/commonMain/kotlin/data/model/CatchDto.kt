package data.model

import kotlinx.serialization.Serializable

@Serializable
data class CatchDto(
    val id: Long,
    val species: String,
    val weight: Double,
    val length: Double,
    val photoUrl: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long? = null
)

sealed class CatchResult {
    data class Success(val catches: List<CatchDto>) : CatchResult()
    data class Error(val message: String) : CatchResult()
    object Loading : CatchResult()
}

sealed class CatchDetailsResult {
    data class Success(val catch: CatchDto) : CatchDetailsResult()
    data class Error(val message: String) : CatchDetailsResult()
    object Loading : CatchDetailsResult()
}

@Serializable
data class SubmitCatchDto(
    val species: String,
    val weight: Double,
    val length: Double,
    val latitude: Double?,
    val longitude: Double?,
    val photoBase64: String?,
    val timestamp: Long
)

sealed class CatchSubmissionResult {
    data class Success(val catchId: Long) : CatchSubmissionResult()
    data class Error(val message: String) : CatchSubmissionResult()
    object Loading : CatchSubmissionResult()
}