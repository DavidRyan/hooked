package com.hooked.catches.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

sealed class CatchesScreenState {
    object Grid : CatchesScreenState()
    data class Details(val catchId: String) : CatchesScreenState()
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CatchesScreen(
    navigate: (Screens) -> Unit,
    modifier: Modifier = Modifier
) {
    val stateManager = rememberCatchesScreenState()
    
    SharedTransitionLayout(
        modifier = modifier
            .fillMaxSize()
            .background(HookedTheme.background)
    ) {
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

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.CatchGridContent(
    onCatchClick: (String) -> Unit,
    navigate: (Screens) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: CatchGridViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.sendIntent(CatchGridIntent.LoadCatches)
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CatchGridEffect.NavigateToCatchDetails -> {
                    onCatchClick(effect.catchId)
                }
                else -> {}
            }
        }
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
                    key = { catch -> catch.id } // Stable key for each item
                ) { catch ->
                    CatchGridItem(
                        catch = catch,
                        onClick = {
                            viewModel.sendIntent(CatchGridIntent.NavigateToCatchDetails(catch.id))
                        },
                        animatedVisibilityScope = animatedVisibilityScope
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AnimationConstants.CONTENT_PADDING_DP.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
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
                            AnimatedDetailCard(
                                label = "Species",
                                value = details.species,
                                translationY = 0f // Already animated by parent Row
                            )
                            
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
                        StaticMapCard(
                            latitude = details.latitude,
                            longitude = details.longitude,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
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
