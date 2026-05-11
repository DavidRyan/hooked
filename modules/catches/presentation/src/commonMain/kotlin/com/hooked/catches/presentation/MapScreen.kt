package com.hooked.catches.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hooked.catches.domain.entities.CatchEntity
import com.hooked.catches.domain.usecases.GetCatchesUseCase
import com.hooked.core.components.AsyncImage
import com.hooked.core.config.AppConfig
import com.hooked.core.domain.UseCaseResult
import com.hooked.theme.Colors
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    contentPadding: PaddingValues = PaddingValues(),
    getCatchesUseCase: GetCatchesUseCase = koinInject()
) {
    var catches by remember { mutableStateOf<List<CatchEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        when (val result = getCatchesUseCase()) {
            is UseCaseResult.Success -> {
                catches = result.data
                isLoading = false
            }
            is UseCaseResult.Error -> {
                catches = emptyList()
                isLoading = false
            }
        }
    }

    val placed = remember(catches) {
        catches.filter { it.latitude != null && it.longitude != null }
    }
    val grouped = remember(placed) { groupByLocation(placed) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Map",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Colors.text
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Colors.text
                )
            )
        },
        containerColor = Colors.base
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(contentPadding)
        ) {
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Colors.primary)
                }
                placed.isEmpty() -> EmptyMapState()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item("map") {
                        MultiPinMap(catches = placed)
                    }
                    item("subheader") {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${grouped.size} ${if (grouped.size == 1) "place" else "places"} fished",
                            style = MaterialTheme.typography.labelLarge,
                            color = Colors.subtext0
                        )
                    }
                    items(items = grouped, key = { it.label }) { group ->
                        LocationRow(group = group)
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiPinMap(catches: List<CatchEntity>, modifier: Modifier = Modifier) {
    val url = remember(catches) { buildMultiPinUrl(catches) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(Colors.surface1)
    ) {
        if (url != null) {
            AsyncImage(
                imageUrl = url,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "Map unavailable",
                modifier = Modifier.align(Alignment.Center),
                color = Colors.subtext1,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LocationRow(group: LocationGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Colors.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = group.count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = Colors.primary
            )
        }
        Column(
            modifier = Modifier.padding(0.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = group.label,
                style = MaterialTheme.typography.titleMedium,
                color = Colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (group.count == 1) "1 catch" else "${group.count} catches",
                style = MaterialTheme.typography.bodyMedium,
                color = Colors.subtext1
            )
        }
    }
}

@Composable
private fun EmptyMapState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No locations yet",
            style = MaterialTheme.typography.headlineMedium,
            color = Colors.text
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Catches you log with location data will appear here.",
            style = MaterialTheme.typography.bodyLarge,
            color = Colors.subtext1,
            textAlign = TextAlign.Center
        )
    }
}

private data class LocationGroup(
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val count: Int
)

private fun groupByLocation(catches: List<CatchEntity>): List<LocationGroup> =
    catches
        .groupBy { (it.location ?: "").trim().ifBlank { "Unknown location" } }
        .map { (label, list) ->
            // Centroid of all catches at this label (in case lat/lng vary slightly).
            val avgLat = list.mapNotNull { it.latitude }.average()
            val avgLng = list.mapNotNull { it.longitude }.average()
            LocationGroup(
                label = label,
                latitude = avgLat,
                longitude = avgLng,
                count = list.size
            )
        }
        .sortedByDescending { it.count }

private const val MAP_WIDTH = 600
private const val MAP_HEIGHT = 450
private const val MARKER_COLOR = "f5a85c" // Sunrise amber, matches HookedTheme primary
private const val MAX_PINS = 25 // Mapbox URL length cap (~8KB total URL)

private fun buildMultiPinUrl(catches: List<CatchEntity>): String? {
    val pins = catches
        .mapNotNull { c ->
            val lat = c.latitude ?: return@mapNotNull null
            val lng = c.longitude ?: return@mapNotNull null
            if (lat !in -90.0..90.0 || lng !in -180.0..180.0) null else lat to lng
        }
        .take(MAX_PINS)

    if (pins.isEmpty()) return null

    val pinSegment = pins.joinToString(",") { (lat, lng) ->
        "pin-s+$MARKER_COLOR($lng,$lat)"
    }

    return buildString {
        append("https://api.mapbox.com/styles/v1/mapbox/outdoors-v12/static/")
        append("$pinSegment/")
        // "auto" lets Mapbox pick a bounding box that contains all pins
        append("auto/")
        append("${MAP_WIDTH}x$MAP_HEIGHT@2x")
        append("?access_token=${AppConfig.MAPBOX_ACCESS_TOKEN}")
        append("&padding=40")
        append("&attribution=false")
        append("&logo=false")
    }
}
