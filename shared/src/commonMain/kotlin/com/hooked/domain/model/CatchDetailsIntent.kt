package com.hooked.domain.model

sealed class CatchDetailsIntent {
    data class LoadCatchDetails(val catchId: Long) : CatchDetailsIntent()
}