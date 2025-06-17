package com.hooked.core.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

object AnimationConstants {
    // Durations
    const val TRANSITION_DURATION_MS = 300
    const val APP_BAR_ANIMATION_MS = 400
    const val DETAILS_ANIMATION_DELAY_MS = 100L
    
    // Spring configurations
    const val SPRING_DAMPING_RATIO = 0.8f
    const val SPRING_STIFFNESS = 380f
    const val DETAILS_SPRING_STIFFNESS = 300f
    const val VISIBILITY_THRESHOLD = 0.1f
    
    // Scale values
    const val SCALE_INITIAL = 0.92f
    const val SCALE_TARGET = 1.08f
    
    // Translation values
    const val CARD_TRANSLATION_OFFSET = 1200f
    
    // Shape values
    const val CORNER_RADIUS_DP = 20
    const val CARD_ELEVATION_DP = 4
}

object AnimationSpecs {
    val boundsTransformSpring = spring<Rect>(
        dampingRatio = AnimationConstants.SPRING_DAMPING_RATIO,
        stiffness = AnimationConstants.SPRING_STIFFNESS
    )
    
    val contentSizeSpring = spring<IntSize>(
        dampingRatio = AnimationConstants.SPRING_DAMPING_RATIO,
        stiffness = AnimationConstants.SPRING_STIFFNESS
    )
    
    val detailsSpringSpec = spring<Float>(
        dampingRatio = AnimationConstants.SPRING_DAMPING_RATIO,
        stiffness = AnimationConstants.DETAILS_SPRING_STIFFNESS,
        visibilityThreshold = AnimationConstants.VISIBILITY_THRESHOLD
    )
    
    val contentTransitionSpec = fadeIn(animationSpec = tween(AnimationConstants.TRANSITION_DURATION_MS)) + 
        scaleIn(
            initialScale = AnimationConstants.SCALE_INITIAL,
            animationSpec = tween(AnimationConstants.TRANSITION_DURATION_MS)
        ) togetherWith fadeOut(animationSpec = tween(AnimationConstants.TRANSITION_DURATION_MS)) + 
        scaleOut(
            targetScale = AnimationConstants.SCALE_TARGET,
            animationSpec = tween(AnimationConstants.TRANSITION_DURATION_MS)
        )
    
    val slideInFromTop = slideInVertically(
        initialOffsetY = { -it },
        animationSpec = tween(AnimationConstants.TRANSITION_DURATION_MS)
    )
    
    val slideOutToTop = slideOutVertically(
        targetOffsetY = { -it },
        animationSpec = tween(AnimationConstants.TRANSITION_DURATION_MS)
    )
    
    val appBarSlideIn = slideInVertically(
        initialOffsetY = { -it },
        animationSpec = tween(AnimationConstants.APP_BAR_ANIMATION_MS)
    )
}