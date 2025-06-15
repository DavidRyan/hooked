package domain.model

sealed class NetworkResult<out T> {
    object Loading : NetworkResult<Nothing>()
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val error: Throwable) : NetworkResult<Nothing>()
}
