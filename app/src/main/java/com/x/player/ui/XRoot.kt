package com.x.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.x.player.XApp
import com.x.player.domain.FAVORITES_ID
import com.x.player.domain.Track
import com.x.player.domain.findDuplicates
import com.x.player.ui.components.AddToPlaylistDialog
import com.x.player.ui.components.ConfirmDialog
import com.x.player.ui.components.NameDialog
import com.x.player.ui.components.TrackMenu
import com.x.player.ui.components.MiniPlayer
import com.x.player.ui.screens.DetailScreen
import com.x.player.ui.screens.DuplicatesScreen
import com.x.player.ui.screens.EqualizerScreen
import com.x.player.ui.screens.HomeScreen
import com.x.player.ui.screens.NowPlayingScreen
import com.x.player.ui.screens.QueueScreen
import com.x.player.ui.screens.SearchScreen
import com.x.player.util.formatCount
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign

private class PendingNew(val track: Track?)

private val AUDIO_PERMISSION =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun XRoot() {
    val context = LocalContext.current
    val container = (context.applicationContext as XApp).container
    val vm: XViewModel = viewModel(factory = XViewModel.Factory(container.library))
    val player = container.player

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, AUDIO_PERMISSION) == PackageManager.PERMISSION_GRANTED)
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        vm.rescan()
    }
    val deleteTracks: (List<Track>) -> Unit = { tracks ->
        if (tracks.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= 30) {
                val request = MediaStore.createDeleteRequest(context.contentResolver, tracks.map { it.uri })
                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            } else {
                tracks.forEach {
                    try {
                        context.contentResolver.delete(it.uri, null, null)
                    } catch (e: Exception) {
                        // Not allowed to delete this file; leave it.
                    }
                }
                vm.rescan()
            }
        }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(granted) {
        if (granted) {
            player.connect()
            vm.rescan()
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (!granted) {
            Box(Modifier.weight(1f)) {
                PermissionScreen(onGrant = { audioLauncher.launch(AUDIO_PERMISSION) })
            }
        } else {
            val nav = rememberNavController()
            val index by vm.index.collectAsState()
            val sort by vm.sort.collectAsState()
            val scanning by vm.scanning.collectAsState()
            val query by vm.query.collectAsState()
            val results by vm.results.collectAsState()
            val favoriteIds by vm.favoriteIds.collectAsState()
            val playlists by vm.playlists.collectAsState()
            val state by player.state.collectAsState()
            val route = nav.currentBackStackEntryAsState().value?.destination?.route

            var addTarget by remember { mutableStateOf<Track?>(null) }
            var pendingNew by remember { mutableStateOf<PendingNew?>(null) }
            val favoriteSet = remember(favoriteIds) { favoriteIds.toSet() }
            val menu = remember(favoriteSet) {
                TrackMenu(
                    favorites = favoriteSet,
                    onToggleFavorite = { vm.toggleFavorite(it.id) },
                    onAddToPlaylist = { addTarget = it },
                )
            }

            Box(Modifier.weight(1f)) {
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            index = index,
                            sort = sort,
                            scanning = scanning,
                            state = state,
                            player = player,
                            onSort = vm::setSort,
                            onRescan = vm::rescan,
                            onSearch = { nav.navigate("search") },
                            onAlbum = { nav.navigate("album/$it") },
                            onArtist = { nav.navigate("artist/$it") },
                            onFolder = { nav.navigate("folder/$it") },
                            favoriteCount = index.tracksFor(favoriteIds).size,
                            playlists = playlists,
                            menu = menu,
                            onPlaylist = { nav.navigate("playlist/$it") },
                            onNewPlaylist = { pendingNew = PendingNew(null) },
                            onDuplicates = { nav.navigate("duplicates") },
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            query = query,
                            results = results,
                            state = state,
                            player = player,
                            menu = menu,
                            onQuery = vm::setQuery,
                            onBack = { vm.setQuery(""); nav.popBackStack() },
                            onAlbum = { nav.navigate("album/$it") },
                            onArtist = { nav.navigate("artist/$it") },
                        )
                    }
                    composable("album/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                        val id = entry.arguments?.getLong("id") ?: 0L
                        val album = index.album(id)
                        DetailScreen(
                            title = album?.name ?: "Album",
                            subtitle = (album?.artist ?: "") + " · " + formatCount(album?.trackCount ?: 0, "song"),
                            artworkUri = album?.artworkUri,
                            tracks = index.albumTracks(id),
                            state = state,
                            player = player,
                            menu = menu,
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable("artist/{key}", arguments = listOf(navArgument("key") { type = NavType.IntType })) { entry ->
                        val key = entry.arguments?.getInt("key") ?: 0
                        val artist = index.artist(key)
                        DetailScreen(
                            title = artist?.name ?: "Artist",
                            subtitle = formatCount(artist?.albumCount ?: 0, "album") + " · " + formatCount(artist?.trackCount ?: 0, "song"),
                            artworkUri = null,
                            tracks = index.artistTracks(key),
                            state = state,
                            player = player,
                            menu = menu,
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable("folder/{key}", arguments = listOf(navArgument("key") { type = NavType.IntType })) { entry ->
                        val key = entry.arguments?.getInt("key") ?: 0
                        val folder = index.folder(key)
                        DetailScreen(
                            title = folder?.name ?: "Folder",
                            subtitle = folder?.path ?: "",
                            artworkUri = null,
                            tracks = index.folderTracks(key),
                            state = state,
                            player = player,
                            menu = menu,
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable("playlist/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                        val id = entry.arguments?.getLong("id") ?: 0L
                        if (id == FAVORITES_ID) {
                            val tracks = index.tracksFor(favoriteIds)
                            DetailScreen(
                                title = "Favourites",
                                subtitle = formatCount(tracks.size, "song"),
                                artworkUri = null,
                                tracks = tracks,
                                state = state,
                                player = player,
                                menu = menu,
                                onBack = { nav.popBackStack() },
                                onRemove = { vm.toggleFavorite(it.id) },
                            )
                        } else {
                            val playlist = playlists.firstOrNull { it.id == id }
                            val tracks = index.tracksFor(playlist?.trackIds.orEmpty())
                            var renaming by remember { mutableStateOf(false) }
                            var deleting by remember { mutableStateOf(false) }
                            DetailScreen(
                                title = playlist?.name ?: "Playlist",
                                subtitle = formatCount(tracks.size, "song"),
                                artworkUri = null,
                                tracks = tracks,
                                state = state,
                                player = player,
                                menu = menu,
                                onBack = { nav.popBackStack() },
                                onRemove = { vm.removeFromPlaylist(id, it.id) },
                                topActions = {
                                    IconButton(onClick = { renaming = true }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Rename")
                                    }
                                    IconButton(onClick = { deleting = true }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                                    }
                                },
                            )
                            if (renaming) {
                                NameDialog(
                                    title = "Rename playlist",
                                    confirmLabel = "Save",
                                    initial = playlist?.name.orEmpty(),
                                    onConfirm = { vm.renamePlaylist(id, it); renaming = false },
                                    onDismiss = { renaming = false },
                                )
                            }
                            if (deleting) {
                                ConfirmDialog(
                                    title = "Delete playlist?",
                                    message = "The songs themselves stay on your phone.",
                                    confirmLabel = "Delete",
                                    onConfirm = { deleting = false; vm.deletePlaylist(id); nav.popBackStack() },
                                    onDismiss = { deleting = false },
                                )
                            }
                        }
                    }
                    composable(
                        "now",
                        enterTransition = { slideInVertically { it } },
                        exitTransition = { slideOutVertically { it } },
                        popEnterTransition = { slideInVertically { it } },
                        popExitTransition = { slideOutVertically { it } },
                    ) {
                        NowPlayingScreen(
                            state = state,
                            player = player,
                            isFavorite = state.currentId?.toLongOrNull()?.let { it in favoriteSet } ?: false,
                            onToggleFavorite = { state.currentId?.toLongOrNull()?.let(vm::toggleFavorite) },
                            onBack = { nav.popBackStack() },
                            onQueue = { nav.navigate("queue") },
                            onEqualizer = { nav.navigate("eq") },
                            lyrics = container.lyrics,
                        )
                    }
                    composable("duplicates") {
                        val groups = remember(index) { findDuplicates(index.tracks) }
                        DuplicatesScreen(groups = groups, onDelete = deleteTracks, onBack = { nav.popBackStack() })
                    }
                    composable("eq") {
                        EqualizerScreen(controller = container.equalizer, onBack = { nav.popBackStack() })
                    }
                    composable("queue") {
                        QueueScreen(state = state, player = player, onBack = { nav.popBackStack() })
                    }
                }
            }

            addTarget?.let { track ->
                AddToPlaylistDialog(
                    playlists = playlists,
                    onPick = { vm.addToPlaylist(it.id, track.id); addTarget = null },
                    onCreate = { pendingNew = PendingNew(track); addTarget = null },
                    onDismiss = { addTarget = null },
                )
            }
            pendingNew?.let { request ->
                NameDialog(
                    title = "New playlist",
                    confirmLabel = "Create",
                    onConfirm = { vm.createPlaylist(it, request.track?.id); pendingNew = null },
                    onDismiss = { pendingNew = null },
                )
            }

            if (state.hasMedia && route != "now") {
                MiniPlayer(
                    state = state,
                    onOpen = { nav.navigate("now") { launchSingleTop = true } },
                    onToggle = player::togglePlayPause,
                    onNext = player::next,
                )
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text("X", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.padding(top = 12.dp))
        Text(
            "X plays the music already on your phone. It needs permission to read your audio files. Nothing ever leaves your device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.padding(top = 24.dp))
        Button(onClick = onGrant) { Text("Allow access to music") }
    }
}
