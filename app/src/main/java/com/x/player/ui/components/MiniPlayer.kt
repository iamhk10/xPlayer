package com.x.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.x.player.service.PlayerController
import com.x.player.service.PlayerState

val MINI_PLAYER_HEIGHT = 62.dp

/**
 * Apple-Music-style mini player: a floating rounded card with artwork, title, play/pause and next.
 * Swipe it sideways to change song. The thin progress line is drawn in the draw phase only, so the
 * 400 ms position updates never recompose this card.
 */
@Composable
fun MiniPlayer(
    state: PlayerState,
    player: PlayerController,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val position = player.position.collectAsState()
    val accent = MaterialTheme.colorScheme.primary
    val duration = state.durationMs

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(MINI_PLAYER_HEIGHT),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val f = if (duration > 0) (position.value.toFloat() / duration).coerceIn(0f, 1f) else 0f
                    val h = 2.dp.toPx()
                    drawRect(accent, topLeft = Offset(0f, size.height - h), size = Size(size.width * f, h))
                }
                .pointerInput(player) {
                    var dx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f },
                        onDragEnd = {
                            if (dx < -90f) player.next() else if (dx > 90f) player.previous()
                        },
                        onHorizontalDrag = { _, delta -> dx += delta },
                    )
                }
                .clickable(onClick = onOpen)
                .padding(start = 9.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(state.artworkUri, Modifier.size(44.dp), corner = 11.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = player::togglePlayPause) {
                Icon(
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(30.dp),
                )
            }
            IconButton(onClick = player::next) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(28.dp))
            }
        }
    }
}
