package com.x.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Seek bar whose played part is a moving wave while music plays (straight when paused).
 * [onDrag] reports the live fraction while dragging (null when released); [onSeek] fires once on
 * release or tap.
 */
@Composable
fun WavySeekBar(
    fraction: Float,
    playing: Boolean,
    color: Color,
    onSeek: (Float) -> Unit,
    onDrag: (Float?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val amplitude by animateFloatAsState(if (playing) 1f else 0f, label = "amplitude")
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnDrag by rememberUpdatedState(onDrag)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    currentOnSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val f = (offset.x / size.width).coerceIn(0f, 1f)
                        dragFraction = f
                        currentOnDrag(f)
                    },
                    onDragEnd = {
                        dragFraction?.let { currentOnSeek(it) }
                        dragFraction = null
                        currentOnDrag(null)
                    },
                    onDragCancel = {
                        dragFraction = null
                        currentOnDrag(null)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val f = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragFraction = f
                        currentOnDrag(f)
                    },
                )
            },
    ) {
        val w = size.width
        val cy = size.height / 2f
        val f = (dragFraction ?: fraction).coerceIn(0f, 1f)
        val x = w * f
        val stroke = 4.dp.toPx()
        val amp = 5.dp.toPx() * amplitude
        val waveLength = 26.dp.toPx()
        val twoPi = (2 * PI).toFloat()

        drawLine(
            color = color.copy(alpha = 0.28f),
            start = Offset(x, cy),
            end = Offset(w, cy),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )

        val path = Path()
        path.moveTo(0f, cy + amp * sin(phase))
        var px = 3f
        while (px <= x) {
            path.lineTo(px, cy + amp * sin(phase + px / waveLength * twoPi))
            px += 3f
        }
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        drawRoundRect(
            color = color,
            topLeft = Offset(x - 2.dp.toPx(), cy - 11.dp.toPx()),
            size = Size(4.dp.toPx(), 22.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}
