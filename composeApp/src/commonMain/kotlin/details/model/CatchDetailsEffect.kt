package details.model

sealed class CatchDetailsEffect {
    data class OnError(val message: String) : CatchDetailsEffect()
}