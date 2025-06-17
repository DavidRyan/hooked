package com.hooked.catches.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel

sealed class CatchesScreenState {
    object Grid : CatchesScreenState()
    data class Details(val catchId: Long) : CatchesScreenState()
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CatchesScreen(
    navigate: (Screens) -> Unit,
    modifier: Modifier = Modifier
) {
    var screenState by remember { mutableStateOf<CatchesScreenState>(CatchesScreenState.Grid) }
    var detailsAnimationKey by remember { mutableStateOf(0) }
    
    SharedTransitionLayout(
        modifier = modifier
            .fillMaxSize()
            .background(HookedTheme.background)
    ) {
        AnimatedContent(
            targetState = screenState,
            label = "catches_screen_transition",
            transitionSpec = {
                AnimationSpecs.contentTransitionSpec
            }
        ) { state ->
            when (state) {
                is CatchesScreenState.Grid -> {
                    CatchGridContent(
                        onCatchClick = { catchId ->
                            detailsAnimationKey += 1
                            screenState = CatchesScreenState.Details(catchId)
                        },
                        navigate = navigate,
                        animatedVisibilityScope = this@AnimatedContent,
                        sharedTransitionScope = this@SharedTransitionLayout
                    )
                }
                is CatchesScreenState.Details -> {
                    CatchDetailsContent(
                        catchId = state.catchId,
                        animationKey = detailsAnimationKey,
                        onBackClick = {
                            screenState = CatchesScreenState.Grid
                        },
                        navigate = navigate,
                        animatedVisibilityScope = this@AnimatedContent,
                        sharedTransitionScope = this@SharedTransitionLayout
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.CatchGridContent(
    onCatchClick: (Long) -> Unit,
    navigate: (Screens) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
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
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = state.catches,
                    key = { catch -> catch.id } // Stable key for each item
                ) { catch ->
                    CatchGridItemWithSharedElement(
                        catch = catch,
                        onClick = {
                            viewModel.sendIntent(CatchGridIntent.NavigateToCatchDetails(it))
                        },
                        animatedVisibilityScope = animatedVisibilityScope,
                        sharedTransitionScope = sharedTransitionScope
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
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.CatchGridItemWithSharedElement(
    catch: CatchModel,
    onClick: (id: Long) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = { onClick(catch.id) }),
        shape = RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = AnimationConstants.CARD_ELEVATION_DP.dp),
        colors = CardDefaults.cardColors(containerColor = HookedTheme.surface)
    ) {
        AsyncImage(
            imageUrl = catch.imageUrl,
            modifier = Modifier
                .fillMaxSize()
                .sharedElement(
                    rememberSharedContentState(key = "catch-image-${catch.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = { _, _ ->
                        AnimationSpecs.boundsTransformSpring
                    },
                    renderInOverlayDuringTransition = true
                )
                .clip(RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp))
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.CatchDetailsContent(
    catchId: Long,
    animationKey: Int,
    onBackClick: () -> Unit,
    navigate: (Screens) -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedTransitionScope: SharedTransitionScope,
    viewModel: CatchDetailsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDetails by remember(animationKey) { mutableStateOf(false) }
    var showAppBar by remember(animationKey) { mutableStateOf(false) }
    
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
    
    Column(
        modifier = Modifier
            .background(HookedTheme.background)
            .fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = showAppBar,
            enter = AnimationSpecs.appBarSlideIn,
            exit = AnimationSpecs.slideOutToTop
        ) {
            TopAppBar(
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
        
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
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
                        .padding(16.dp)
                        .animateContentSize(
                            animationSpec = AnimationSpecs.contentSizeSpring
                        ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Photo Section with shared element
                    Card(
                        modifier = Modifier
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
                                .clip(RoundedCornerShape(AnimationConstants.CORNER_RADIUS_DP.dp))
                        )
                    }
                    
                    // Species Section
                    DetailCard(
                        label = "Species",
                        value = details.species,
                        modifier = Modifier.graphicsLayer {
                            translationY = cardsTranslation
                        }
                    )
                    
                    // Weight Section
                    DetailCard(
                        label = "Weight",
                        value = "${details.weight} kg",
                        modifier = Modifier.graphicsLayer {
                            translationY = cardsTranslation
                        }
                    )
                    
                    // Length Section
                    DetailCard(
                        label = "Length",
                        value = "${details.length} cm",
                        modifier = Modifier.graphicsLayer {
                            translationY = cardsTranslation
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = AnimationConstants.CARD_ELEVATION_DP.dp)
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
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}