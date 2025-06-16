package com.hooked.features.catches.presentation.model

sealed class CatchDetailsEffect {
    data class OnError(val message: String) : CatchDetailsEffect()
}