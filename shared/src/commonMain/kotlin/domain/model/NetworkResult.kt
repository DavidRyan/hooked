package domain.model

/**
 * A sealed class to handle network results (stub)
 */
sealed class NetworkResult<out T> {
    object Loading : NetworkResult<Nothing>()
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val error: Throwable) : NetworkResult<Nothing>()
}
