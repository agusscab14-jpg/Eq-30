package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SliderBackground

@Composable
fun VerticalEqSlider(
    value: Float, // -15f to +15f
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = -15f..15f
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(40.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val trackY = 12.dp.toPx()
                        val trackHeight = size.height - 24.dp.toPx()
                        val clampedY = offset.y.coerceIn(trackY, trackY + trackHeight)
                        val normalized = 1f - ((clampedY - trackY) / trackHeight)
                        val newValue = range.start + normalized * (range.endInclusive - range.start)
                        onValueChange(newValue.coerceIn(range))
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val trackY = 12.dp.toPx()
                        val trackHeight = size.height - 24.dp.toPx()
                        val clampedY = change.position.y.coerceIn(trackY, trackY + trackHeight)
                        val normalized = 1f - ((clampedY - trackY) / trackHeight)
                        val newValue = range.start + normalized * (range.endInclusive - range.start)
                        onValueChange(newValue.coerceIn(range))
                    }
                }
        ) {
            val trackWidth = 8.dp.toPx()
            val trackHeight = size.height - 24.dp.toPx() // padding top/bottom
            val trackX = center.x - trackWidth / 2
            val trackY = 12.dp.toPx()

            // Draw track background
            drawRoundRect(
                color = SliderBackground,
                topLeft = Offset(trackX, trackY),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(trackWidth / 2, trackWidth / 2)
            )

            // Draw zero line
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(center.x - 12.dp.toPx(), center.y),
                end = Offset(center.x + 12.dp.toPx(), center.y),
                strokeWidth = 2f
            )

            // Calculate fill height based on value
            val normalizedValue = (value - range.start) / (range.endInclusive - range.start)
            val thumbY = trackY + trackHeight * (1f - normalizedValue)

            // Draw active track
            val trackGradient = Brush.verticalGradient(
                colors = listOf(NeonPink, NeonPurple),
                startY = trackY,
                endY = trackY + trackHeight
            )

            if (value > 0) {
                // Fill from center up to thumb
                drawRoundRect(
                    brush = trackGradient,
                    topLeft = Offset(trackX, thumbY),
                    size = Size(trackWidth, center.y - thumbY),
                    cornerRadius = CornerRadius(trackWidth / 2, trackWidth / 2)
                )
            } else {
                // Fill from center down to thumb
                drawRoundRect(
                    brush = trackGradient,
                    topLeft = Offset(trackX, center.y),
                    size = Size(trackWidth, thumbY - center.y),
                    cornerRadius = CornerRadius(trackWidth / 2, trackWidth / 2)
                )
            }

            // Draw thumb (optional, a small bar across)
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(center.x - 16.dp.toPx(), thumbY - 4.dp.toPx()),
                size = Size(32.dp.toPx(), 8.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}


