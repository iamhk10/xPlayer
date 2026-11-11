package com.x.player.ui.components

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.x.player.util.LyricLine

/** Keeps the returned angle (degrees) turning while [playing] is true. */
@Composable
private fun rememberSpin(playing: Boolean, periodMs: Int): Animatable<Float, AnimationVector1D> {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        if (playing) {
            while (true) {
                rotation.animateTo(rotation.value + 360f, tween(periodMs, easing = LinearEasing))
                rotation.snapTo(rotation.value % 360f)
            }
        }
    }
    return rotation
}

@Composable
fun VinylArt(uri: Uri?, playing: Boolean, modifier: Modifier = Modifier) {
    val rotation = rememberSpin(playing, 6000)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer { rotationZ = rotation.value },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(Color(0xFF0E0E12), radius = r)
            var ring = r * 0.97f
            while (ring > r * 0.42f) {
                drawCircle(Color(0xFF26252E), radius = ring, style = Stroke(width = 1.2f))
                ring -= r * 0.045f
            }
        }
        Artwork(
            uri = uri,
            modifier = Modifier.fillMaxSize(0.38f),
            corner = 400.dp,
            iconSize = 40.dp,
        )
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF0B0B12)),
        )
    }
}

@Composable
fun CassetteArt(title: String, artist: String, playing: Boolean, modifier: Modifier = Modifier) {
    val rotation = rememberSpin(playing, 3000)
    Box(modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1.6f)) {
            val labelTop = maxHeight * 0.14f
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRoundRect(Color(0xFF2A2833), cornerRadius = CornerRadius(h * 0.06f))
                drawRoundRect(
                    Color(0xFFEDE7DA),
                    topLeft = Offset(w * 0.08f, h * 0.1f),
                    size = Size(w * 0.84f, h * 0.5f),
                    cornerRadius = CornerRadius(h * 0.03f),
                )
                drawRoundRect(
                    Color(0xFF14131A),
                    topLeft = Offset(w * 0.2f, h * 0.64f),
                    size = Size(w * 0.6f, h * 0.24f),
                    cornerRadius = CornerRadius(h * 0.12f),
                )
                listOf(0.34f, 0.66f).forEach { fx ->
                    val c = Offset(w * fx, h * 0.76f)
                    val rr = h * 0.085f
                    drawCircle(Color(0xFFEDE7DA), radius = rr, center = c)
                    for (k in 0 until 3) {
                        rotate(rotation.value + k * 60f, pivot = c) {
                            drawLine(
                                Color(0xFF14131A),
                                start = Offset(c.x - rr * 0.85f, c.y),
                                end = Offset(c.x + rr * 0.85f, c.y),
                                strokeWidth = rr * 0.2f,
                            )
                        }
                    }
                    drawCircle(Color(0xFF14131A), radius = rr * 0.3f, center = c)
                }
            }
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.76f)
                    .padding(top = labelTop),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    color = Color(0xFF2A2833),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    artist,
                    color = Color(0xFF55505F),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun LyricsPanel(
    lines: List<LyricLine>,
    positionMs: Long,
    accent: Color,
    onSeek: (Long) -> Unit,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (lines.isEmpty()) {
        Column(
            modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("No synced lyrics found", style = MaterialTheme.typography.titleMedium)
            Text(
                "Keep a .lrc file with the same name next to the song, or choose one here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            OutlinedButton(onClick = onPick) { Text("Choose .lrc file") }
        }
    } else {
        val current = lines.indexOfLast { it.timeMs <= positionMs }
        val listState = rememberLazyListState()
        LaunchedEffect(current) {
            if (current >= 0) listState.animateScrollToItem(current, scrollOffset = -200)
        }
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 80.dp),
        ) {
            itemsIndexed(lines) { i, line ->
                val active = i == current
                Text(
                    text = line.text.ifBlank { "♪" },
                    color = if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = if (active) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeek(line.timeMs) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}
