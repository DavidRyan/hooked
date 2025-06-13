package grid.repository

import grid.model.CatchModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatchGridRepository(private val httpClient: HttpClient) {

    suspend fun getCatches(): List<CatchModel> {
        return withContext(Dispatchers.Default) {
            httpClient.get("http://10.0.2.2:8080/catches").body()
        }
    }
}
