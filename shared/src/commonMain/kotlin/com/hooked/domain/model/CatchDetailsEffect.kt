package com.hooked.domain.model

sealed class CatchDetailsEffect {
    data class OnError(val message: String) : CatchDetailsEffect()
}