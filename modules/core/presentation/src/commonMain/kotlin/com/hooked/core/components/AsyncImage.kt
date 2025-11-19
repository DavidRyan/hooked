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
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.hooked.theme.HookedTheme
import com.hooked.core.logging.Logger
import androidx.compose.foundation.layout.size

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

    val context = LocalPlatformContext.current
    
    val imageRequest = ImageRequest.Builder(context)
        .data(imageUrl)
        .build()

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = if (shape != null) {
            modifier
                .clip(shape)
        } else {
            modifier
        },
        loading = {
            Box(
                modifier = Modifier.fillMaxSize().background(HookedTheme.surface),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = HookedTheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        },
        error = {
            Logger.info("AsyncImage", "Failed to load image: ${it.result.throwable.message}")
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Failed to load image",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}