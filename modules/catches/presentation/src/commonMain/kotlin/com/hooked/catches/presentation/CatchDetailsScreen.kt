package com.hooked.catches.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hooked.core.components.AsyncImage
import com.hooked.core.nav.Screens
import com.hooked.catches.presentation.model.CatchDetailsIntent
import com.hooked.theme.HookedTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchDetailsScreen(
    viewModel: CatchDetailsViewModel, 
    catchId: Long,
    navigate: (Screens) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sendIntent(CatchDetailsIntent.LoadCatchDetails(catchId))
    }

    Column(
        modifier = Modifier
            .background(HookedTheme.background)
            .fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Catch Details") },
            navigationIcon = {
                IconButton(onClick = { navigate(Screens.CatchGrid) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = HookedTheme.primary,
                titleContentColor = HookedTheme.onPrimary,
                navigationIconContentColor = HookedTheme.onPrimary
            )
        )
        
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = HookedTheme.primary)
            }
        } else {
            state.catchDetails?.let { details ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Photo Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        AsyncImage(
                            imageUrl = details.photoUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                    
                    // Species Section
                    DetailCard(
                        label = "Species",
                        value = details.species
                    )
                    
                    // Weight Section
                    DetailCard(
                        label = "Weight",
                        value = "${details.weight} kg"
                    )
                    
                    // Length Section
                    DetailCard(
                        label = "Length", 
                        value = "${details.length} cm"
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailCard(
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = HookedTheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}