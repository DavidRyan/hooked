package com.hooked.catches.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import kotlinx.coroutines.delay
import com.hooked.catches.presentation.model.CatchDetailsIntent
import com.hooked.catches.presentation.model.CatchGridEffect
import com.hooked.catches.presentation.model.CatchGridIntent
import com.hooked.catches.presentation.model.CatchModel
import com.hooked.core.components.AsyncImage
import com.hooked.core.nav.Screens
import com.hooked.theme.HookedTheme
import com.hooked.core.util.BackHandler
import com.hooked.core.animation.AnimationConstants
import com.hooked.core.animation.AnimationSpecs
import com.hooked.catches.presentation.components.CatchGridItem
import com.hooked.catches.presentation.components.AnimatedDetailCard
import com.hooked.catches.presentation.components.StaticMapCard
import com.hooked.catches.presentation.state.rememberCatchesScreenState
import com.hooked.core.presentation.toast.ToastManager
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

sealed class CatchesScreenState {
    object Grid : CatchesScreenState()
    data class Details(val catchId: String) : CatchesScreenState()
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CatchesScreen(
    navigate: (Screens) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val stateManager = rememberCatchesScreenState()
    
    with(sharedTransitionScope) {
        AnimatedContent(
            targetState = stateManager.screenState,
            label = "catches_screen_transition",
            transitionSpec = {
                AnimationSpecs.contentTransitionSpec
            }
        ) { state ->
            when (state) {
                is CatchesScreenState.Grid -> {
                    CatchGridContent(
                        onCatchClick = stateManager::navigateToDetails,
                        navigate = navigate,
                        animatedVisibilityScope = this@AnimatedContent
                    )
                }
                is CatchesScreenState.Details -> {
                    CatchDetailsContent(
                        catchId = state.catchId,
                        animationKey = stateManager.animationKey,
                        onBackClick = stateManager::navigateToGrid,
                        navigate = navigate,
                        animatedVisibilityScope = this@AnimatedContent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SharedTransitionScope.CatchGridContent(
    onCatchClick: (String) -> Unit,
    navigate: (Screens) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: CatchGridViewModel = koinViewModel(),
    toastManager: ToastManager = koinInject()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.sendIntent(CatchGridIntent.LoadCatches)
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CatchGridEffect.NavigateToCatchDetails -> {
                    onCatchClick(effect.catchId)
                }
                is CatchGridEffect.ShowError -> {
                    toastManager.showError(effect.message)
                }
                is CatchGridEffect.ShowSuccess -> {
                    toastManager.showSuccess(effect.message)
                }
            }
        }
    }
    
    if (state.showDeleteDialog && state.catchToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.sendIntent(CatchGridIntent.HideDeleteDialog) },
            title = { Text("Delete Catch") },
            text = { Text("Are you sure you want to delete this catch? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.sendIntent(CatchGridIntent.DeleteCatch(state.catchToDelete))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.sendIntent(CatchGridIntent.HideDeleteDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .background(HookedTheme.background)
            .fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = true,
            enter = AnimationSpecs.slideInFromTop,
            exit = AnimationSpecs.slideOutToTop
        ) {
            TopAppBar(
                title = { Text("My Catches") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HookedTheme.primary,
                    titleContentColor = HookedTheme.onPrimary
                )
            )
        }
        
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Setup pull refresh state
            val pullRefreshState = rememberPullRefreshState(
                refreshing = state.isRefreshing,
                onRefresh = { viewModel.sendIntent(CatchGridIntent.LoadCatches) }
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                when {
                    state.isLoading -> {
                        LoadingSkeletonGrid()
                    }
                    state.catches.isEmpty() -> {
                        EmptyStateView(
                            onAddCatchClick = { navigate(Screens.SubmitCatch) }
                        )
                    }
                    else -> {
                        AnimatedVisibility(
                            visible = true,
                            enter = androidx.compose.animation.fadeIn(
                                animationSpec = tween(300)
                            ),
                            exit = androidx.compose.animation.fadeOut(
                                animationSpec = tween(300)
                            )
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(AnimationConstants.GRID_SPACING_DP.dp),
                                verticalArrangement = Arrangement.spacedBy(AnimationConstants.GRID_SPACING_DP.dp),
                                horizontalArrangement = Arrangement.spacedBy(AnimationConstants.GRID_SPACING_DP.dp)
                            ) {
                                items(
                                    items = state.catches,
                                    key = { catch -> catch.id }
                                ) { catch ->
                                    CatchGridItem(
                                        catch = catch,
                                        onClick = {
                                            viewModel.sendIntent(CatchGridIntent.NavigateToCatchDetails(catch.id))
                                        },
                                        onLongClick = {
                                            viewModel.sendIntent(CatchGridIntent.ShowDeleteDialog(catch.id))
                                        },
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Pull refresh indicator with enhanced styling
                if (!state.isLoading) {
                    PullRefreshIndicator(
                        refreshing = state.isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        backgroundColor = HookedTheme.primary,
                        contentColor = HookedTheme.onPrimary,
                        scale = true
                    )
                }
            }
            
            FloatingActionButton(
                onClick = { navigate(Screens.SubmitCatch) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(AnimationConstants.FAB_PADDING_DP.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new catch"
                )
            }
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.CatchDetailsContent(
    catchId: String,
    animationKey: Int,
    onBackClick: () -> Unit,
    navigate: (Screens) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: CatchDetailsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDetails by remember(animationKey) { mutableStateOf(false) }
    var showAppBar by remember(animationKey) { mutableStateOf(false) }
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    
    // Animation state for all cards
    val cardsTranslation by animateFloatAsState(
        targetValue = if (showDetails) 0f else AnimationConstants.CARD_TRANSLATION_OFFSET,
        animationSpec = AnimationSpecs.detailsSpringSpec,
        label = "cards_translation"
    )

    // Handle back button press
    BackHandler {
        showDetails = false
        showAppBar = false
        onBackClick()
    }
    
    LaunchedEffect(animationKey) {
        // Reset animation state first
        showDetails = false
        showAppBar = false
        viewModel.sendIntent(CatchDetailsIntent.LoadCatchDetails(catchId))
        delay(AnimationConstants.DETAILS_ANIMATION_DELAY_MS) // Small delay to let the image transition start
        showDetails = true
        showAppBar = true
    }
    
    Box(
        modifier = Modifier
            .background(HookedTheme.background)
            .fillMaxSize()
    ) {
        // Content goes first (behind the app bar)
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = HookedTheme.primary,
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "Loading catch details...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            state.catchDetails?.let { details ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(AnimationConstants.CONTENT_PADDING_DP.dp)
                        .animateContentSize(
                            animationSpec = AnimationSpecs.contentSizeSpring
                        ),
                    verticalArrangement = Arrangement.spacedBy(AnimationConstants.CONTENT_PADDING_DP.dp)
                ) {
                    // Photo Section with shared element
                    Card(
                        modifier = Modifier
                            //.padding(top = AnimationConstants.DETAIL_CARD_TOP_PADDING_DP.dp)
                            .padding(top = topBarHeight)
                            .fillMaxWidth()
                            .aspectRatio(1f), // Keep same aspect ratio as grid
                        elevation = CardDefaults.cardElevation(defaultElevation = AnimationConstants.CARD_ELEVATION_DP.dp),
                        shape = RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp)
                    ) {
                        AsyncImage(
                            imageUrl = details.photoUrl,
                            modifier = Modifier
                                .fillMaxSize()
                                .sharedElement(
                                    rememberSharedContentState(key = "catch-image-${catchId}"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ ->
                                        AnimationSpecs.boundsTransformSpring
                                    },
                                    renderInOverlayDuringTransition = true
                                )
                                .border(
                                    width = AnimationConstants.IMAGE_BORDER_WIDTH_DP.dp,
                                    color = HookedTheme.tertiary,
                                    shape = RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp)
                                )
                                .clip(RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp))
                        )
                    }
                    
                    // Metadata and Map Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = cardsTranslation
                            },
                        horizontalArrangement = Arrangement.spacedBy(AnimationConstants.CONTENT_PADDING_DP.dp)
                    ) {
                        // Left column - Metadata
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(AnimationConstants.CONTENT_PADDING_DP.dp)
                        ) {
                            // Species Section
                            details.species?.let { species ->
                                AnimatedDetailCard(
                                    label = "Species",
                                    value = species,
                                    translationY = 0f // Already animated by parent Row
                                )
                            }

                            // Weight Section
                            AnimatedDetailCard(
                                label = "Weight",
                                value = "${details.weight} kg",
                                translationY = 0f // Already animated by parent Row
                            )
                            
                            // Length Section
                            AnimatedDetailCard(
                                label = "Length",
                                value = "${details.length} cm",
                                translationY = 0f // Already animated by parent Row
                            )
                        }
                        
                        // Right column - Map
                        details.latitude?.let { latitude ->
                            details.longitude?.let { longitude ->
                                StaticMapCard(
                                    latitude = latitude,
                                    longitude = longitude,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
        }
    }
}

@Composable
private fun LoadingSkeletonGrid(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .padding(AnimationConstants.GRID_SPACING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(AnimationConstants.GRID_SPACING_DP.dp),
        horizontalArrangement = Arrangement.spacedBy(AnimationConstants.GRID_SPACING_DP.dp)
    ) {
        items(6) { index ->
            SkeletonCatchItem(shimmerAlpha)
        }
    }
}

@Composable
private fun SkeletonCatchItem(
    shimmerAlpha: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = AnimationConstants.CARD_ELEVATION_DP.dp
        ),
        colors = CardDefaults.cardColors(containerColor = HookedTheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    HookedTheme.primary.copy(alpha = shimmerAlpha),
                    RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .background(
                            HookedTheme.surface.copy(alpha = 0.8f),
                            RoundedCornerShape(4.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .background(
                            HookedTheme.surface.copy(alpha = 0.6f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    onAddCatchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    HookedTheme.primary.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .rotate(rotation),
                tint = HookedTheme.primary.copy(alpha = 0.6f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "No Catches Yet",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Start building your fishing collection!\nCapture your first catch to get started.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onAddCatchClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = HookedTheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = "Add Your First Catch",
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(
                containerColor = HookedTheme.primary.copy(alpha = 0.05f)
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = HookedTheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Tap the + button to add a catch anytime",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

                }
            }
        }
        
        // App bar overlays the content
        AnimatedVisibility(
            visible = showAppBar,
            enter = AnimationSpecs.appBarSlideIn,
            exit = AnimationSpecs.slideOutToTop
        ) {
            TopAppBar(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    topBarHeight = with(density) { coordinates.size.height.toDp() }
                },
                title = { Text("Catch Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        showDetails = false // Reset animation state
                        showAppBar = false
                        onBackClick()
                    }) {
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
        }
    }
}
