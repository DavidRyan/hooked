package com.hooked.catches.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.hooked.core.animation.AnimationConstants
import kotlinx.coroutines.delay

@Composable
fun AnimatedDetailCard(
    label: String,
    value: String,
    index: Int = 0,
    modifier: Modifier = Modifier
) {
    var revealed by remember { mutableStateOf(false) }

    LaunchedEffect(index, value) {
        revealed = false
        delay(index * 60L)
        revealed = true
    }

    val translation by animateFloatAsState(
        targetValue = if (revealed) 0f else AnimationConstants.CARD_TRANSLATION_OFFSET,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_translation_$index"
    )
    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_alpha_$index"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = translation
                this.alpha = alpha
            },
        elevation = CardDefaults.cardElevation(defaultElevation = AnimationConstants.CARD_ELEVATION_DP.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            LabelChip(text = label)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = AnimationConstants.DETAIL_CARD_TOP_PADDING_DP.dp)
            )
        }
    }
}
