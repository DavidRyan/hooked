package com.hooked.features.catches.presentation.model

sealed class CatchDetailsIntent {
    data class LoadCatchDetails(val catchId: Long) : CatchDetailsIntent()
}