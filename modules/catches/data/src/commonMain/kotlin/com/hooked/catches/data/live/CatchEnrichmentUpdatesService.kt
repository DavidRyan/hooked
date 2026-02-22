package com.hooked.catches.data.live

import com.hooked.auth.data.storage.TokenStorage
import com.hooked.auth.domain.entities.UserEntity
import com.hooked.catches.domain.entities.CatchEnrichmentUpdate
import com.hooked.core.config.NetworkConfig
import com.hooked.core.logging.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.takeFrom
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CatchEnrichmentUpdatesService(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage,
    private val json: Json
) {
    companion object {
        private const val TAG = "CatchEnrichmentSocket"
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
        private const val MAX_RETRY_DELAY_MS = 5_000L
    }

    fun updatesFlow(): Flow<CatchEnrichmentUpdate> = channelFlow {
        val authState = loadAuthState()
        if (authState == null) {
            Logger.warning(TAG, "Cannot open websocket without authentication")
            close()
            return@channelFlow
        }

        val topic = "catch_enrichment:${authState.user.id}"
        val socketUrl = buildSocketUrl(NetworkConfig.BASE_URL)

        val socketJob = launch {
            var attempt = 0

            while (isActive) {
                try {
                    attempt += 1
                    httpClient.webSocket(
                        request = {
                            url.takeFrom(socketUrl)
                            // Phoenix sockets expect the auth token as a URL query param
                            url.parameters.append("token", authState.token)
                            url.parameters.append("vsn", "2.0.0")
                        }
                    ) {
                        Logger.info(TAG, "Connected to catch enrichment socket")
                        attempt = 0
                        maintainConnection(topic, this@channelFlow)
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    Logger.warning(TAG, "Socket disconnected: ${error.message}")
                    val backoff = (attempt.coerceAtMost(5) * 1_000L).coerceAtMost(MAX_RETRY_DELAY_MS)
                    delay(backoff)
                }
            }
        }

        awaitClose { socketJob.cancel() }
    }

    private suspend fun DefaultClientWebSocketSession.maintainConnection(
        topic: String,
        producer: SendChannel<CatchEnrichmentUpdate>
    ) {
        val refGenerator = RefGenerator()
        val joinRef = refGenerator.nextRef()
        sendMessage(
            PhoenixMessage(
                topic = topic,
                event = "phx_join",
                payload = emptyPayload(),
                ref = joinRef,
                joinRef = joinRef
            )
        )

        val heartbeatJob = launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                sendMessage(
                    PhoenixMessage(
                        topic = "phoenix",
                        event = "heartbeat",
                        payload = emptyPayload(),
                        ref = refGenerator.nextRef()
                    )
                )
            }
        }

        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> handleIncomingFrame(frame.readText(), topic, producer)
                    is Frame.Close -> {
                        val reason = frame.readReason() ?: CloseReason(CloseReason.Codes.NORMAL, "")
                        Logger.info(TAG, "Socket closed: ${reason.message}")
                        return
                    }

                    else -> Unit
                }
            }
        } finally {
            heartbeatJob.cancel()
        }
    }

    private suspend fun DefaultClientWebSocketSession.sendMessage(message: PhoenixMessage) {
        val encoded = json.encodeToString(PhoenixMessage.serializer(), message)
        send(Frame.Text(encoded))
    }

    private fun handleIncomingFrame(
        payload: String,
        topic: String,
        producer: SendChannel<CatchEnrichmentUpdate>
    ) {
        val message = runCatching { json.decodeFromString<PhoenixMessage>(payload) }.getOrNull()
        if (message == null) {
            Logger.warning(TAG, "Failed to decode Phoenix message: $payload")
            return
        }

        if (message.topic != topic) {
            return
        }

        when (message.event) {
            "enrichment_completed" -> {
                val catchId = message.payload.jsonObject
                    .get("catch")
                    ?.jsonObject
                    ?.get("id")
                    ?.asString()

                if (catchId != null) {
                    producer.trySend(CatchEnrichmentUpdate.Completed(catchId))
                } else {
                    Logger.warning(TAG, "Received enrichment_completed with missing catch ID: ${message.payload}")
                }
            }

            "enrichment_failed" -> {
                val payloadObject = message.payload.jsonObject
                val catchId = payloadObject["catch_id"]?.asString()
                val error = payloadObject["error"]?.asString()

                if (catchId != null) {
                    producer.trySend(CatchEnrichmentUpdate.Failed(catchId, error))
                } else {
                    Logger.warning(TAG, "Received enrichment_failed with missing catch ID: ${message.payload}")
                }
            }

            else -> Unit
        }
    }

    private fun JsonElement.asString(): String? = (this as? JsonPrimitive)?.contentOrNull

    private suspend fun loadAuthState(): AuthState? {
        val token = tokenStorage.getToken() ?: return null
        val userJson = tokenStorage.getUser() ?: return null

        return runCatching { json.decodeFromString<UserEntity>(userJson) }
            .map { AuthState(token, it) }
            .onFailure { Logger.error(TAG, "Failed to decode cached user: ${it.message}", it) }
            .getOrNull()
    }

    private fun buildSocketUrl(apiBaseUrl: String): String {
        val base = URLBuilder(apiBaseUrl)
        val socketProtocol = if (base.protocol.isSecure()) URLProtocol.WSS else URLProtocol.WS

        val explicitPort = base.port.takeIf { port ->
            port != 0 && port != base.protocol.defaultPort
        }

        val normalizedPath = base.encodedPath.trimEnd('/')
        val prefix = if (normalizedPath.endsWith("/api")) {
            normalizedPath.removeSuffix("/api")
        } else {
            normalizedPath
        }

        val prefixSegment = prefix.trim('/').takeIf { it.isNotEmpty() }
        val socketPath = ensureLeadingSlash(
            buildList {
                if (prefixSegment != null) add(prefixSegment)
                add("socket")
                add("websocket")
            }.joinToString(separator = "/")
        )

        return URLBuilder().apply {
            protocol = socketProtocol
            host = base.host
            if (explicitPort != null) {
                port = explicitPort
            }
            encodedPath = socketPath
        }.buildString()
    }

    private fun URLProtocol.isSecure(): Boolean = this == URLProtocol.HTTPS || this == URLProtocol.WSS

    private fun ensureLeadingSlash(path: String): String =
        if (path.startsWith("/")) path else "/$path"

    private fun emptyPayload(): JsonObject = buildJsonObject { }

    private data class AuthState(val token: String, val user: UserEntity)

    @Serializable
    private data class PhoenixMessage(
        val topic: String,
        val event: String,
        val payload: JsonElement = JsonNull,
        @SerialName("ref") val ref: String? = null,
        @SerialName("join_ref") val joinRef: String? = null
    )

    private class RefGenerator {
        @OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
        private val counter = kotlin.concurrent.atomics.AtomicInt(0)

        @OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
        fun nextRef(): String = counter.addAndFetch(1).toString()
    }
}
