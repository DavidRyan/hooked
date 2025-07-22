package com.hooked.core.presentation.toast

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class ToastMessage(
    val id: String = Clock.System.now().toEpochMilliseconds().toString(),
    val message: String,
    val type: ToastType = ToastType.INFO,
    val duration: Long = 3000L, // 3 seconds default
    val timestamp: Instant = Clock.System.now()
)