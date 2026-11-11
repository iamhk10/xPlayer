package com.x.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.x.player.domain.SearchResults
import com.x.player.service.PlayerController
import com.x.player.service.PlayerState
import com.x.player.ui.components.Artwork
import com.x.player.ui.components.EmptyState
import com.x.player.ui.components.TrackMenu
import com.x.player.ui.components.TrackRow
import com.x.player.util.formatCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    results: SearchResults,
    state: PlayerState,
    player: PlayerController,
    menu: TrackMenu,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onAlbum: (Long) -> Unit,
    onArtist: (Int) -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (e: Exception) { } }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    TextField(
                        value = query,
                        onValueChange = onQuery,
                        placeholder = { Text("Songs, albums, artists") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedIndicatorColor = MaterialTheme.colorScheme.background,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.background,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                query.isBlank() -> EmptyState("Search your library", "Type a song, album or artist name.")
                results.isEmpty -> EmptyState("No results", "Nothing matches \"" + query.trim() + "\".")
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    if (results.artists.isNotEmpty()) {
                        item(key = "h-artists") { Header("Artists") }
                        items(results.artists, key = { "ar" + it.key }) { artist ->
                            ListRow(
                                title = artist.name,
                                subtitle = formatCount(artist.trackCount, "song"),
                                onClick = { onArtist(artist.key) },
                            ) { Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                    if (results.albums.isNotEmpty()) {
                        item(key = "h-albums") { Header("Albums") }
                        items(results.albums, key = { "al" + it.id }) { album ->
                            AlbumRow(album.name, album.artist, album.artworkUri) { onAlbum(album.id) }
                        }
                    }
                    if (results.songs.isNotEmpty()) {
                        item(key = "h-songs") { Header("Songs") }
                        itemsIndexed(results.songs, key = { _, t -> "s" + t.id }) { i, track ->
                            TrackRow(
                                track = track,
                                isCurrent = state.currentId == track.id.toString(),
                                onClick = { player.play(results.songs, i) },
                                onPlayNext = { player.playNext(track) },
                                onAddToQueue = { player.addToQueue(track) },
                                menu = menu,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun AlbumRow(name: String, artist: String, art: android.net.Uri, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(art, Modifier.padding(end = 12.dp).size(48.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

