package com.x.player.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.x.player.domain.Track
import com.x.player.service.PlayerState
import com.x.player.util.formatTime

/** Rounded artwork with a music-note fallback when no image can be loaded. */
@Composable
fun Artwork(
    uri: Uri?,
    modifier: Modifier = Modifier,
    corner: Dp = 8.dp,
    iconSize: Dp = 24.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(iconSize),
        )
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Favourite / playlist hooks shared by every track list. */
class TrackMenu(
    val favorites: Set<Long>,
    val onToggleFavorite: (Track) -> Unit,
    val onAddToPlaylist: (Track) -> Unit,
)

private fun shareTrack(context: android.content.Context, track: Track) {
    try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, track.uri)
            putExtra(Intent.EXTRA_TITLE, track.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share song"))
    } catch (e: Exception) {
        // No app can take it; nothing to do.
    }
}

@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    menu: TrackMenu,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    onRemove: (() -> Unit)? = null,
    card: Boolean = false,
) {
    val favorite = track.id in menu.favorites
    var menuOpen by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(18.dp)
    val cardLook = if (card) {
        Modifier
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f), cardShape)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(cardLook)
            .clickable(onClick = onClick)
            .padding(start = if (card) 10.dp else 16.dp, end = 4.dp, top = if (card) 8.dp else 6.dp, bottom = if (card) 8.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showArtwork) {
            Artwork(track.artworkUri, Modifier.size(if (card) 52.dp else 48.dp), corner = if (card) 12.dp else 8.dp)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist + " · " + track.album,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatTime(track.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Play next") },
                    leadingIcon = { Icon(Icons.Rounded.SkipNext, contentDescription = null) },
                    onClick = { menuOpen = false; onPlayNext() },
                )
                DropdownMenuItem(
                    text = { Text("Add to queue") },
                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    onClick = { menuOpen = false; onAddToQueue() },
                )
                DropdownMenuItem(
                    text = { Text(if (favorite) "Remove from favourites" else "Add to favourites") },
                    leadingIcon = {
                        Icon(if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = null)
                    },
                    onClick = { menuOpen = false; menu.onToggleFavorite(track) },
                )
                DropdownMenuItem(
                    text = { Text("Add to playlist") },
                    leadingIcon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null) },
                    onClick = { menuOpen = false; menu.onAddToPlaylist(track) },
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                    onClick = { menuOpen = false; shareTrack(context, track) },
                )
                if (onRemove != null) {
                    DropdownMenuItem(
                        text = { Text("Remove from this list") },
                        leadingIcon = { Icon(Icons.Rounded.Close, contentDescription = null) },
                        onClick = { menuOpen = false; onRemove() },
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
