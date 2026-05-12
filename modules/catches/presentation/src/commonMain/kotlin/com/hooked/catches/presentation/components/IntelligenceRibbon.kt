package com.hooked.catches.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hooked.catches.domain.entities.RibbonInsightEntity
import com.hooked.catches.domain.usecases.GetRibbonInsightUseCase
import com.hooked.core.domain.UseCaseResult
import com.hooked.theme.Colors
import org.koin.compose.koinInject

@Composable
fun IntelligenceRibbon(
    onTap: (headline: String?) -> Unit,
    modifier: Modifier = Modifier,
    getRibbonInsight: GetRibbonInsightUseCase = koinInject()
) {
    var insight by remember { mutableStateOf<RibbonInsightEntity?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        when (val result = getRibbonInsight()) {
            is UseCaseResult.Success -> {
                insight = result.data
                loaded = true
            }
            is UseCaseResult.Error -> {
                // Quietly hide on error — ribbon is optional decoration
                loaded = true
            }
        }
    }

    AnimatedVisibility(
        visible = loaded && insight != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        insight?.let { current ->
            RibbonContent(
                insight = current,
                onTap = { onTap(current.headline) },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun RibbonContent(
    insight: RibbonInsightEntity,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Colors.surface1)
            .clickable(onClick = onTap)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Colors.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✦",
                style = MaterialTheme.typography.titleMedium,
                color = Colors.primary
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = insight.headline,
                style = MaterialTheme.typography.titleMedium,
                color = Colors.text
            )
            Text(
                text = insight.body,
                style = MaterialTheme.typography.bodyMedium,
                color = Colors.subtext1
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Colors.subtext0
        )
    }
}
