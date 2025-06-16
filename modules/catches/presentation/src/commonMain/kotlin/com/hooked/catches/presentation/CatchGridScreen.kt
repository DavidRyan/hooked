package com.hooked.catches.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hooked.core.components.AsyncImage
import com.hooked.core.nav.Screens
import com.hooked.catches.presentation.model.CatchGridEffect
import com.hooked.catches.presentation.model.CatchGridIntent
import com.hooked.catches.presentation.model.CatchModel
import kotlinx.coroutines.flow.collectLatest
import com.hooked.theme.HookedTheme

@Composable
fun CatchGridScreen(
    modifier: Modifier = Modifier,
    viewModel: CatchGridViewModel,
    navigate : (Screens) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.sendIntent(CatchGridIntent.LoadCatches)
        viewModel.effect.collectLatest {
            when (it) {
                is CatchGridEffect.NavigateToCatchDetails ->
                    navigate(Screens.CatchDetails(it.catchId))

                else -> {}
            }
        }
    }
    
    Box(
        modifier = modifier
            .background(HookedTheme.background)
            .fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.catches) { catch ->
                CatchGridItem(
                    catch = catch,
                    onClick = {
                        viewModel.sendIntent(CatchGridIntent.NavigateToCatchDetails(it))
                    }
                )
            }
        }
        
        FloatingActionButton(
            onClick = { navigate(Screens.SubmitCatch) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new catch"
            )
        }
    }
}

@Composable
fun CatchGridItem(
    catch: CatchModel,
    onClick: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = { onClick(catch.id) }),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = HookedTheme.tertiary)
    ) {
        AsyncImage(
            imageUrl = catch.imageUrl,
            modifier = Modifier.fillMaxSize()
        )
    }
}