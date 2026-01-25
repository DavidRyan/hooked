package com.hooked.catches.presentation

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val catchDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)

actual fun formatCatchDate(timestamp: Long): String {
    return catchDateFormatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
}
