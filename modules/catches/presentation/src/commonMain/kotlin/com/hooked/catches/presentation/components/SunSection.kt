package com.hooked.catches.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padding = 16.dp.toPx()
            val startX = padding
            val endX = size.width - padding
            val bottomY = size.height - 12.dp.toPx()
            val peakY = 20.dp.toPx()
            val width = endX - startX
            
            val path = Path().apply {
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
            
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
            
            val sunriseX = startX + width * (sunriseHour / 24f)
            val sunsetX = startX + width * (sunsetHour / 24f)
            val horizonY = (bottomY + peakY) / 2f
            
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = androidx.compose.ui.geometry.Offset(0f, horizonY),
                end = androidx.compose.ui.geometry.Offset(size.width, horizonY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            val catchT = catchHour / 24f
            val catchX = startX + width * catchT
            val catchY = when {
                catchT <= 0.5f -> {
                    val localT = catchT / 0.5f
                    val p0y = bottomY
                    val p1y = bottomY - 10.dp.toPx()
                    val p2y = peakY
                    val p3y = peakY
                    val oneMinusT = 1 - localT
                    oneMinusT * oneMinusT * oneMinusT * p0y +
                        3 * oneMinusT * oneMinusT * localT * p1y +
                        3 * oneMinusT * localT * localT * p2y +
                        localT * localT * localT * p3y
                }
                else -> {
                    val localT = (catchT - 0.5f) / 0.5f
                    val p0y = peakY
                    val p1y = peakY
                    val p2y = bottomY - 10.dp.toPx()
                    val p3y = bottomY
                    val oneMinusT = 1 - localT
                    oneMinusT * oneMinusT * oneMinusT * p0y +
                        3 * oneMinusT * oneMinusT * localT * p1y +
                        3 * oneMinusT * localT * localT * p2y +
                        localT * localT * localT * p3y
                }
            }
            
            drawCircle(
                color = Color.Yellow,
                radius = 6.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(catchX, catchY)
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
