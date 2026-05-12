package com.hooked.chat.model

sealed class ChatIntent {
    data class UpdateInput(val text: String) : ChatIntent()
    data class SendMessage(val text: String) : ChatIntent()
    object ClearChat : ChatIntent()
    object Reconnect : ChatIntent()
}
