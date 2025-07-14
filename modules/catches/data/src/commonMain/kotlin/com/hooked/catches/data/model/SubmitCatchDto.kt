package com.hooked.catches.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmitCatchDto(
    val species: String,
    val location: String,
    val latitude: Double?,
    val longitude: Double?,
    @SerialName("caught_at") val caughtAt: String,
    val notes: String? = null
)

sealed class CatchSubmissionResult {
    data class Success(val catchId: String) : CatchSubmissionResult()
    data class Error(val message: String) : CatchSubmissionResult()
    object Loading : CatchSubmissionResult()
}