package com.hooked.core.presentation.toast

import kotlin.random.Random

data class ToastMessage(
    val id: String = Random.nextLong().toString(),
    val message: String,
    val type: ToastType = ToastType.INFO,
    val duration: Long = 3000L // 3 seconds default
)
