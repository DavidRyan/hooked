package com.hooked.chat.model

import com.hooked.chat.ChatMessage

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val activeToolCall: String? = null,
    val isAssistantThinking: Boolean = false,
    val inputText: String = "",
    val isConnecting: Boolean = true,
    val connectionError: String? = null
)
