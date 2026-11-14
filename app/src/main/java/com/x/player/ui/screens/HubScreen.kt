package com.x.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.x.player.domain.Playlist
import com.x.player.ui.components.ScreenFrame
import com.x.player.ui.components.SectionLabel
import com.x.player.util.formatCount

private class HubTile(val icon: ImageVector, val title: String, val subtitle: String, val onClick: () -> Unit)

/**
 * The button next to Settings: quick access to favourites, smart lists, the folders you create,
 * and the handy tools (stats, equaliser, sleep timer, duplicates, rescan).
 */
@Composable
fun HubScreen(
    favoriteCount: Int,
    deviceFolderCount: Int,
    folders: List<Playlist>,
    sleepActive: Boolean,
    speedLabel: String,
    scanning: Boolean,
    onBack: () -> Unit,
    onFavorites: () -> Unit,
    onSmart: (String) -> Unit,
    onDeviceFolders: () -> Unit,
    onOpenFolder: (Long) -> Unit,
    onNewFolder: () -> Unit,
    onNewPlaylist: () -> Unit,
    onStats: () -> Unit,
    onEqualizer: () -> Unit,
    onSleep: () -> Unit,
    onSpeed: () -> Unit,
    onDuplicates: () -> Unit,
    onRescan: () -> Unit,
) {
    val quick = listOf(
        HubTile(Icons.Rounded.Favorite, "Favourites", formatCount(favoriteCount, "song"), onFavorites),
        HubTile(Icons.Rounded.History, "Recently played", "Pick up where you left", { onSmart("recent") }),
        HubTile(Icons.Rounded.TrendingUp, "Most played", "Your top songs", { onSmart("most") }),
        HubTile(Icons.Rounded.NewReleases, "Recently added", "Newest on your phone", { onSmart("added") }),
        HubTile(Icons.Rounded.Folder, "Device folders", formatCount(deviceFolderCount, "folder"), onDeviceFolders),
        HubTile(Icons.Rounded.BarChart, "Listening stats", "Day, week, month, year", onStats),
        HubTile(Icons.Rounded.Equalizer, "Equalizer", "5-band and presets", onEqualizer),
        HubTile(Icons.Rounded.Timer, "Sleep timer", if (sleepActive) "Running" else "Off", onSleep),
        HubTile(Icons.Rounded.Speed, "Playback speed", speedLabel, onSpeed),
        HubTile(Icons.Rounded.ContentCopy, "Find duplicates", "Free up space", onDuplicates),
        HubTile(Icons.Rounded.Refresh, "Rescan library", if (scanning) "Scanning…" else "Look for new songs", onRescan),
    )

    ScreenFrame(title = "Hub", onBack = onBack) {
        SectionLabel("Quick access")
        quick.chunked(2).forEach { pair ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { tile -> TileCard(tile, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        SectionLabel("My folders")
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FilledTonalButton(onClick = onNewFolder, modifier = Modifier.weight(1f).height(48.dp)) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New folder")
            }
            FilledTonalButton(onClick = onNewPlaylist, modifier = Modifier.weight(1f).height(48.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New playlist")
            }
        }
        if (folders.isEmpty()) {
            Text(
                "Folders are your own groups of songs, whatever folder they sit in on the phone. " +
                    "Create one, then add songs with the ⋮ menu on any track.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        } else {
            folders.forEach { folder ->
                ListRow(
                    title = folder.name,
                    subtitle = formatCount(folder.trackIds.size, "song"),
                    onClick = { onOpenFolder(folder.id) },
                ) { Icon(Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun TileCard(tile: HubTile, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .height(76.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f), shape)
            .clickable(onClick = tile.onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tile.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tile.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                tile.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
