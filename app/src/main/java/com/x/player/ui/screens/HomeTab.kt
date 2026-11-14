package com.x.player.ui.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.x.player.data.AppSettings
import com.x.player.data.db.TopRow
import com.x.player.domain.FAVORITES_ID
import com.x.player.domain.KIND_PLAYLIST
import com.x.player.domain.LibraryIndex
import com.x.player.domain.Playlist
import com.x.player.domain.Track
import com.x.player.service.PlayerController
import com.x.player.service.PlayerState
import com.x.player.ui.components.Artwork
import com.x.player.ui.components.TrackMenu
import com.x.player.ui.components.TrackRow
import com.x.player.ui.theme.LocalBottomInset
import com.x.player.util.formatCount

/** "Listen Now": featured carousel plus horizontal shelves. Sections can be switched off in Settings. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTab(
    index: LibraryIndex,
    settings: AppSettings,
    state: PlayerState,
    player: PlayerController,
    recentIds: List<Long>,
    topTracks: List<TopRow>,
    favoriteIds: List<Long>,
    playlists: List<Playlist>,
    artistArt: Map<Int, Uri>,
    menu: TrackMenu,
    listState: LazyListState,
    onAlbum: (Long) -> Unit,
    onArtist: (Int) -> Unit,
    onPlaylist: (Long) -> Unit,
    onSeeAll: (Int) -> Unit,
    onSmart: (String) -> Unit,
) {
    val hero = remember(index) {
        index.tracks.sortedByDescending { it.dateAdded }.distinctBy { it.albumId }.take(8)
    }
    val recent = remember(index, recentIds) { index.tracksFor(recentIds).take(12) }
    val most = remember(index, topTracks) {
        topTracks.mapNotNull { row -> index.tracksFor(listOf(row.trackId)).firstOrNull() }.take(5)
    }
    val favorites = remember(index, favoriteIds) { index.tracksFor(favoriteIds) }
    val lists = remember(playlists) { playlists.filter { it.kind == KIND_PLAYLIST } }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomInset.current + 16.dp),
    ) {
        item(key = "title") {
            Text(
                "Listen Now",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
            )
        }

        if (hero.isNotEmpty()) {
            item(key = "hero") {
                val rowState = rememberLazyListState()
                LazyRow(
                    state = rowState,
                    flingBehavior = rememberSnapFlingBehavior(rowState),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(hero, key = { _, t -> "hero" + t.id }) { i, track ->
                        HeroCard(track) { player.play(hero, i) }
                    }
                }
            }
            item(key = "quick") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        onClick = { player.shuffleAll(index.tracks) },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Shuffle all")
                    }
                    FilledTonalButton(
                        onClick = { onPlaylist(FAVORITES_ID) },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Favourites")
                    }
                }
            }
        }

        if (settings.showRecent && recent.isNotEmpty()) {
            item(key = "h-recent") { SectionHeader("Recently played") { onSmart("recent") } }
            item(key = "r-recent") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(recent, key = { _, t -> "rec" + t.id }) { i, track ->
                        SquareCard(track.artworkUri, track.title, track.artist) { player.play(recent, i) }
                    }
                }
            }
        }

        if (settings.showMostPlayed && most.isNotEmpty()) {
            item(key = "h-most") { SectionHeader("Most played") { onSmart("most") } }
            itemsIndexed(most, key = { _, t -> "most" + t.id }) { i, track ->
                TrackRow(
                    track = track,
                    isCurrent = state.currentId == track.id.toString(),
                    onClick = { player.play(most, i) },
                    onPlayNext = { player.playNext(track) },
                    onAddToQueue = { player.addToQueue(track) },
                    menu = menu,
                    card = true,
                )
            }
        }

        if (settings.showFavorites && favorites.isNotEmpty()) {
            item(key = "h-fav") { SectionHeader("Favourites") { onPlaylist(FAVORITES_ID) } }
            item(key = "r-fav") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(favorites.take(12), key = { _, t -> "fav" + t.id }) { i, track ->
                        SquareCard(track.artworkUri, track.title, track.artist) { player.play(favorites, i) }
                    }
                }
            }
        }

        if (settings.showPlaylists && lists.isNotEmpty()) {
            item(key = "h-lists") { SectionHeader("Playlists") { onSeeAll(4) } }
            item(key = "r-lists") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(lists.take(12), key = { "pl" + it.id }) { list ->
                        val art = remember(index, list) { index.tracksFor(list.trackIds).firstOrNull()?.artworkUri }
                        SquareCard(art, list.name, formatCount(list.trackIds.size, "song")) { onPlaylist(list.id) }
                    }
                }
            }
        }

        if (settings.showAlbums && index.albums.isNotEmpty()) {
            item(key = "h-albums") { SectionHeader("Albums") { onSeeAll(2) } }
            item(key = "r-albums") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(index.albums.take(12), key = { "al" + it.id }) { album ->
                        SquareCard(album.artworkUri, album.name, album.artist) { onAlbum(album.id) }
                    }
                }
            }
        }

        if (settings.showArtists && index.artists.isNotEmpty()) {
            item(key = "h-artists") { SectionHeader("Artists") { onSeeAll(3) } }
            item(key = "r-artists") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(index.artists.take(12), key = { "ar" + it.key }) { artist ->
                        CircleCard(artistArt[artist.key], artist.name) { onArtist(artist.key) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) { Text("See all") }
        }
    }
}

@Composable
private fun HeroCard(track: Track, onClick: () -> Unit) {
    Box(
        Modifier
            .width(300.dp)
            .height(190.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
    ) {
        Artwork(track.artworkUri, Modifier.fillMaxSize(), corner = 0.dp, iconSize = 64.dp)
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.8f))),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 64.dp, bottom = 14.dp),
        ) {
            Text(
                track.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track.artist,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color(0xFF14101F))
        }
    }
}

@Composable
fun SquareCard(art: Uri?, title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Artwork(art, Modifier.size(140.dp), corner = 16.dp, iconSize = 44.dp)
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CircleCard(art: Uri?, name: String, onClick: () -> Unit) {
    Column(
        Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Artwork(art, Modifier.size(104.dp), corner = 52.dp, iconSize = 36.dp)
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
