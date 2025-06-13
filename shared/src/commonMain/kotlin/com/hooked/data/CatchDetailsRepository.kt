package com.hooked.data

import details.model.CatchDetailsModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatchDetailsRepository(private val httpClient: HttpClient) {

    suspend fun getCatchDetails(catchId: Long): CatchDetailsModel {
        return withContext(Dispatchers.Default) {
            httpClient.get("http://10.0.2.2:8080/catch/$catchId").body()
        }
    }
}
