package com.hooked.catches.presentation


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import com.hooked.catches.domain.entities.EnrichmentStatus
import com.hooked.catches.presentation.components.SpeedDialFab
import com.hooked.catches.presentation.components.SpeedDialItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api

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
import com.hooked.catches.presentation.model.WeatherUi
import com.hooked.core.components.AsyncImage
import com.hooked.core.nav.Screens
import com.hooked.theme.HookedTheme
import com.hooked.core.util.BackHandler
import com.hooked.core.animation.AnimationConstants
import com.hooked.core.animation.AnimationSpecs
import com.hooked.catches.presentation.components.CatchGridItem
import com.hooked.catches.presentation.components.TimelineSection
import com.hooked.catches.presentation.components.CatchHero
import com.hooked.catches.presentation.components.StatStrip
import com.hooked.catches.presentation.components.MapCard
import com.hooked.catches.presentation.components.ConditionsNarrative
import com.hooked.catches.presentation.components.IntelligenceRibbon
import com.hooked.catches.presentation.components.AnimatedDetailCard
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.hooked.catches.presentation.components.MapColumn
import com.hooked.catches.presentation.components.SunSection
import com.hooked.catches.presentation.components.WeatherSection
import com.hooked.catches.presentation.state.rememberCatchesScreenState
import com.hooked.core.presentation.toast.ToastManager
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

