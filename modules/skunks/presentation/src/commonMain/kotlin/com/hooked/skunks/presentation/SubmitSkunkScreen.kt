package com.hooked.skunks.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hooked.core.datetime.NativeDateTimePickerField
import com.hooked.core.location.LocationPermissionRequester
import com.hooked.core.map.NativeMapPicker
import com.hooked.core.nav.Screens
import com.hooked.core.presentation.toast.ToastManager
import com.hooked.skunks.presentation.model.SubmitSkunkEffect
import com.hooked.skunks.presentation.model.SubmitSkunkIntent
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitSkunkScreen(
    modifier: Modifier = Modifier,
    viewModel: SubmitSkunkViewModel,
    navigate: (Screens) -> Unit,
    toastManager: ToastManager = koinInject()
) {
    val state by viewModel.state.collectAsState()
    var showMapPicker by remember { mutableStateOf(false) }

    LocationPermissionRequester(
        onPermissionResult = { granted ->
            viewModel.onLocationPermissionResult(granted)
            if (!granted) {
                toastManager.showError("Location permission is required to get your position")
            }
        }
    ) { requestPermission ->

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SubmitSkunkEffect.SubmitSuccess -> {
                    toastManager.showSuccess("Skunk logged!")
                    navigate(Screens.CatchGrid)
                }
                is SubmitSkunkEffect.SubmitError -> {
                    toastManager.showError(effect.message)
                }
                is SubmitSkunkEffect.RequestLocationPermission -> {
                    requestPermission()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Log a Skunk") },
            navigationIcon = {
                IconButton(onClick = { navigate(Screens.CatchGrid) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date/Time section
            DateTimeSection(
                fishedAt = state.fishedAt,
                onFishedAtChanged = { viewModel.sendIntent(SubmitSkunkIntent.UpdateFishedAt(it)) }
            )

            // Location section
            LocationSection(
                hasLocation = state.hasLocation,
                locationName = state.locationName,
                isLoading = state.isLocationLoading,
                onGetLocation = { viewModel.sendIntent(SubmitSkunkIntent.GetCurrentLocation) },
                onToggleMapPicker = { showMapPicker = !showMapPicker },
                isMapPickerOpen = showMapPicker
            )

            if (showMapPicker) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    NativeMapPicker(
                        latitude = state.latitude,
                        longitude = state.longitude,
                        onLocationSelected = { lat, lon ->
                            viewModel.sendIntent(
                                SubmitSkunkIntent.UpdateLocationFromMap(lat, lon)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                }
            }

            // Notes section
            NotesSection(
                notes = state.notes,
                onNotesChanged = { viewModel.sendIntent(SubmitSkunkIntent.UpdateNotes(it)) }
            )

            // Error message
            state.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit button
            Button(
                onClick = { viewModel.sendIntent(SubmitSkunkIntent.Submit) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isValid && !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Log Skunk")
                }
            }
        }
    }
    } // LocationPermissionRequester
}

@Composable
private fun DateTimeSection(
    fishedAt: String,
    onFishedAtChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "When did you fish?",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            NativeDateTimePickerField(
                value = fishedAt,
                onValueChange = onFishedAtChanged,
                modifier = Modifier.fillMaxWidth(),
                label = "Date/Time"
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Preview: ${formatDateTime(fishedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocationSection(
    hasLocation: Boolean,
    locationName: String,
    isLoading: Boolean,
    onGetLocation: () -> Unit,
    onToggleMapPicker: () -> Unit,
    isMapPickerOpen: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (hasLocation) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (hasLocation) locationName else "Tap to get your current location",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasLocation) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onGetLocation) {
                        Text(if (hasLocation) "Update" else "Get Location")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onToggleMapPicker) {
                        Text(if (isMapPickerOpen) "Hide Map" else "Pick on Map")
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesSection(
    notes: String,
    onNotesChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Notes (optional)",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What happened? Conditions, lures tried, etc.") },
                minLines = 3,
                maxLines = 5
            )
        }
    }
}

private fun formatDateTime(isoString: String): String {
    // Simple display format — parse the ISO string for a human-readable version
    return try {
        // ISO format: 2026-02-07T14:30:00
        val parts = isoString.split("T")
        if (parts.size == 2) {
            val datePart = parts[0] // 2026-02-07
            val timePart = parts[1].substringBefore(".").take(5) // 14:30
            "$datePart at $timePart"
        } else {
            isoString
        }
    } catch (e: Exception) {
        isoString
    }
}
