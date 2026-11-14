package com.x.player.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.x.player.ui.theme.LocalIsDark
import androidx.compose.ui.text.font.FontWeight
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
import com.x.player.ui.components.SleepTimerDialog
import com.x.player.ui.components.VinylArt
import com.x.player.ui.components.WavySeekBar
import com.x.player.ui.components.rememberArtworkColor
import com.x.player.util.LyricLine
import com.x.player.util.formatTime
import kotlinx.coroutines.launch

private val ART_STYLES = listOf("Cover", "Vinyl", "Cassette")

@Composable
fun NowPlayingScreen(
    state: PlayerState,
    player: PlayerController,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onBack: () -> Unit,
    onQueue: () -> Unit,
    onEqualizer: () -> Unit,
    lyrics: LyricsRepository,
    keepScreenOn: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("x_ui", Context.MODE_PRIVATE) }
    var artMode by remember { mutableStateOf(prefs.getInt("art_mode", 0)) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var lyricsReload by remember { mutableStateOf(0) }
    var preview by remember { mutableStateOf<Float?>(null) }
    val sleep by player.sleep.collectAsState()

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

    // Colours: tint the whole screen with the artwork colour.
    // This screen is always dark, whatever the app theme is, so white text stays readable.
    val bg = Color(0xFF0B0B12)
    val position by player.position.collectAsState()
    val themeIsDark = LocalIsDark.current
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = !themeIsDark
            controller?.isAppearanceLightNavigationBars = !themeIsDark
        }
    }
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    val artColor = rememberArtworkColor(state.artworkUri)
    val fallback = lerp(bg, MaterialTheme.colorScheme.primary, 0.20f)
    val tint by animateColorAsState(
        targetValue = artColor?.let { lerp(bg, it, 0.38f) } ?: fallback,
        label = "tint",
    )
    val darker = lerp(tint, Color.Black, 0.35f)
    val soft = Color.White.copy(alpha = 0.72f)

    val duration = state.durationMs.coerceAtLeast(1L)
    val fraction = (position.toFloat() / duration).coerceIn(0f, 1f)
    val shownPosition = ((preview ?: fraction) * duration).toLong()
    val currentLine = lyricLines.lastOrNull { it.timeMs <= position }?.text?.takeIf { it.isNotBlank() }
    val badge = remember(state.path, state.sizeBytes, state.durationMs) {
        val ext = state.path.substringAfterLast('.', "").uppercase()
        val kbps = if (state.durationMs > 0 && state.sizeBytes > 0) (state.sizeBytes * 8L / state.durationMs).toInt() else 0
        listOfNotNull(
            ext.takeIf { it.isNotEmpty() && it.length <= 5 },
            kbps.takeIf { it > 0 }?.let { "$it kbps" },
        ).joinToString(" • ")
    }

    Column(Modifier.fillMaxSize().background(tint)) {
        // ---- artwork zone -------------------------------------------------------------------
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                showLyrics -> Box(
                    Modifier.fillMaxSize().statusBarsPadding().padding(top = 56.dp, start = 16.dp, end = 16.dp),
                ) {
                    LyricsPanel(
                        lines = lyricLines,
                        positionMs = position,
                        accent = Color.White,
                        onSeek = { player.seekTo(it) },
                        onPick = { lrcPicker.launch(arrayOf("*/*")) },
                    )
                }
                artMode == 1 -> Box(
                    Modifier.fillMaxSize().statusBarsPadding().padding(top = 56.dp, start = 40.dp, end = 40.dp),
                    contentAlignment = Alignment.Center,
                ) { VinylArt(state.artworkUri, state.isPlaying) }
                artMode == 2 -> Box(
                    Modifier.fillMaxSize().statusBarsPadding().padding(top = 56.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center,
                ) { CassetteArt(state.title, state.artist, state.isPlaying) }
                else -> Artwork(
                    uri = state.artworkUri,
                    modifier = Modifier.fillMaxSize(),
                    corner = 0.dp,
                    iconSize = 96.dp,
                )
            }

            // Fade the artwork into the tinted panel below.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, tint))),
            )

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f)),
            ) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close", tint = Color.White)
            }

            if (!showLyrics && currentLine != null) {
                Text(
                    currentLine,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                )
            }
        }

        // ---- controls panel -----------------------------------------------------------------
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(tint, darker)))
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        state.title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        state.artist,
                        color = soft,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                SquircleButton(Icons.Rounded.Add, "Add to playlist", onAddToPlaylist)
                Spacer(Modifier.width(8.dp))
                SquircleButton(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    "Favourite",
                    onToggleFavorite,
                )
            }

            Spacer(Modifier.height(8.dp))
            WavySeekBar(
                fraction = fraction,
                playing = state.isPlaying,
                color = Color.White,
                onSeek = { player.seekTo((it * duration).toLong()) },
                onDrag = { preview = it },
            )
            Box(Modifier.fillMaxWidth()) {
                Text(
                    formatTime(shownPosition),
                    color = soft,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                if (badge.isNotEmpty()) {
                    Text(
                        badge,
                        color = soft,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Text(
                    formatTime(state.durationMs),
                    color = soft,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().height(76.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ControlKey(
                    Modifier.weight(1f),
                    Icons.Rounded.SkipPrevious,
                    "Previous",
                    Color.White.copy(alpha = 0.16f),
                    Color.White,
                ) { player.previous() }
                ControlKey(
                    Modifier.weight(1.15f),
                    if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (state.isPlaying) "Pause" else "Play",
                    Color.White,
                    Color(0xFF14101F),
                ) { player.togglePlayPause() }
                ControlKey(
                    Modifier.weight(1f),
                    Icons.Rounded.SkipNext,
                    "Next",
                    Color.White.copy(alpha = 0.16f),
                    Color.White,
                ) { player.next() }
            }

            sleep?.let {
                Text(
                    if (it.endOfSong) "Sleep: after this song" else "Sleep in " + formatTime(it.remainingMs),
                    color = soft,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillButton(Icons.Rounded.LibraryMusic, "Queue", false, Modifier.weight(1f)) { onQueue() }
                    PillButton(Icons.Rounded.Timer, "Sleep timer", sleep != null, Modifier.weight(1f)) { showSleep = true }
                    PillButton(Icons.Rounded.MusicNote, "Lyrics", showLyrics, Modifier.weight(1f)) { showLyrics = !showLyrics }
                    PillButton(Icons.Rounded.Shuffle, "Shuffle", state.shuffle, Modifier.weight(1f)) { player.toggleShuffle() }
                    PillButton(
                        if (state.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Repeat",
                        state.repeat != Player.REPEAT_MODE_OFF,
                        Modifier.weight(1f),
                    ) { player.cycleRepeat() }
                }
                Spacer(Modifier.width(10.dp))
                Box {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { moreMenu = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = Color(0xFF14101F))
                    }
                    DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Equalizer") },
                            onClick = { moreMenu = false; onEqualizer() },
                        )
                        DropdownMenuItem(
                            text = { Text("Art style: " + ART_STYLES[artMode]) },
                            onClick = {
                                moreMenu = false
                                artMode = (artMode + 1) % ART_STYLES.size
                                prefs.edit().putInt("art_mode", artMode).apply()
                                showLyrics = false
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
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
}

@Composable
private fun SquircleButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color(0xFF14101F))
    }
}

@Composable
private fun ControlKey(
    modifier: Modifier,
    icon: ImageVector,
    description: String,
    container: Color,
    content: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(28.dp))
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = content, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun PillButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(if (active) Color.White.copy(alpha = 0.22f) else Color.Transparent)
            .border(1.dp, Color.White.copy(alpha = if (active) 0.6f else 0.3f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}
