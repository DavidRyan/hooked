package com.hooked.catches.presentation.model

sealed class CatchDetailsIntent {
    data class LoadCatchDetails(val catchId: String) : CatchDetailsIntent()
}