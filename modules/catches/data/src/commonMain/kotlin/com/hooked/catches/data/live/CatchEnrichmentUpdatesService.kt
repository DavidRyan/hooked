package com.hooked.catches.data.live

import com.hooked.auth.data.storage.TokenStorage
import com.hooked.auth.domain.entities.UserEntity
import com.hooked.catches.domain.entities.CatchEnrichmentUpdate
import com.hooked.core.logging.Logger
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull

class CatchEnrichmentUpdatesService(
    private val socketClient: PhoenixSocketClient,
    private val authProvider: AuthProvider,
    private val eventDecoder: EnrichmentEventDecoder
) {
    constructor(
        httpClient: HttpClient,
        tokenStorage: TokenStorage,
        json: Json,
        baseUrlProvider: BaseUrlProvider = NetworkBaseUrlProvider(),
        eventDecoder: EnrichmentEventDecoder = DefaultEnrichmentEventDecoder()
    ) : this(
        socketClient = PhoenixSocketClient(httpClient, json, baseUrlProvider),
        authProvider = TokenStorageAuthProvider(tokenStorage, json),
        eventDecoder = eventDecoder
    )

    fun updatesFlow(): Flow<CatchEnrichmentUpdate> =
        socketClient.messages(
            topicFor = { auth -> "catch_enrichment:${auth.userId}" },
            authProvider = authProvider
        ).mapNotNull(eventDecoder::decode)
}

interface EnrichmentEventDecoder {
    fun decode(message: PhoenixMessage): CatchEnrichmentUpdate?
}

class DefaultEnrichmentEventDecoder : EnrichmentEventDecoder {
    override fun decode(message: PhoenixMessage): CatchEnrichmentUpdate? {
        Logger.debug(TAG, "Decoding socket event=${message.event} topic=${message.topic}")
        return when (message.event) {
            "enrichment_completed" -> decodeCompleted(message.payload)
            "enrichment_failed" -> decodeFailed(message.payload)
            else -> null
        }
    }

    private fun decodeCompleted(payload: JsonElement): CatchEnrichmentUpdate? {
        val catchId = payload.jsonObject["catch_id"]?.asString()
            ?: payload.jsonObject["catch"]?.jsonObject?.get("id")?.asString()

        return if (catchId != null) {
            CatchEnrichmentUpdate.Completed(catchId)
        } else {
            Logger.warning(TAG, "Received enrichment_completed with missing catch ID: $payload")
            null
        }
    }

    private fun decodeFailed(payload: JsonElement): CatchEnrichmentUpdate? {
        val payloadObject = payload.jsonObject
        val catchId = payloadObject["catch_id"]?.asString()
            ?: payloadObject["catch"]?.jsonObject?.get("id")?.asString()
        val error = payloadObject["error"]?.asString()

        return if (catchId != null) {
            CatchEnrichmentUpdate.Failed(catchId, error)
        } else {
            Logger.warning(TAG, "Received enrichment_failed with missing catch ID: $payload")
            null
        }
    }

    private fun JsonElement.asString(): String? = (this as? JsonPrimitive)?.contentOrNull
}

class TokenStorageAuthProvider(
    private val tokenStorage: TokenStorage,
    private val json: Json
) : AuthProvider {
    override suspend fun currentAuth(): SocketAuth? {
        val token = tokenStorage.getToken() ?: return null
        val userJson = tokenStorage.getUser() ?: return null

        return runCatching { json.decodeFromString<UserEntity>(userJson) }
            .map { SocketAuth(token, it.id) }
            .onFailure { Logger.error(TAG, "Failed to decode cached user: ${it.message}", it) }
            .getOrNull()
    }
}

private const val TAG = "CatchEnrichmentSocket"
