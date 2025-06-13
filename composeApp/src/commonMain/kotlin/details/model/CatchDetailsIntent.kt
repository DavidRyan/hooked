package details.model

sealed class CatchDetailsIntent {
    data class LoadCatchDetails(val catchId: Long) : CatchDetailsIntent()
}