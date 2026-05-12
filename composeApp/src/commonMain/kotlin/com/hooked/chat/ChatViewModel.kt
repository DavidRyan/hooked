package com.hooked.chat

import com.hooked.chat.model.ChatEffect
import com.hooked.chat.model.ChatIntent
import com.hooked.chat.model.ChatState
import com.hooked.core.HookedViewModel
import com.hooked.core.logging.Logger
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ChatViewModel(
    private val client: ChatSocketClient
) : HookedViewModel<ChatIntent, ChatState, ChatEffect>() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    init {
        observeEvents()
        connect()
    }

    override fun createInitialState() = ChatState()

    override fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.UpdateInput -> setState { copy(inputText = intent.text) }
            is ChatIntent.SendMessage -> sendMessage(intent.text)
            ChatIntent.ClearChat -> setState {
                copy(messages = emptyList(), activeToolCall = null, isAssistantThinking = false)
            }
            ChatIntent.Reconnect -> {
                client.close()
                setState { copy(isConnecting = true, connectionError = null) }
                connect()
            }
        }
    }

    private fun connect() {
        viewModelScope.launch {
            client.connect()
            setState { copy(isConnecting = false) }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            client.events.collect { event ->
                when (event) {
                    is ChatEvent.ToolCall -> setState {
                        copy(activeToolCall = event.name)
                    }
                    is ChatEvent.Message -> {
                        appendAssistantMessage(event.content)
                        sendEffect { ChatEffect.ScrollToBottom }
                    }
                    is ChatEvent.Error -> {
                        setState {
                            copy(
                                isAssistantThinking = false,
                                activeToolCall = null,
                                connectionError = event.message
                            )
                        }
                        sendEffect { ChatEffect.ShowError(event.message) }
                    }
                    ChatEvent.Closed -> {
                        Logger.info(TAG, "WS closed")
                        setState { copy(connectionError = "Disconnected") }
                    }
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return
        val trimmed = text.trim()
        appendUserMessage(trimmed)
        setState {
            copy(
                inputText = "",
                isAssistantThinking = true,
                activeToolCall = null,
                connectionError = null
            )
        }
        viewModelScope.launch {
            client.send(trimmed)
            sendEffect { ChatEffect.ScrollToBottom }
        }
    }

    private fun appendUserMessage(content: String) {
        setState {
            copy(messages = messages + ChatMessage(
                id = generateId(),
                role = ChatRole.User,
                content = content
            ))
        }
    }

    private fun appendAssistantMessage(content: String) {
        setState {
            copy(
                messages = messages + ChatMessage(
                    id = generateId(),
                    role = ChatRole.Assistant,
                    content = content
                ),
                isAssistantThinking = false,
                activeToolCall = null
            )
        }
    }

    private fun generateId(): String = "msg-${kotlin.random.Random.nextLong()}"

    override fun onCleared() {
        client.close()
    }
}
