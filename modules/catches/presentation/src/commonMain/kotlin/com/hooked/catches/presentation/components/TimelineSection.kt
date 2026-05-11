@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.hooked.catches.presentation.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hooked.catches.presentation.model.CatchModel
import com.hooked.theme.Colors
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.TimelineSection(
    catches: List<CatchModel>,
    onCatchClick: (String) -> Unit,
    onCatchLongClick: (String) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
) {
    val grouped = remember(catches) { groupByDay(catches) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        grouped.forEach { (day, entries) ->
            item(
                key = "header-${day?.toString() ?: "undated"}",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = formatSectionHeader(day),
                        style = MaterialTheme.typography.labelLarge,
                        color = Colors.subtext0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }
            items(items = entries, key = { "tile-${it.id}" }) { catch ->
                TimelineCatchTile(
                    catch = catch,
                    onClick = { onCatchClick(catch.id) },
                    onLongClick = { onCatchLongClick(catch.id) },
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
    }
}

private fun groupByDay(catches: List<CatchModel>): Map<LocalDate?, List<CatchModel>> =
    catches
        .map { it to parseLocalDate(it.dateCaught) }
        .sortedByDescending { (_, date) -> date?.toEpochDays() ?: Int.MIN_VALUE }
        .groupBy({ it.second }, { it.first })

internal fun parseLocalDate(dateStr: String?): LocalDate? {
    if (dateStr.isNullOrBlank()) return null
    return runCatching {
        Instant.parse(dateStr).toLocalDateTime(TimeZone.currentSystemDefault()).date
    }.getOrElse {
        runCatching { LocalDateTime.parse(dateStr).date }.getOrNull()
            ?: runCatching { LocalDate.parse(dateStr) }.getOrNull()
    }
}

internal fun parseLocalDateTime(dateStr: String?): LocalDateTime? {
    if (dateStr.isNullOrBlank()) return null
    return runCatching {
        Instant.parse(dateStr).toLocalDateTime(TimeZone.currentSystemDefault())
    }.getOrElse {
        runCatching { LocalDateTime.parse(dateStr) }.getOrNull()
    }
}

internal fun formatSectionHeader(date: LocalDate?): String {
    if (date == null) return "Undated"
    return "${weekdayName(date.dayOfWeek)} · ${monthName(date.month)} ${date.dayOfMonth}"
}

internal fun formatTimeOfDay(dateStr: String?): String? {
    val dt = parseLocalDateTime(dateStr) ?: return null
    val hour24 = dt.hour
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    val suffix = if (hour24 < 12) "AM" else "PM"
    val minute = dt.minute.toString().padStart(2, '0')
    return "$hour12:$minute $suffix"
}

private fun weekdayName(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "Monday"
    DayOfWeek.TUESDAY -> "Tuesday"
    DayOfWeek.WEDNESDAY -> "Wednesday"
    DayOfWeek.THURSDAY -> "Thursday"
    DayOfWeek.FRIDAY -> "Friday"
    DayOfWeek.SATURDAY -> "Saturday"
    DayOfWeek.SUNDAY -> "Sunday"
}

private fun monthName(month: Month): String = when (month) {
    Month.JANUARY -> "January"
    Month.FEBRUARY -> "February"
    Month.MARCH -> "March"
    Month.APRIL -> "April"
    Month.MAY -> "May"
    Month.JUNE -> "June"
    Month.JULY -> "July"
    Month.AUGUST -> "August"
    Month.SEPTEMBER -> "September"
    Month.OCTOBER -> "October"
    Month.NOVEMBER -> "November"
    Month.DECEMBER -> "December"
}
