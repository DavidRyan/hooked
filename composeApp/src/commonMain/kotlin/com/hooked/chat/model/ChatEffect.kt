package com.hooked.chat.model

sealed class ChatEffect {
    object ScrollToBottom : ChatEffect()
    data class ShowError(val message: String) : ChatEffect()
}
