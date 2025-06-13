package com.hooked.domain

sealed class CatchDetailsEffect {
    data class OnError(val message: String) : CatchDetailsEffect()
}