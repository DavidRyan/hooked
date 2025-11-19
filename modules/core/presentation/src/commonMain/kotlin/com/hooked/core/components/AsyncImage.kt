package com.hooked.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage as CoilAsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.hooked.theme.HookedTheme
import com.hooked.core.logging.Logger
import androidx.compose.foundation.layout.size
import coil3.request.crossfade

/**
 * Cross-platform AsyncImage component using Coil3 for Compose Multiplatform.
 * Handles both remote URLs and local URIs (file://, content://).
 * Uses CoilAsyncImage for better shared element transition support.
 * 
 * Note: For shared element transitions to work properly, modifiers are applied directly 
 * to the image rather than wrapping it in additional containers. This creates a simpler
 * component hierarchy which is essential for transitions to work correctly.
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun AsyncImage(
    imageUrl: String?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape? = RoundedCornerShape(8.dp)
) {
    if (imageUrl == null) {
        return
    }

    val sourceType = when {
        imageUrl.startsWith("http://") -> "HTTP URL"
        imageUrl.startsWith("https://") -> "HTTPS URL"
        imageUrl.startsWith("file://") -> "File URI"
        imageUrl.startsWith("content://") -> "Content URI"
        else -> "Unknown URI type"
    }
    
    Logger.debug("AsyncImage", "Loading image from $sourceType: $imageUrl")
    
    val context = LocalPlatformContext.current
    
    // Optimize image request for shared element transitions
    val imageRequest = ImageRequest.Builder(context)
        .data(imageUrl)
        .crossfade(false)
        .placeholder(null) // No placeholder for shared element transitions
        .size(coil3.size.Size.ORIGINAL) // Use original size to avoid resizing
        .build()

    // Using CoilAsyncImage for better shared element transition support
    // The key is to avoid any animation or placeholder changes during transitions
    CoilAsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        contentScale = contentScale,
        // Apply modifiers directly to the image without wrapping in a Box
        // This is crucial for shared element transitions to work
        modifier = if (shape != null) {
            modifier.clip(shape)
        } else {
            modifier
        }
    )
}