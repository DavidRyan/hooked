package com.hooked.domain

sealed class CatchDetailsIntent {
    data class LoadCatchDetails(val catchId: Long) : CatchDetailsIntent()
}