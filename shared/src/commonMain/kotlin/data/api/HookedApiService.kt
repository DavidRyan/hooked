package data.api

import data.model.CatchDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * Defines the network API operations. Stub for now.
 */
class HookedApiService(
    private val httpClient: HttpClient,
) {
    fun getCatches(): List<CatchDto> {
        val remoteCatches = httpClient.get("http://10.0.2.2:8080/catches").body<List<CatchDto>>()
        return remoteCatches
    }
}
