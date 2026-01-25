package com.hooked.catches.presentation

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.localeWithLocaleIdentifier

actual fun formatCatchDate(timestamp: Long): String {
    val formatter = NSDateFormatter().apply {
        locale = NSLocale.localeWithLocaleIdentifier("en_US_POSIX")
        dateFormat = "MMMM d, yyyy"
    }
    val secondsSince1970 = timestamp.toDouble() / 1000.0
    val secondsSinceReferenceDate = secondsSince1970 - SECONDS_BETWEEN_1970_AND_2001
    val date = NSDate(timeIntervalSinceReferenceDate = secondsSinceReferenceDate)
    return formatter.stringFromDate(date)
}

private const val SECONDS_BETWEEN_1970_AND_2001 = 978307200.0
