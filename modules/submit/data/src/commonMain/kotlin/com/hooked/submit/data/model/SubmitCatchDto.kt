package com.hooked.submit.data.model

import kotlinx.serialization.Serializable

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