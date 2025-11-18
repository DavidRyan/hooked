package com.hooked.catches.presentation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hooked.core.nav.Screens
import com.hooked.core.presentation.toast.ToastManager
import kotlinx.coroutines.flow.collectLatest
import com.hooked.catches.presentation.model.SubmitCatchEffect
import org.koin.compose.koinInject
import com.hooked.catches.presentation.model.SubmitCatchIntent
import com.hooked.theme.HookedTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SubmitCatchScreen(
    modifier: Modifier = Modifier,
    viewModel: SubmitCatchViewModel,
    navigate: (Screens) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    toastManager: ToastManager = koinInject()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SubmitCatchEffect.NavigateBack -> {
                    navigate(Screens.CatchGrid)
                }
                is SubmitCatchEffect.ShowError -> {
                    toastManager.showError(effect.message)
                }
                is SubmitCatchEffect.CatchSubmittedSuccessfully -> {
                    toastManager.showSuccess("Catch submitted successfully!")
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
                onPhotoSelected = { uri -> viewModel.sendIntent(SubmitCatchIntent.UpdatePhoto(uri)) },
                onRemovePhoto = { viewModel.sendIntent(SubmitCatchIntent.RemovePhoto) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                catchId = state.submittedCatchId
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
fun ProgressIndicator(
    step: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        if (step > 3) {
            var slideIn by remember { mutableStateOf(false) }
            var showText by remember { mutableStateOf(false) }
            var animationComplete by remember { mutableStateOf(false) }
            var drawCircle by remember { mutableStateOf(false) }
            var showCheckmark by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                slideIn = true
            }
            
            val density = androidx.compose.ui.platform.LocalDensity.current
            val screenWidthPx = with(density) { 500.dp.toPx() }
            
            val slideOffset by animateFloatAsState(
                targetValue = if (slideIn) 0f else -screenWidthPx,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                ),
                finishedListener = {
                    if (slideIn) {
                        showText = true
                        drawCircle = true
                    }
                },
                label = "slideOffset"
            )
            
            val textAlpha by animateFloatAsState(
                targetValue = if (showText) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = 50
                ),
                finishedListener = {
                    if (showText) {
                        animationComplete = true
                    }
                },
                label = "textAlpha"
            )
            
            val circleProgress by animateFloatAsState(
                targetValue = if (drawCircle) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                ),
                finishedListener = {
                    if (drawCircle) {
                        showCheckmark = true
                    }
                },
                label = "circleProgress"
            )
            
            val checkmarkAlpha by animateFloatAsState(
                targetValue = if (showCheckmark) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 200
                ),
                label = "checkmarkAlpha"
            )
            
            val backgroundColor by animateColorAsState(
                targetValue = if (animationComplete) Color(0xFFFFD700) else HookedTheme.tertiary,
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearEasing
                ),
                label = "backgroundColor"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .graphicsLayer {
                        translationX = slideOffset
                    }
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Submitted",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer {
                            alpha = textAlpha
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Canvas(
                        modifier = Modifier.size(48.dp)
                    ) {
                        val strokeWidth = 4.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)
                        
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = 360f * circleProgress,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        
                        if (showCheckmark) {
                            val checkPath = Path().apply {
                                val checkSize = radius * 1.2f
                                moveTo(center.x - checkSize * 0.3f, center.y)
                                lineTo(center.x - checkSize * 0.1f, center.y + checkSize * 0.3f)
                                lineTo(center.x + checkSize * 0.4f, center.y - checkSize * 0.3f)
                            }
                            
                            drawPath(
                                path = checkPath,
                                color = Color.White.copy(alpha = checkmarkAlpha),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                Bobber(shouldSink = step > 0)
                Bobber(shouldSink = step > 1)
                Bobber(shouldSink = step > 2)
            }
        }
    }
}

@Composable
fun Bobber(
    shouldSink: Boolean = false,
    onSinkComplete: () -> Unit = {}
) {
    val horizontalOffset = remember { (-10..10).random().toFloat() }
    val rotationRange = remember { 3f + kotlin.random.Random.nextFloat() * 4f }
    val bobbingSpeed = remember { 500 + kotlin.random.Random.nextInt(300) }
    
    val infiniteTransition = rememberInfiniteTransition()
    
    val bobbingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = bobbingSpeed,
                easing = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val sinkOffset by animateFloatAsState(
        targetValue = if (shouldSink) 1000f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutLinearInEasing
        ),
        finishedListener = {
            if (shouldSink) {
                onSinkComplete()
            }
        }
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -rotationRange,
        targetValue = rotationRange,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 400,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Canvas(
        modifier = Modifier
            .size(100.dp)
            .graphicsLayer {
                translationX = if (!shouldSink) horizontalOffset else 0f
                translationY = if (shouldSink) sinkOffset else bobbingOffset
                rotationZ = if (!shouldSink) rotation else 0f
            }
    ) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)

        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )
        drawArc(
            color = Color.Red,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )
        
        val tipSize = radius * 0.3f
        drawRect(
            color = Color.White,
            topLeft = Offset(center.x - tipSize / 2, center.y - radius - tipSize),
            size = Size(tipSize, tipSize)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PhotoSection(
    photoUri: String?,
    onPhotoSelected: (String) -> Unit,
    onRemovePhoto: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    catchId: String?
) {
    val photoPicker = getPhotoPicker()
    val launchPhotoPicker = photoPicker.rememberPhotoPickerLauncher(onPhotoSelected)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        PhotoSectionContent(
            photoUri = photoUri,
            onPhotoSelected = onPhotoSelected,
            onRemovePhoto = onRemovePhoto,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            catchId = catchId,
            launchPhotoPicker = launchPhotoPicker
        )
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