package com.hooked.catches.domain.entities

sealed interface CatchEnrichmentUpdate {
    data class Completed(val catchId: String) : CatchEnrichmentUpdate
    data class Failed(val catchId: String, val errorMessage: String?) : CatchEnrichmentUpdate
}
