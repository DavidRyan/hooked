package com.hooked.catches.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SunSection(
    sunriseHour: Float,
    sunsetHour: Float,
    catchHour: Float,
    modifier: Modifier = Modifier
) {
    // Sky gradient colors keyed on catch hour
    val gradientColors = when {
        catchHour < 7f  -> listOf(Color(0xFF1A1040), Color(0xFF7B3A1E))   // dawn: indigo → burnt orange
        catchHour < 10f -> listOf(Color(0xFF1A2550), Color(0xFF3A5F8A))   // morning: navy → steel blue
        catchHour < 16f -> listOf(Color(0xFF0D1B2A), Color(0xFF1E3A5F))   // midday: dark navy → twilight blue
        catchHour < 18f -> listOf(Color(0xFF1A1040), Color(0xFF7B3A1E))   // dusk-early: indigo → orange
        catchHour < 20f -> listOf(Color(0xFF2D1B4E), Color(0xFF7B4A0A))   // dusk: purple → amber
        else            -> listOf(Color(0xFF050A0F), Color(0xFF0D1B2A))   // night: near-black → dark navy
    }

    // Arc draw animation
    var startArcAnimation by remember { mutableStateOf(false) }
    val arcProgress by animateFloatAsState(
        targetValue = if (startArcAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "arc_draw"
    )
    LaunchedEffect(Unit) { startArcAnimation = true }

    // Pulsing sun dot alpha
    val infiniteTransition = rememberInfiniteTransition(label = "sun_pulse")
    val sunAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sun_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                brush = Brush.verticalGradient(gradientColors),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padPx = 16.dp.toPx()
            val startX = padPx
            val endX = size.width - padPx
            val bottomY = size.height - 12.dp.toPx()
            val peakY = 20.dp.toPx()
            val width = endX - startX

            // Build the full arc path
            val fullPath = Path().apply {
                moveTo(startX, bottomY)
                cubicTo(
                    startX + width * 0.25f, bottomY - 10.dp.toPx(),
                    startX + width * 0.35f, peakY,
                    startX + width * 0.5f, peakY
                )
                cubicTo(
                    startX + width * 0.65f, peakY,
                    startX + width * 0.75f, bottomY - 10.dp.toPx(),
                    endX, bottomY
                )
            }

            // Measure and extract segment based on arcProgress
            val pathMeasure = PathMeasure()
            pathMeasure.setPath(fullPath, false)
            val pathLength = pathMeasure.length
            val drawnPath = Path()
            pathMeasure.getSegment(0f, arcProgress * pathLength, drawnPath, true)

            drawPath(
                path = drawnPath,
                color = Color.White,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Horizon line
            val horizonY = (bottomY + peakY) / 2f
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(0f, horizonY),
                end = Offset(size.width, horizonY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Sun dot position
            val catchT = catchHour / 24f
            val catchX = startX + width * catchT
            val catchY = when {
                catchT <= 0.5f -> {
                    val localT = catchT / 0.5f
                    val p0y = bottomY
                    val p1y = bottomY - 10.dp.toPx()
                    val p2y = peakY
                    val p3y = peakY
                    val u = 1 - localT
                    u * u * u * p0y + 3 * u * u * localT * p1y +
                        3 * u * localT * localT * p2y + localT * localT * localT * p3y
                }
                else -> {
                    val localT = (catchT - 0.5f) / 0.5f
                    val p0y = peakY
                    val p1y = peakY
                    val p2y = bottomY - 10.dp.toPx()
                    val p3y = bottomY
                    val u = 1 - localT
                    u * u * u * p0y + 3 * u * u * localT * p1y +
                        3 * u * localT * localT * p2y + localT * localT * localT * p3y
                }
            }

            val center = Offset(catchX, catchY)

            // Glow halo behind the dot
            drawCircle(
                color = Color.Yellow.copy(alpha = sunAlpha * 0.3f),
                radius = 12.dp.toPx(),
                center = center
            )

            // Sun dot with pulsing alpha
            drawCircle(
                color = Color.Yellow.copy(alpha = sunAlpha),
                radius = 6.dp.toPx(),
                center = center
            )
        }

        LabelChip(
            text = "Sun",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        )
    }
}
