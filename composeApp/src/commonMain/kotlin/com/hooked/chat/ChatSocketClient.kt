package com.hooked.chat

import com.hooked.auth.data.storage.TokenStorage
import com.hooked.core.config.AppConfig
import com.hooked.core.logging.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Ktor WebSocket client for chat-server. Single session per instance.
 *
 * Lifecycle:
 *   val client = ChatSocketClient(...)
 *   val events = client.events
 *   client.connect()           // suspending — returns once handshake done
 *   client.send("hello")
 *   client.close()
 */
class ChatSocketClient(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) {
    companion object {
        private const val TAG = "ChatSocketClient"
    }

    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 16)
    val events: Flow<ChatEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var session: DefaultClientWebSocketSession? = null
    private var readerJob: Job? = null

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun connect() {
        if (session != null) {
            Logger.warning(TAG, "connect() called but session already exists")
            return
        }
        val token = tokenStorage.getToken()
        if (token.isNullOrBlank()) {
            _events.emit(ChatEvent.Error("Not authenticated"))
            return
        }
        val url = "${AppConfig.CHAT_BASE_URL}/chat?token=$token"
        Logger.info(TAG, "Connecting to $url")

        try {
            session = httpClient.webSocketSession { url(url) }
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to open WS: ${e.message}", e)
            _events.emit(ChatEvent.Error("Couldn't reach chat server"))
            return
        }

        readerJob = scope.launch {
            try {
                val active = session ?: return@launch
                for (frame in active.incoming) {
                    if (frame !is Frame.Text) continue
                    val text = frame.readText()
                    decode(text)?.let { _events.emit(it) }
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Reader error: ${e.message}", e)
                _events.emit(ChatEvent.Error(e.message ?: "Connection error"))
            } finally {
                // Channel closed (or errored). Drop state so the next send triggers a reconnect.
                session = null
                readerJob = null
                _events.emit(ChatEvent.Closed)
            }
        }
    }

    suspend fun send(text: String) {
        // Lazy reconnect — if the session dropped (Phoenix restart, network blip),
        // try once before erroring out.
        if (session == null) {
            connect()
        }
        val active = session
        if (active == null) {
            _events.emit(ChatEvent.Error("Not connected"))
            return
        }
        try {
            active.send(text)
        } catch (e: Exception) {
            Logger.error(TAG, "Send failed: ${e.message}", e)
            _events.emit(ChatEvent.Error(e.message ?: "Send failed"))
        }
    }

    fun close() {
        runBlocking {
            runCatching { session?.close() }
            readerJob?.cancel()
            session = null
            readerJob = null
        }
    }

    private fun decode(raw: String): ChatEvent? {
        return try {
            val obj = json.parseToJsonElement(raw).jsonObject
            when (obj["type"]?.string()) {
                "tool_call" -> ChatEvent.ToolCall(name = obj["name"]?.string() ?: "")
                "message" -> ChatEvent.Message(content = obj["content"]?.string() ?: "")
                "error" -> ChatEvent.Error(message = obj["message"]?.string() ?: "Unknown error")
                else -> null
            }
        } catch (e: Exception) {
            Logger.warning(TAG, "Failed to decode chat frame: $raw — ${e.message}")
            null
        }
    }

    private fun JsonElement.string(): String? = (this as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
}
