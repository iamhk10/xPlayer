package com.x.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.x.player.data.LyricsRepository
import com.x.player.service.PlayerController
import com.x.player.service.PlayerState
import com.x.player.ui.components.Artwork
import com.x.player.ui.components.CassetteArt
import com.x.player.ui.components.LyricsPanel
import com.x.player.ui.components.VinylArt
import com.x.player.ui.components.SleepTimerDialog
import com.x.player.ui.components.rememberArtworkColor
import com.x.player.util.LyricLine
import com.x.player.util.formatTime
import kotlinx.coroutines.launch

@Composable
fun NowPlayingScreen(
    state: PlayerState,
    player: PlayerController,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    onQueue: () -> Unit,
    onEqualizer: () -> Unit,
    lyrics: LyricsRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("x_ui", Context.MODE_PRIVATE) }
    var artMode by remember { mutableStateOf(prefs.getInt("art_mode", 0)) }
    var showLyrics by remember { mutableStateOf(false) }
    var lyricsReload by remember { mutableStateOf(0) }
    val lyricLines by produceState(emptyList<LyricLine>(), state.currentId, lyricsReload) {
        value = emptyList()
        val id = state.currentId?.toLongOrNull()
        if (id != null) value = lyrics.load(id, state.path)
    }
    val lrcPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val id = state.currentId?.toLongOrNull()
        if (uri != null && id != null) {
            scope.launch { if (lyrics.import(id, uri)) lyricsReload++ }
        }
    }

    var dragging by remember { mutableStateOf<Float?>(null) }
    var showSleep by remember { mutableStateOf(false) }
    val sleep by player.sleep.collectAsState()

    val bg = MaterialTheme.colorScheme.background
    val artColor = rememberArtworkColor(state.artworkUri)
    val accent by animateColorAsState(
        targetValue = artColor?.let { lerp(it, Color.White, 0.35f) } ?: MaterialTheme.colorScheme.primary,
        label = "accent",
    )
    val wash by animateColorAsState(
        targetValue = artColor?.let { lerp(bg, it, 0.40f) } ?: bg,
        label = "wash",
    )
    val onAccent = Color(0xFF14101F)
    val duration = state.durationMs.coerceAtLeast(1L).toFloat()
    val position = (dragging ?: state.positionMs.toFloat()).coerceIn(0f, duration)

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(wash, bg)))
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close", modifier = Modifier.size(32.dp))
            }
            Text("Now playing", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showSleep = true }) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = "Sleep timer",
                        tint = if (sleep != null) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onEqualizer) {
                    Icon(Icons.Rounded.Tune, contentDescription = "Equalizer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onQueue) {
                    Icon(Icons.Rounded.LibraryMusic, contentDescription = "Queue")
                }
            }
        }
        sleep?.let {
            Text(
                if (it.endOfSong) "Sleep: after this song" else "Sleep in " + formatTime(it.remainingMs),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (showSleep) {
            SleepTimerDialog(
                active = sleep != null,
                onMinutes = { player.startSleepTimer(it); showSleep = false },
                onEndOfSong = { player.sleepAtEndOfSong(); showSleep = false },
                onCancelTimer = { player.cancelSleepTimer(); showSleep = false },
                onDismiss = { showSleep = false },
            )
        }

        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            if (showLyrics) {
                LyricsPanel(
                    lines = lyricLines,
                    positionMs = state.positionMs,
                    accent = accent,
                    onSeek = { player.seekTo(it) },
                    onPick = { lrcPicker.launch(arrayOf("*/*")) },
                )
            } else when (artMode) {
                1 -> VinylArt(state.artworkUri, state.isPlaying)
                2 -> CassetteArt(state.title, state.artist, state.isPlaying)
                else -> Artwork(
                    uri = state.artworkUri,
                    modifier = Modifier.fillMaxSize(),
                    corner = 24.dp,
                    iconSize = 96.dp,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = {
                artMode = (artMode + 1) % 3
                prefs.edit().putInt("art_mode", artMode).apply()
                showLyrics = false
            }) {
                Text(listOf("Cover", "Vinyl", "Cassette")[artMode], color = accent)
            }
            TextButton(onClick = { showLyrics = !showLyrics }) {
                Text(if (showLyrics) "Hide lyrics" else "Lyrics", color = accent)
            }
        }
        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favourite",
                    tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Slider(
            value = position,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { player.seekTo(it.toLong()) }
                dragging = null
            },
            valueRange = 0f..duration,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(position.toLong()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { player.toggleShuffle() }) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (state.shuffle) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { player.previous() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp))
            }
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = { player.togglePlayPause() }, modifier = Modifier.size(72.dp)) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = onAccent,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            IconButton(onClick = { player.next() }, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { player.cycleRepeat() }) {
                Icon(
                    if (state.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = if (state.repeat == Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurfaceVariant else accent,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
