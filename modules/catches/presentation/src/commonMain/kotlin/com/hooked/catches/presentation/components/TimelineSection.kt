@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.hooked.catches.presentation.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hooked.catches.presentation.model.CatchModel
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Plain 2-column grid of all catches, sorted most-recent first. No day grouping,
 * tight gutters. Maximizes screen real estate on the home screen.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.TimelineSection(
    catches: List<CatchModel>,
    onCatchClick: (String) -> Unit,
    onCatchLongClick: (String) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
) {
    val sorted = catches.sortedByDescending { parseLocalDate(it.dateCaught)?.toEpochDays() ?: Int.MIN_VALUE }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(items = sorted, key = { "tile-${it.id}" }) { catch ->
            TimelineCatchTile(
                catch = catch,
                onClick = { onCatchClick(catch.id) },
                onLongClick = { onCatchLongClick(catch.id) },
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

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
