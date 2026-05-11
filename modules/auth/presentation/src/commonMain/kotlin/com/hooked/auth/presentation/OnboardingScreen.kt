package com.hooked.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hooked.auth.domain.entities.OnboardingPreferences
import com.hooked.auth.domain.usecases.UpdatePreferencesUseCase
import com.hooked.core.location.LocationPermissionRequester
import com.hooked.core.map.NativeMapPicker
import com.hooked.theme.Colors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private val SPECIES_OPTIONS = listOf(
    "Bass", "Walleye", "Trout", "Panfish", "Pike", "Musky",
    "Catfish", "Salmon", "Crappie", "Perch"
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    updatePreferences: UpdatePreferencesUseCase = koinInject()
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    var homeLat by remember { mutableStateOf<Double?>(null) }
    var homeLng by remember { mutableStateOf<Double?>(null) }
    var selectedSpecies by remember { mutableStateOf<Set<String>>(emptySet()) }
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    fun submitAndContinue() {
        if (isSubmitting) return
        isSubmitting = true
        submitError = null
        scope.launch {
            val result = updatePreferences(
                OnboardingPreferences(
                    homeLat = homeLat,
                    homeLng = homeLng,
                    targetSpecies = selectedSpecies.toList().takeIf { it.isNotEmpty() },
                    onboardingCompleted = true
                )
            )
            isSubmitting = false
            if (result.isSuccess) {
                onComplete()
            } else {
                submitError = result.exceptionOrNull()?.message ?: "Couldn't save preferences"
                // Still let the user proceed — onboarding is non-blocking
                onComplete()
            }
        }
    }

    fun next() {
        scope.launch {
            if (pagerState.currentPage < 2) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            } else {
                submitAndContinue()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.base)
    ) {
        Spacer(Modifier.height(48.dp))

        StepDots(currentPage = pagerState.currentPage, total = 3)

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> HomeWaterStep(
                    selectedLat = homeLat,
                    selectedLng = homeLng,
                    onPicked = { lat, lng ->
                        homeLat = lat
                        homeLng = lng
                    }
                )
                1 -> SpeciesStep(
                    selected = selectedSpecies,
                    onToggle = { species ->
                        selectedSpecies = if (species in selectedSpecies) {
                            selectedSpecies - species
                        } else {
                            selectedSpecies + species
                        }
                    }
                )
                2 -> PermissionsStep(
                    locationGranted = locationPermissionGranted,
                    onLocationGranted = { locationPermissionGranted = it }
                )
            }
        }

        submitError?.let {
            Text(
                text = it,
                color = Colors.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { next() }, enabled = !isSubmitting) {
                Text(
                    text = if (pagerState.currentPage < 2) "Skip" else "Done",
                    color = Colors.subtext1
                )
            }
            Button(
                onClick = { next() },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Colors.primary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Colors.onPrimary
                    )
                } else {
                    Text(
                        text = if (pagerState.currentPage < 2) "Next" else "Get started",
                        color = Colors.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun StepDots(currentPage: Int, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(total) { index ->
            val active = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (active) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (active) Colors.primary else Colors.overlay1)
            )
        }
    }
}

@Composable
private fun HomeWaterStep(
    selectedLat: Double?,
    selectedLng: Double?,
    onPicked: (Double, Double) -> Unit
) {
    StepScaffold(
        title = "Where do you fish?",
        subtitle = "Drop a pin where you fish most. We'll use it to set up your default view.",
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                NativeMapPicker(
                    latitude = selectedLat,
                    longitude = selectedLng,
                    onLocationSelected = onPicked,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    )
}

@Composable
private fun SpeciesStep(
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    StepScaffold(
        title = "What do you target?",
        subtitle = "Pick a few species you usually fish for. Tap to toggle.",
        content = {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SPECIES_OPTIONS.forEach { species ->
                    FilterChip(
                        selected = species in selected,
                        onClick = { onToggle(species) },
                        label = { Text(species) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Colors.primary.copy(alpha = 0.2f),
                            selectedLabelColor = Colors.primary,
                            containerColor = Colors.surface1,
                            labelColor = Colors.subtext1
                        )
                    )
                }
            }
        }
    )
}

@Composable
private fun PermissionsStep(
    locationGranted: Boolean,
    onLocationGranted: (Boolean) -> Unit
) {
    LocationPermissionRequester(
        onPermissionResult = onLocationGranted
    ) { requestPermission ->
        StepScaffold(
            title = "Permissions",
            subtitle = "Hooked uses your location to record where you fish, and the camera to photograph catches.",
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PermissionCard(
                        title = "Location",
                        body = "So we can save where each catch happened.",
                        granted = locationGranted,
                        onRequest = requestPermission
                    )
                    PermissionCard(
                        title = "Camera",
                        body = "You'll be asked when you take your first photo.",
                        granted = false,
                        onRequest = null
                    )
                }
            }
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    granted: Boolean,
    onRequest: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Colors.surface1)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = Colors.text)
            Text(text = body, style = MaterialTheme.typography.bodyMedium, color = Colors.subtext1)
        }
        if (onRequest != null) {
            if (granted) {
                Text("✓", color = Colors.primary, style = MaterialTheme.typography.titleLarge)
            } else {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = Colors.primary)
                ) {
                    Text("Allow", color = Colors.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun StepScaffold(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = Colors.text
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = Colors.subtext1
        )
        content()
    }
}
