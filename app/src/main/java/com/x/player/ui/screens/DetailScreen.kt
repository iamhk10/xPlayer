package com.x.player.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.x.player.domain.Track
import com.x.player.service.PlayerController
import com.x.player.service.PlayerState
import com.x.player.ui.components.Artwork
import com.x.player.ui.components.CircleIconButton
import com.x.player.ui.components.GlassSurface
import com.x.player.ui.components.TrackMenu
import com.x.player.ui.components.TrackRow
import com.x.player.ui.components.appBackground
import com.x.player.ui.theme.LocalBottomInset
import com.x.player.util.formatCount
import com.x.player.util.formatListening

/**
 * Album, artist, folder, playlist and smart-list page: a soft blurred backdrop of the artwork,
 * centred cover, "60 songs • 3h 37m" chip, Play / Shuffle and rounded track cards.
 */
@Composable
fun DetailScreen(
    title: String,
    subtitle: String,
    artworkUri: Uri?,
    tracks: List<Track>,
    state: PlayerState,
    player: PlayerController,
    menu: TrackMenu,
    onBack: () -> Unit,
    onRemove: ((Track) -> Unit)? = null,
    topActions: (@Composable RowScope.() -> Unit)? = null,
    showArtwork: Boolean = true,
) {
    val context = LocalContext.current
    val heroArt = artworkUri ?: tracks.firstOrNull()?.artworkUri
    val totalMs = remember(tracks) { tracks.sumOf { it.durationMs } }
    val background = MaterialTheme.colorScheme.background

    Box(
        Modifier
            .fillMaxSize()
            .appBackground(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LocalBottomInset.current + 16.dp),
        ) {
            item(key = "header") {
                Box(Modifier.fillMaxWidth()) {
                    if (heroArt != null) {
                        // A 32 px copy stretched over the header reads as a blur and costs nothing to draw.
                        AsyncImage(
                            model = remember(heroArt) { ImageRequest.Builder(context).data(heroArt).size(32).build() },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alpha = 0.6f,
                            modifier = Modifier.matchParentSize(),
                        )
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(Brush.verticalGradient(0.2f to Color.Transparent, 1f to background)),
                        )
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Artwork(
                            heroArt,
                            Modifier
                                .fillMaxWidth(0.62f)
                                .aspectRatio(1f)
                                .shadow(20.dp, RoundedCornerShape(26.dp)),
                            corner = 26.dp,
                            iconSize = 72.dp,
                        )
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp),
                        )
                        if (subtitle.isNotBlank()) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
                            )
                        }
                        if (tracks.isNotEmpty()) {
                            Text(
                                formatCount(tracks.size, "song") + " • " + formatListening(totalMs),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp),
                            ) {
                                Button(
                                    onClick = { player.play(tracks, 0) },
                                    shape = RoundedCornerShape(26.dp),
                                    modifier = Modifier.weight(1f).height(50.dp),
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Play", fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.width(12.dp))
                                FilledTonalButton(
                                    onClick = { player.shuffleAll(tracks) },
                                    shape = RoundedCornerShape(26.dp),
                                    modifier = Modifier.weight(1f).height(50.dp),
                                ) {
                                    Icon(Icons.Rounded.Shuffle, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Shuffle", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
            if (tracks.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Nothing here yet. Add songs from the ⋮ menu on any track.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
            itemsIndexed(tracks, key = { _, t -> t.id }) { i, track ->
                TrackRow(
                    track = track,
                    isCurrent = state.currentId == track.id.toString(),
                    onClick = { player.play(tracks, i) },
                    onPlayNext = { player.playNext(track) },
                    onAddToQueue = { player.addToQueue(track) },
                    menu = menu,
                    showArtwork = showArtwork,
                    onRemove = if (onRemove != null) ({ onRemove(track) }) else null,
                    card = true,
                )
            }
        }

        // Back button and page actions float above the list.
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.weight(1f))
            if (topActions != null) {
                GlassSurface(Modifier, RoundedCornerShape(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, content = topActions)
                }
            }
        }
    }
}
