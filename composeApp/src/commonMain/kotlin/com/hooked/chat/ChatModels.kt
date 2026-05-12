package com.hooked.chat

enum class ChatRole { User, Assistant, ToolCall }

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val isPending: Boolean = false
)

sealed class ChatEvent {
    data class ToolCall(val name: String) : ChatEvent()
    data class Message(val content: String) : ChatEvent()
    data class Error(val message: String) : ChatEvent()
    object Closed : ChatEvent()
}
