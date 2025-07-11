package com.hooked.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.kamel.core.Resource
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import com.hooked.theme.HookedTheme

@Composable
fun AsyncImage(
    imageUrl: String,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape? = null
) {
    val getPainterResource: @Composable (BoxWithConstraintsScope.() -> Resource<Painter>) = {
        asyncPainterResource(
            imageUrl,
            filterQuality = FilterQuality.High
        )
    }
    KamelImage(
        resource = getPainterResource,
        contentDescription = contentDescription,
        modifier = if (shape != null) {
            modifier
                .background(HookedTheme.surface, shape)
                .clip(shape)
        } else {
            modifier
                .background(HookedTheme.surface)
        },
        contentScale = contentScale,
        onLoading = { CircularProgressIndicator(it) },
        onFailure = { exception: Throwable ->
            exception.printStackTrace()
        },
    )
}