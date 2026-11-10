package com.x.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.x.player.domain.Album
import com.x.player.domain.Artist
import com.x.player.domain.Folder
import com.x.player.domain.LibraryIndex
import com.x.player.domain.SongSort
import com.x.player.domain.Track
import com.x.player.domain.applySort
import com.x.player.service.PlayerController
import com.x.player.service.PlayerState
import com.x.player.ui.components.Artwork
import com.x.player.ui.components.EmptyState
import com.x.player.ui.components.TrackRow
import com.x.player.util.formatCount

private val TABS = listOf("Songs", "Albums", "Artists", "Folders")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    index: LibraryIndex,
    sort: SongSort,
    scanning: Boolean,
    state: PlayerState,
    player: PlayerController,
    onSort: (SongSort) -> Unit,
    onRescan: () -> Unit,
    onSearch: () -> Unit,
    onAlbum: (Long) -> Unit,
    onArtist: (Int) -> Unit,
    onFolder: (Int) -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("X", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary) },
                actions = {
                    if (tab == 0) {
                        Box {
                            IconButton(onClick = { sortMenu = true }) {
                                Icon(Icons.Rounded.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                SongSort.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                option.label,
                                                color = if (option == sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        onClick = { sortMenu = false; onSort(option) },
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = onRescan) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Rescan library")
                    }
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                TABS.forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                }
            }
            if (scanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            if (index.tracks.isEmpty()) {
                EmptyState(
                    title = if (scanning) "Scanning your music…" else "No music found",
                    message = if (scanning) "This only takes a moment." else "Songs shorter than 30 seconds are skipped. Tap refresh after adding music.",
                )
            } else when (tab) {
                0 -> SongsTab(index.tracks.applySort(sort), state, player)
                1 -> AlbumsTab(index.albums, onAlbum)
                2 -> ArtistsTab(index.artists, onArtist)
                else -> FoldersTab(index.folders, onFolder)
            }
        }
    }
}

@Composable
private fun SongsTab(tracks: List<Track>, state: PlayerState, player: PlayerController) {
    LazyColumn(Modifier.fillMaxSize()) {
        item(key = "shuffle") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { player.shuffleAll(tracks) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Shuffle all · " + formatCount(tracks.size, "song"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
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
            )
        }
    }
}

@Composable
private fun AlbumsTab(albums: List<Album>, onAlbum: (Long) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        gridItems(albums, key = { it.id }) { album ->
            Column(Modifier.clickable { onAlbum(album.id) }) {
                Artwork(
                    album.artworkUri,
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    corner = 12.dp,
                    iconSize = 48.dp,
                )
                Spacer(Modifier.padding(top = 8.dp))
                Text(album.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ArtistsTab(artists: List<Artist>, onArtist: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(artists, key = { it.key }) { artist ->
            ListRow(
                title = artist.name,
                subtitle = formatCount(artist.albumCount, "album") + " · " + formatCount(artist.trackCount, "song"),
                onClick = { onArtist(artist.key) },
            ) { Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun FoldersTab(folders: List<Folder>, onFolder: (Int) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(folders, key = { it.key }) { folder ->
            ListRow(
                title = folder.name,
                subtitle = formatCount(folder.trackCount, "song") + " · " + folder.path,
                onClick = { onFolder(folder.key) },
            ) { Icon(Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun ListRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) { leading() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