sealed class CatchesScreenState {
    object Grid : CatchesScreenState()
    data class Details(
        val catchId: String,
        // Carried from the grid so the detail screen has something to render
        // immediately, before the per-catch ViewModel finishes loading.
        val seedSpecies: String? = null,
        val seedLocation: String? = null,
        val seedImageUrl: String? = null,
        val seedDateCaught: String? = null,
        val seedEnrichmentStatus: EnrichmentStatus? = null
    ) : CatchesScreenState()
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
            // Instant swap — no enter, no exit, no size morph. Default SizeTransform
            // animates the container between grid and detail sizes; we want neither.
            transitionSpec = {
                (androidx.compose.animation.EnterTransition.None togetherWith
                    androidx.compose.animation.ExitTransition.None)
                    .using(
                        androidx.compose.animation.SizeTransform(clip = false) { _, _ ->
                            tween(0)
                        }
                    )
            },
            label = "catches_screen_transition"
        ) { state ->
            when (state) {
                is CatchesScreenState.Grid -> {
                    CatchGridContent(
                        onCatchClick = { model -> stateManager.navigateToDetails(model) },
                        navigate = navigate,
                        animatedVisibilityScope = this@AnimatedContent
                    )
                }
                is CatchesScreenState.Details -> {
                    CatchDetailsContent(
                        catchId = state.catchId,
                        seedSpecies = state.seedSpecies,
                        seedLocation = state.seedLocation,
                        seedImageUrl = state.seedImageUrl,
                        seedDateCaught = state.seedDateCaught,
                        seedEnrichmentStatus = state.seedEnrichmentStatus,
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
    onCatchClick: (CatchModel) -> Unit,
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
                    // Resolve the model from current state so the detail screen
                    // gets seed values (species, location, image) immediately.
                    val model = state.catches.firstOrNull { it.id == effect.catchId }
                    com.hooked.core.logging.Logger.info(
                        "CatchGridNav",
                        "nav effect catchId=${effect.catchId} model=${model?.let { "name=${it.name},loc=${it.location}" } ?: "NULL"}"
                    )
                    if (model != null) onCatchClick(model)
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
                        state.catchToDelete?.let { catchId ->
                            viewModel.sendIntent(CatchGridIntent.DeleteCatch(catchId))
                        }
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
                title = {
                    Text(
                        "Log",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = HookedTheme.onSurface
                )
            )
        }

        IntelligenceRibbon(
            onTap = { headline ->
                val starter = headline?.takeIf { it.isNotBlank() }?.let { "Tell me more: $it" }
                navigate(Screens.Chat(starter))
            }
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val pullRefreshState = rememberPullRefreshState(
                refreshing = state.isRefreshing,
                onRefresh = { viewModel.sendIntent(CatchGridIntent.LoadCatches) }
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState)
            ) {
                AnimatedContent(
                    targetState = when {
                        state.isLoading -> "loading"
                        state.catches.isEmpty() -> "empty"
                        else -> "content"
                    },
                    transitionSpec = {
                        androidx.compose.animation.fadeIn(
                            animationSpec = tween(300)
                        ) togetherWith androidx.compose.animation.fadeOut(
                            animationSpec = tween(300)
                        )
                    }
                ) { targetState ->
                    when (targetState) {
                        "loading" -> {
                            LoadingSkeletonGrid()
                        }
                        "empty" -> {
                            EmptyStateView(
                                onAddCatchClick = { navigate(Screens.SubmitCatch) }
                            )
                        }
                        "content" -> {
                            TimelineSection(
                                catches = state.catches,
                                onCatchClick = { id ->
                                    viewModel.sendIntent(CatchGridIntent.NavigateToCatchDetails(id))
                                },
                                onCatchLongClick = { id ->
                                    viewModel.sendIntent(CatchGridIntent.ShowDeleteDialog(id))
                                },
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                }
                
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
            
            SpeedDialFab(
                modifier = Modifier.fillMaxSize(),
                items = listOf(
                    SpeedDialItem(
                        icon = Icons.Default.Clear,
                        label = "Log Skunk",
                        onClick = { navigate(Screens.SubmitSkunk) }
                    ),
                    SpeedDialItem(
                        icon = Icons.Default.Add,
                        label = "Log Catch",
                        onClick = { navigate(Screens.SubmitCatch) },
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            )
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
    seedSpecies: String? = null,
    seedLocation: String? = null,
    seedImageUrl: String? = null,
    seedDateCaught: String? = null,
    seedEnrichmentStatus: EnrichmentStatus? = null,
    viewModel: CatchDetailsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDetails by remember(animationKey) { mutableStateOf(false) }
    var showAppBar by remember(animationKey) { mutableStateOf(false) }
    var topBarHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    
    val cardsTranslation by animateFloatAsState(
        targetValue = if (showDetails) 0f else AnimationConstants.CARD_TRANSLATION_OFFSET,
        animationSpec = AnimationSpecs.detailsSpringSpec,
        label = "cards_translation"
    )

    BackHandler {
        showDetails = false
        showAppBar = false
        onBackClick()
    }
    
    LaunchedEffect(animationKey) {
        // Show the details + app bar immediately — no artificial delay. The
        // EnrichmentBanner still slide-animates on its own, but the rest of
        // the content paints at frame 1.
        showDetails = true
        showAppBar = true
        viewModel.sendIntent(CatchDetailsIntent.LoadCatchDetails(catchId))
    }
    
    // Always render the Column. Use ViewModel data when available, otherwise fall
    // back to seed values carried from the grid tile so species/location/image
    // show immediately on tap rather than after the network load.
    val details = state.catchDetails
    val effectivePhotoUrl = details?.photoUrl ?: seedImageUrl
    val effectiveSpecies = details?.species ?: seedSpecies
    val effectiveLocation = details?.location ?: seedLocation
    val effectiveEnrichmentStatus = details?.enrichmentStatus
        ?: seedEnrichmentStatus
        ?: EnrichmentStatus.Pending

    Box(
        modifier = Modifier
            .background(HookedTheme.background)
            .fillMaxSize()
    ) {
        run {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    CatchHero(
                        catchId = catchId,
                        photoUrl = effectivePhotoUrl,
                        species = effectiveSpecies,
                        location = effectiveLocation,
                        weatherDescription = details?.weatherData?.get("description"),
                        tempFahrenheit = details?.weatherData
                            ?.get("temp")
                            ?.toFloatOrNull(),
                        animatedVisibilityScope = animatedVisibilityScope
                    )

                    // Render the banner directly — no AnimatedVisibility wrapper.
                    // The previous slide-in caused the Column below to reflow as the
                    // banner expanded its space, which read as content "popping in"
                    // at exactly the moment the banner settled. The banner still has
                    // its own shimmer + icon-scale animations internally.
                    EnrichmentBanner(status = effectiveEnrichmentStatus)

                    details?.timestamp
                        ?.takeIf { it > 0 }
                        ?.let { ts ->
                            Text(
                                text = formatCatchDate(ts),
                                style = MaterialTheme.typography.labelLarge,
                                color = HookedTheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }

                    // Render directly — both components no-op when weatherData is null,
                    // and appear instantly when it arrives. No fade wrappers.
                    val weatherData = details?.weatherData
                    StatStrip(weatherData = weatherData)

                    MapCard(
                        latitude = details?.latitude,
                        longitude = details?.longitude,
                        location = effectiveLocation
                    )

                    ConditionsNarrative(weatherData = weatherData)

                    SunSection(
                        sunriseHour = 6.5f,
                        sunsetHour = 19.5f,
                        catchHour = catchHourFrom(details?.timestamp),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(Modifier.size(24.dp))
                }
        }
        
        AnimatedVisibility(
            visible = showAppBar,
            enter = AnimationSpecs.appBarSlideIn,
            exit = AnimationSpecs.slideOutToTop
        ) {
            TopAppBar(
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    topBarHeight = with(density) { coordinates.size.height.toDp() }
                },
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        showDetails = false
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
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun EnrichmentBanner(
    status: EnrichmentStatus,
    modifier: Modifier = Modifier
) {
    // Infinite pulse for Pending border/spinner
    val infiniteTransition = rememberInfiniteTransition(label = "enrichment_banner")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // One-shot shimmer sweep on Completed
    val shimmerProgress = remember { Animatable(0f) }
    LaunchedEffect(status) {
        if (status == EnrichmentStatus.Completed) {
            shimmerProgress.snapTo(0f)
            shimmerProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = LinearEasing)
            )
        }
    }
    val shimmerValue = shimmerProgress.value

    // Spring-punch scale for icon on Completed/Failed
    val iconScale = remember { Animatable(0f) }
    LaunchedEffect(status) {
        if (status != EnrichmentStatus.Pending) {
            iconScale.snapTo(0f)
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val statusColor = when (status) {
        EnrichmentStatus.Completed -> HookedTheme.primary
        EnrichmentStatus.Failed    -> MaterialTheme.colorScheme.error
        EnrichmentStatus.Pending   -> HookedTheme.primary
    }

    val borderAlpha = if (status == EnrichmentStatus.Pending) pulseAlpha * 0.6f else 0.75f
    val glowAlpha   = if (status == EnrichmentStatus.Pending) pulseAlpha else 1f

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                // Glow layers drawn in unclipped space (before clip)
                .drawBehind {
                    val r = size.height / 2f
                    listOf(10f to 0.06f, 5f to 0.12f, 2f to 0.22f).forEach { (expand, alpha) ->
                        drawRoundRect(
                            color = statusColor,
                            topLeft = Offset(-expand, -expand),
                            size = Size(size.width + expand * 2, size.height + expand * 2),
                            cornerRadius = CornerRadius((r + expand)),
                            alpha = alpha * glowAlpha
                        )
                    }
                }
                .clip(RoundedCornerShape(50))
                .background(statusColor.copy(alpha = 0.13f))
                // Border + shimmer drawn inside the clipped pill
                .drawBehind {
                    val r = CornerRadius(size.height / 2f)
                    // Crisp border
                    drawRoundRect(
                        color = statusColor,
                        cornerRadius = r,
                        style = Stroke(width = 1.2.dp.toPx()),
                        alpha = borderAlpha
                    )
                    // Shimmer sweep
                    if (shimmerValue > 0f) {
                        val sweepWidth = size.width * 0.5f
                        val centerX = shimmerValue * (size.width + sweepWidth) - sweepWidth / 2f
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.28f),
                                    Color.Transparent
                                ),
                                startX = centerX - sweepWidth / 2f,
                                endX   = centerX + sweepWidth / 2f
                            ),
                            cornerRadius = r
                        )
                    }
                }
                .padding(horizontal = 22.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (status) {
                EnrichmentStatus.Pending -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = statusColor.copy(alpha = pulseAlpha)
                )
                EnrichmentStatus.Completed -> Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value }
                )
                EnrichmentStatus.Failed -> Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value }
                )
            }
            Text(
                text = when (status) {
                    EnrichmentStatus.Completed -> "AI Enriched"
                    EnrichmentStatus.Failed    -> "Enrichment Failed"
                    EnrichmentStatus.Pending   -> "Enriching…"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

@Composable
fun LoadingSkeletonGrid(
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
fun SkeletonCatchItem(
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
fun EmptyStateView(
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
                imageVector = Icons.Filled.Add,
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
                imageVector = Icons.Filled.Add,
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
                    imageVector = Icons.Default.Add,
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

@OptIn(kotlin.time.ExperimentalTime::class)
private fun catchHourFrom(timestamp: Long?): Float {
    if (timestamp == null || timestamp <= 0) return 12f
    val ldt = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return ldt.hour + ldt.minute / 60f
}
