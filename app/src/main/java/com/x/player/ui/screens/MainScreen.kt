package com.x.player.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.x.player.data.AppSettings
import com.x.player.data.db.TopRow
import com.x.player.domain.Album
import com.x.player.domain.Artist
import com.x.player.domain.FAVORITES_ID
import com.x.player.domain.KIND_PLAYLIST
import com.x.player.domain.LibraryIndex
import com.x.player.domain.Playlist
import com.x.player.domain.SongSort
import com.x.player.domain.Track
import com.x.player.domain.applySort
import com.x.player.service.PlayerController
import com.x.player.service.PlayerState
import com.x.player.ui.components.Artwork
import com.x.player.ui.components.EmptyState
import com.x.player.ui.components.MainTopBar
import com.x.player.ui.components.NavTab
import com.x.player.ui.components.TrackMenu
import com.x.player.ui.components.TrackRow
import com.x.player.ui.components.appBackground
import com.x.player.ui.theme.LocalBottomInset
import com.x.player.util.formatCount

/** Tabs of the floating nav bar, in order. */
val MAIN_TABS = listOf(
    NavTab("Home", Icons.Rounded.Home),
    NavTab("Songs", Icons.Rounded.MusicNote),
    NavTab("Albums", Icons.Rounded.Album),
    NavTab("Artists", Icons.Rounded.Person),
    NavTab("Playlists", Icons.Rounded.LibraryMusic),
)

@Composable
fun MainScreen(
    tab: Int,
    index: LibraryIndex,
    sort: SongSort,
    scanning: Boolean,
    state: PlayerState,
    player: PlayerController,
    settings: AppSettings,
    recentIds: List<Long>,
    topTracks: List<TopRow>,
    favoriteIds: List<Long>,
    playlists: List<Playlist>,
    menu: TrackMenu,
    onTab: (Int) -> Unit,
    onSort: (SongSort) -> Unit,
    onSearch: () -> Unit,
    onHub: () -> Unit,
    onSettings: () -> Unit,
    onAlbum: (Long) -> Unit,
    onArtist: (Int) -> Unit,
    onPlaylist: (Long) -> Unit,
    onNewPlaylist: () -> Unit,
    onSmart: (String) -> Unit,
) {
    // One scroll position per tab, kept while switching tabs and coming back from detail pages.
    val homeList = rememberLazyListState()
    val songsList = rememberLazyListState()
    val albumsGrid = rememberLazyGridState()
    val artistsList = rememberLazyListState()
    val playlistsList = rememberLazyListState()

    val artistArt = remember(index) {
        val map = HashMap<Int, Uri>()
        for (t in index.tracks) {
            val key = t.artistKey
            if (!map.containsKey(key)) map[key] = t.artworkUri
        }
        map
    }

    Column(
        Modifier
            .fillMaxSize()
            .appBackground(),
    ) {
        MainTopBar(onSearch = onSearch, onHub = onHub, onSettings = onSettings)
        if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth())
        Box(Modifier.weight(1f)) {
            if (tab != 4 && index.tracks.isEmpty()) {
                EmptyState(
                    title = if (scanning) "Scanning your music…" else "No music found",
                    message = if (scanning) "This only takes a moment." else "Songs shorter than 30 seconds are skipped. Use Rescan library in Settings after adding music.",
                )
            } else when (tab) {
                0 -> HomeTab(
                    index = index,
                    settings = settings,
                    state = state,
                    player = player,
                    recentIds = recentIds,
                    topTracks = topTracks,
                    favoriteIds = favoriteIds,
                    playlists = playlists,
                    artistArt = artistArt,
                    menu = menu,
                    listState = homeList,
                    onAlbum = onAlbum,
                    onArtist = onArtist,
                    onPlaylist = onPlaylist,
                    onSeeAll = onTab,
                    onSmart = onSmart,
                )
                1 -> SongsTab(index.tracks, sort, onSort, state, player, menu, songsList)
                2 -> AlbumsTab(index.albums, onAlbum, albumsGrid)
                3 -> ArtistsTab(index.artists, artistArt, onArtist, artistsList)
                else -> PlaylistsTab(index, favoriteIds.size, playlists, onPlaylist, onNewPlaylist, playlistsList)
            }
        }
    }
}

@Composable
private fun TabTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun SongsTab(
    tracks: List<Track>,
    sort: SongSort,
    onSort: (SongSort) -> Unit,
    state: PlayerState,
    player: PlayerController,
    menu: TrackMenu,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val sorted = remember(tracks, sort) { tracks.applySort(sort) }
    var sortMenu by remember { mutableStateOf(false) }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomInset.current + 16.dp),
    ) {
        item(key = "title") { TabTitle("Songs") }
        item(key = "actions") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(onClick = { player.play(sorted, 0) }, modifier = Modifier.height(44.dp)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play")
                }
                FilledTonalButton(onClick = { player.shuffleAll(sorted) }, modifier = Modifier.height(44.dp)) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Shuffle")
                }
                Spacer(Modifier.weight(1f))
                Box {
                    TextButton(onClick = { sortMenu = true }) { Text(sort.label) }
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
        }
        item(key = "count") {
            Text(
                formatCount(sorted.size, "song"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, top = 6.dp, bottom = 4.dp),
            )
        }
        itemsIndexed(sorted, key = { _, t -> t.id }) { i, track ->
            TrackRow(
                track = track,
                isCurrent = state.currentId == track.id.toString(),
                onClick = { player.play(sorted, i) },
                onPlayNext = { player.playNext(track) },
                onAddToQueue = { player.addToQueue(track) },
                menu = menu,
            )
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<Album>,
    onAlbum: (Long) -> Unit,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = LocalBottomInset.current + 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "title", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            TabTitle("Albums")
        }
        gridItems(albums, key = { it.id }) { album ->
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onAlbum(album.id) },
            ) {
                Artwork(
                    album.artworkUri,
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    corner = 16.dp,
                    iconSize = 48.dp,
                )
                Text(
                    album.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
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
private fun ArtistsTab(
    artists: List<Artist>,
    artistArt: Map<Int, Uri>,
    onArtist: (Int) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomInset.current + 16.dp),
    ) {
        item(key = "title") { TabTitle("Artists") }
        items(artists, key = { it.key }) { artist ->
            ListRow(
                title = artist.name,
                subtitle = formatCount(artist.albumCount, "album") + " · " + formatCount(artist.trackCount, "song"),
                onClick = { onArtist(artist.key) },
            ) { Artwork(artistArt[artist.key], Modifier.fillMaxSize(), corner = 0.dp, iconSize = 24.dp) }
        }
    }
}

@Composable
private fun PlaylistsTab(
    index: LibraryIndex,
    favoriteCount: Int,
    playlists: List<Playlist>,
    onPlaylist: (Long) -> Unit,
    onNewPlaylist: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val lists = remember(playlists) { playlists.filter { it.kind == KIND_PLAYLIST } }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomInset.current + 16.dp),
    ) {
        item(key = "title") { TabTitle("Playlists") }
        item(key = "new") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNewPlaylist)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("New playlist", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        item(key = "favorites") {
            ListRow(
                title = "Favourites",
                subtitle = formatCount(favoriteCount, "song"),
                onClick = { onPlaylist(FAVORITES_ID) },
            ) { Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        }
        items(lists, key = { "p" + it.id }) { playlist ->
            ListRow(
                title = playlist.name,
                subtitle = formatCount(playlist.trackIds.size, "song"),
                onClick = { onPlaylist(playlist.id) },
            ) { Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
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
