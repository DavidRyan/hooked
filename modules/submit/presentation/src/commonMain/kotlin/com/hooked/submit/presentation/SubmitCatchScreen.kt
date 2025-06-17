package com.hooked.submit.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hooked.core.components.AsyncImage
import com.hooked.core.nav.Screens
import kotlinx.coroutines.flow.collectLatest
import com.hooked.submit.presentation.model.SubmitCatchEffect
import com.hooked.submit.presentation.model.SubmitCatchIntent
import com.hooked.theme.HookedTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitCatchScreen(
    modifier: Modifier = Modifier,
    viewModel: SubmitCatchViewModel,
    navigate: (Screens) -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SubmitCatchEffect.NavigateBack -> {
                    navigate(Screens.CatchGrid)
                }
                is SubmitCatchEffect.ShowError -> {
                }
                is SubmitCatchEffect.CatchSubmittedSuccessfully -> {
                    navigate(Screens.CatchGrid)
                }
                else -> {}
            }
        }
    }
    
    Column(
        modifier = modifier
            .background(HookedTheme.background)
            .fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Submit New Catch") },
            navigationIcon = {
                IconButton(onClick = { 
                    viewModel.sendIntent(SubmitCatchIntent.NavigateBack) 
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = HookedTheme.primary,
                titleContentColor = HookedTheme.onPrimary,
                navigationIconContentColor = HookedTheme.onPrimary
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PhotoSection(
                photoUri = state.photoUri,
                onPickPhoto = { viewModel.sendIntent(SubmitCatchIntent.PickPhoto) }
            )
            
            OutlinedTextField(
                value = state.species,
                onValueChange = { viewModel.sendIntent(SubmitCatchIntent.UpdateSpecies(it)) },
                label = { Text("Species") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = state.weight,
                onValueChange = { viewModel.sendIntent(SubmitCatchIntent.UpdateWeight(it)) },
                label = { Text("Weight (kg)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            
            OutlinedTextField(
                value = state.length,
                onValueChange = { viewModel.sendIntent(SubmitCatchIntent.UpdateLength(it)) },
                label = { Text("Length (cm)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            
            LocationSection(
                locationText = state.locationText,
                isLoading = state.isLocationLoading,
                onGetLocation = { viewModel.sendIntent(SubmitCatchIntent.GetCurrentLocation) }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { viewModel.sendIntent(SubmitCatchIntent.SubmitCatch) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isFormValid && !state.isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HookedTheme.primary
                )
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = HookedTheme.onPrimary
                    )
                } else {
                    Text("Submit Catch")
                }
            }
        }
    }
}

@Composable
private fun PhotoSection(
    photoUri: String?,
    onPickPhoto: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (photoUri != null) {
                AsyncImage(
                    imageUrl = photoUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onPickPhoto) {
                        Text("Choose Different")
                    }
                }
            } else {
                Icon(
                    modifier = Modifier.size(40.dp)
                        .align(Alignment.CenterHorizontally),
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = HookedTheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    text = "Add a photo of your catch",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onPickPhoto) {
                        Text("Choose from Gallery")
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationSection(
    locationText: String,
    isLoading: Boolean,
    onGetLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                tint = HookedTheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = HookedTheme.primary
                )
            } else {
                TextButton(onClick = onGetLocation) {
                    Text("Get Location")
                }
            }
        }
    }
}