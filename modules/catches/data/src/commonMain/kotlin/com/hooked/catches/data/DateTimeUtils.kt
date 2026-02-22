@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.hooked.catches.data

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant

fun parseCaughtAtToTimestamp(caughtAt: String?): Long? {
    if (caughtAt.isNullOrBlank()) {
        return null
    }

    return runCatching {
        Instant.parse(caughtAt).toEpochMilliseconds()
    }.getOrElse { _: Throwable ->
        runCatching {
            LocalDateTime.parse(caughtAt)
                .toInstant(TimeZone.UTC)
                .toEpochMilliseconds()
        }.getOrNull() ?: runCatching {
            LocalDate.parse(caughtAt)
                .atStartOfDayIn(TimeZone.UTC)
                .toEpochMilliseconds()
        }.getOrNull()
    }
}
