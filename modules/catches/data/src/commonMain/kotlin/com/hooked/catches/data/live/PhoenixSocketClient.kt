package com.hooked.catches.data.live

import com.hooked.core.config.NetworkConfig
import com.hooked.core.logging.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.takeFrom
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import io.ktor.websocket.send
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

interface BaseUrlProvider {
    val baseUrl: String
}

class NetworkBaseUrlProvider : BaseUrlProvider {
    override val baseUrl: String = NetworkConfig.BASE_URL
}

data class SocketAuth(val token: String, val userId: String)

interface AuthProvider {
    suspend fun currentAuth(): SocketAuth?
}

class PhoenixSocketClient(
    private val httpClient: HttpClient,
    private val json: Json,
    private val baseUrlProvider: BaseUrlProvider
) {
    companion object {
        private const val TAG = "CatchEnrichmentSocket"
        private const val HEARTBEAT_INTERVAL_MS = 30_000L
        private const val MAX_RETRY_DELAY_MS = 5_000L
    }

    fun messages(
        topicFor: (SocketAuth) -> String,
        authProvider: AuthProvider
    ): Flow<PhoenixMessage> = channelFlow {
        var attempt = 0

        while (isActive) {
            val auth = authProvider.currentAuth()
            if (auth == null) {
                Logger.warning(TAG, "Cannot open websocket without authentication")
                close()
                return@channelFlow
            }

            val socketUrl = buildSocketUrl(baseUrlProvider.baseUrl)
            val topic = topicFor(auth)

            try {
                attempt += 1
                Logger.debug(TAG, "Opening websocket to $socketUrl for topic=$topic attempt=$attempt")
                httpClient.webSocket(
                    request = {
                        url.takeFrom(socketUrl)
                        url.parameters.append("token", auth.token)
                        url.parameters.append("vsn", "2.0.0")
                    }
                ) {
                    Logger.info(TAG, "Connected to catch enrichment socket topic=$topic")
                    attempt = 0
                    maintainConnection(topic, this@channelFlow)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Logger.warning(TAG, "Socket disconnected: ${error.message}")
                val backoff = (attempt.coerceAtMost(5) * 1_000L).coerceAtMost(MAX_RETRY_DELAY_MS)
                Logger.debug(TAG, "Reconnecting in ${backoff}ms (attempt=$attempt)")
                delay(backoff)
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.maintainConnection(
        topic: String,
        producer: SendChannel<PhoenixMessage>
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

        Logger.debug(TAG, "Sent phx_join for topic=$topic joinRef=$joinRef")

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

                Logger.trace(TAG, "Sent heartbeat for topic=$topic")
            }
        }

        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> handleIncomingFrame(frame.readText(), topic, joinRef, producer, this)
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
            runCatching {
                sendMessage(
                    PhoenixMessage(
                        topic = topic,
                        event = "phx_leave",
                        payload = emptyPayload(),
                        ref = refGenerator.nextRef(),
                        joinRef = joinRef
                    )
                )
                close(CloseReason(CloseReason.Codes.NORMAL, "client_closed"))
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.sendMessage(message: PhoenixMessage) {
        val encoded = json.encodeToString(PhoenixMessage.serializer(), message)
        send(Frame.Text(encoded))
    }

    private suspend fun handleIncomingFrame(
        payload: String,
        topic: String,
        joinRef: String,
        producer: SendChannel<PhoenixMessage>,
        session: DefaultClientWebSocketSession
    ) {
        val message = runCatching { json.decodeFromString<PhoenixMessage>(payload) }.getOrNull()
        if (message == null) {
            Logger.warning(TAG, "Failed to decode Phoenix message: $payload")
            return
        }

        Logger.debug(TAG, "Received event=${message.event} topic=${message.topic} ref=${message.ref}")

        if (message.event == "phx_reply" && message.ref == joinRef) {
            val status = message.payload.jsonObject["status"]?.jsonPrimitive?.contentOrNull
            if (status != null && status != "ok") {
                Logger.error(TAG, "Join rejected for topic $topic: ${message.payload}")
                producer.close()
                session.close(CloseReason(CloseReason.Codes.NORMAL, "join_rejected"))
            }
            if (status == "ok") {
                Logger.debug(TAG, "Join acknowledged for topic=$topic")
            }
            return
        }

        if (message.topic != topic) {
            Logger.trace(TAG, "Ignoring message for other topic ${message.topic}")
            return
        }

        val result = producer.trySend(message)
        if (result.isFailure) {
            Logger.warning(TAG, "Dropped socket message for $topic: ${result.exceptionOrNull()?.message ?: "channel closed"}")
        }
    }

    private fun buildSocketUrl(apiBaseUrl: String): String {
        val base = URLBuilder(apiBaseUrl)
        val socketProtocol = if (base.protocol.isSecure()) URLProtocol.WSS else URLProtocol.WS

        val explicitPort = base.port.takeIf { port ->
            port != 0 && port != base.protocol.defaultPort
        }

        val baseSegments = base.encodedPathSegments.filter { it.isNotEmpty() }
        val prefixSegments = if (baseSegments.lastOrNull() == "api") {
            baseSegments.dropLast(1)
        } else {
            baseSegments
        }

        val socketSegments = buildList {
            addAll(prefixSegments)
            add("socket")
            add("websocket")
        }

        return URLBuilder().apply {
            protocol = socketProtocol
            host = base.host
            if (explicitPort != null) {
                port = explicitPort
            }
            encodedPathSegments = socketSegments
            if (!base.parameters.isEmpty()) {
                parameters.appendAll(base.parameters.build())
            }
        }.buildString()
    }

    private fun URLProtocol.isSecure(): Boolean = this == URLProtocol.HTTPS || this == URLProtocol.WSS

    private fun emptyPayload(): JsonObject = buildJsonObject { }
}

// Phoenix V2 wire format: [join_ref, ref, topic, event, payload]
@Serializable(with = PhoenixMessageSerializer::class)
data class PhoenixMessage(
    val topic: String,
    val event: String,
    val payload: JsonElement = JsonNull,
    val ref: String? = null,
    val joinRef: String? = null
)

@OptIn(kotlinx.serialization.InternalSerializationApi::class)
private object PhoenixMessageSerializer : KSerializer<PhoenixMessage> {
    @OptIn(kotlinx.serialization.InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("PhoenixMessage", StructureKind.LIST)

    override fun serialize(encoder: Encoder, value: PhoenixMessage) {
        val jsonEncoder = encoder as JsonEncoder
        val array = buildJsonArray {
            add(value.joinRef?.let { JsonPrimitive(it) } ?: JsonNull)
            add(value.ref?.let { JsonPrimitive(it) } ?: JsonNull)
            add(JsonPrimitive(value.topic))
            add(JsonPrimitive(value.event))
            add(value.payload)
        }
        jsonEncoder.encodeJsonElement(array)
    }

    override fun deserialize(decoder: Decoder): PhoenixMessage {
        val jsonDecoder = decoder as JsonDecoder
        val array = jsonDecoder.decodeJsonElement().jsonArray
        return PhoenixMessage(
            joinRef = array[0].jsonPrimitive.contentOrNull,
            ref    = array[1].jsonPrimitive.contentOrNull,
            topic  = array[2].jsonPrimitive.content,
            event  = array[3].jsonPrimitive.content,
            payload = array[4]
        )
    }
}

private class RefGenerator {
    @OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
    private val counter = kotlin.concurrent.atomics.AtomicInt(0)

    @OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
    fun nextRef(): String = counter.addAndFetch(1).toString()
}
