package com.x.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.x.player.XApp
import com.x.player.di.AppContainer
import com.x.player.domain.FAVORITES_ID
import com.x.player.domain.KIND_FOLDER
import com.x.player.domain.KIND_PLAYLIST
import com.x.player.domain.Track
import com.x.player.domain.findDuplicates
import com.x.player.ui.components.AddToPlaylistDialog
import com.x.player.ui.components.ConfirmDialog
import com.x.player.ui.components.FloatingNavBar
import com.x.player.ui.components.MINI_PLAYER_HEIGHT
import com.x.player.ui.components.MiniPlayer
import com.x.player.ui.components.NAV_BAR_HEIGHT
import com.x.player.ui.components.NameDialog
import com.x.player.ui.components.SleepTimerDialog
import com.x.player.ui.components.TrackMenu
import com.x.player.ui.components.rememberArtworkColor
import com.x.player.ui.screens.AboutScreen
import com.x.player.ui.screens.DetailScreen
import com.x.player.ui.screens.DeviceFoldersScreen
import com.x.player.ui.screens.DuplicatesScreen
import com.x.player.ui.screens.EqualizerScreen
import com.x.player.ui.screens.HubScreen
import com.x.player.ui.screens.MAIN_TABS
import com.x.player.ui.screens.MainScreen
import com.x.player.ui.screens.NowPlayingScreen
import com.x.player.ui.screens.QueueScreen
import com.x.player.ui.screens.SearchScreen
import com.x.player.ui.screens.SettingsScreen
import com.x.player.ui.screens.StatsScreen
import com.x.player.ui.screens.ThemeScreen
import com.x.player.ui.theme.LocalBottomInset
import com.x.player.ui.theme.XTheme
import com.x.player.util.formatCount
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private class PendingNew(val track: Track?, val kind: Int)

private val AUDIO_PERMISSION =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun XRoot() {
    val container = (LocalContext.current.applicationContext as XApp).container
    val settings by container.settings.settings.collectAsState()

    // The app takes its colour from the artwork that is playing. Keep the last colour while the next
    // one loads, so the accent glides from song to song instead of flashing back to the default.
    val artUri by remember { container.player.state.map { it.artworkUri }.distinctUntilChanged() }
        .collectAsState(initial = null)
    val artColor = rememberArtworkColor(if (settings.adaptToArt) artUri else null)
    var lastArt by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(artColor) { if (artColor != null) lastArt = artColor }

    XTheme(settings = settings, artColor = lastArt) {
        XContent(container)
    }
}

@Composable
private fun XContent(container: AppContainer) {
    val context = LocalContext.current
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

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (!granted) {
            PermissionScreen(onGrant = { audioLauncher.launch(AUDIO_PERMISSION) })
        } else {
            AppHost(container, vm, deleteTracks)
        }
    }
}

@Composable
private fun AppHost(
    container: AppContainer,
    vm: XViewModel,
    deleteTracks: (List<Track>) -> Unit,
) {
    val player = container.player
    val nav = rememberNavController()
    val index by vm.index.collectAsState()
    val sort by vm.sort.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()
    val favoriteIds by vm.favoriteIds.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val recentIds by vm.recentIds.collectAsState()
    val topTracks by vm.topTracks.collectAsState()
    val settings by container.settings.settings.collectAsState()
    val state by player.state.collectAsState()
    val sleep by player.sleep.collectAsState()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    var mainTab by rememberSaveable { mutableIntStateOf(0) }
    var addTarget by remember { mutableStateOf<Track?>(null) }
    var pendingNew by remember { mutableStateOf<PendingNew?>(null) }
    var showSleep by remember { mutableStateOf(false) }

    val favoriteSet = remember(favoriteIds) { favoriteIds.toSet() }
    val menu = remember(favoriteSet) {
        TrackMenu(
            favorites = favoriteSet,
            onToggleFavorite = { vm.toggleFavorite(it.id) },
            onAddToPlaylist = { addTarget = it },
        )
    }
    val folders = remember(playlists) { playlists.filter { it.kind == KIND_FOLDER } }

    val onMain = route == "main"
    val showMini = state.hasMedia && route != "now" && route != "queue"
    BackHandler(enabled = onMain && mainTab != 0) { mainTab = 0 }

    // Room the floating nav bar and mini player take at the bottom, so lists can scroll clear of them.
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val overlayHeight = (if (showMini) MINI_PLAYER_HEIGHT else 0.dp) +
        (if (onMain) NAV_BAR_HEIGHT else 0.dp) +
        (if (showMini && onMain) 8.dp else 0.dp)
    val bottomInset = if (overlayHeight > 0.dp) navBarInset + 8.dp + overlayHeight + 8.dp else navBarInset

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomInset provides bottomInset) {
            NavHost(
                navController = nav,
                startDestination = "main",
                enterTransition = { fadeIn(tween(200)) + slideInHorizontally(tween(260)) { it / 8 } },
                exitTransition = { fadeOut(tween(140)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(240)) { it / 8 } },
            ) {
                composable("main") {
                    MainScreen(
                        tab = mainTab,
                        index = index,
                        sort = sort,
                        scanning = scanning,
                        state = state,
                        player = player,
                        settings = settings,
                        recentIds = recentIds,
                        topTracks = topTracks,
                        favoriteIds = favoriteIds,
                        playlists = playlists,
                        menu = menu,
                        onTab = { mainTab = it },
                        onSort = vm::setSort,
                        onSearch = { nav.navigate("search") },
                        onHub = { nav.navigate("hub") },
                        onSettings = { nav.navigate("settings") },
                        onAlbum = { nav.navigate("album/$it") },
                        onArtist = { nav.navigate("artist/$it") },
                        onPlaylist = { nav.navigate("playlist/$it") },
                        onNewPlaylist = { pendingNew = PendingNew(null, KIND_PLAYLIST) },
                        onSmart = { nav.navigate("smart/$it") },
                    )
                }
                composable("search") {
                    Padded {
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
                }
                composable("album/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    val album = index.album(id)
                    DetailScreen(
                        title = album?.name ?: "Album",
                        subtitle = (album?.artist ?: "") + (if ((album?.year ?: 0) > 0) " · " + album?.year else ""),
                        artworkUri = album?.artworkUri,
                        tracks = index.albumTracks(id),
                        state = state,
                        player = player,
                        menu = menu,
                        onBack = { nav.popBackStack() },
                        showArtwork = false,
                    )
                }
                composable("artist/{key}", arguments = listOf(navArgument("key") { type = NavType.IntType })) { entry ->
                    val key = entry.arguments?.getInt("key") ?: 0
                    val artist = index.artist(key)
                    DetailScreen(
                        title = artist?.name ?: "Artist",
                        subtitle = formatCount(artist?.albumCount ?: 0, "album"),
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
                composable("smart/{type}", arguments = listOf(navArgument("type") { type = NavType.StringType })) { entry ->
                    val type = entry.arguments?.getString("type").orEmpty()
                    val tracks = remember(index, recentIds, topTracks, type) {
                        when (type) {
                            "recent" -> index.tracksFor(recentIds)
                            "most" -> index.tracksFor(topTracks.map { it.trackId })
                            else -> index.tracks.sortedByDescending { it.dateAdded }.take(100)
                        }
                    }
                    DetailScreen(
                        title = when (type) {
                            "recent" -> "Recently played"
                            "most" -> "Most played"
                            else -> "Recently added"
                        },
                        subtitle = when (type) {
                            "recent" -> "What you listened to lately"
                            "most" -> "Songs you play the most"
                            else -> "Newest songs on your phone"
                        },
                        artworkUri = null,
                        tracks = tracks,
                        state = state,
                        player = player,
                        menu = menu,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable("playlist/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments?.getLong("id") ?: 0L
                    if (id == FAVORITES_ID) {
                        val tracks = remember(index, favoriteIds) { index.tracksFor(favoriteIds) }
                        DetailScreen(
                            title = "Favourites",
                            subtitle = "Songs you loved",
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
                        val isFolder = playlist?.kind == KIND_FOLDER
                        val label = if (isFolder) "folder" else "playlist"
                        val tracks = remember(index, playlist) { index.tracksFor(playlist?.trackIds.orEmpty()) }
                        var renaming by remember { mutableStateOf(false) }
                        var deleting by remember { mutableStateOf(false) }
                        DetailScreen(
                            title = playlist?.name ?: if (isFolder) "Folder" else "Playlist",
                            subtitle = if (isFolder) "Your folder" else "Your playlist",
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
                                title = "Rename $label",
                                confirmLabel = "Save",
                                initial = playlist?.name.orEmpty(),
                                onConfirm = { vm.renamePlaylist(id, it); renaming = false },
                                onDismiss = { renaming = false },
                            )
                        }
                        if (deleting) {
                            ConfirmDialog(
                                title = "Delete $label?",
                                message = "The songs themselves stay on your phone.",
                                confirmLabel = "Delete",
                                onConfirm = { deleting = false; vm.deletePlaylist(id); nav.popBackStack() },
                                onDismiss = { deleting = false },
                            )
                        }
                    }
                }
                composable("hub") {
                    HubScreen(
                        favoriteCount = index.tracksFor(favoriteIds).size,
                        deviceFolderCount = index.folders.size,
                        folders = folders,
                        sleepActive = sleep != null,
                        scanning = scanning,
                        onBack = { nav.popBackStack() },
                        onFavorites = { nav.navigate("playlist/$FAVORITES_ID") },
                        onSmart = { nav.navigate("smart/$it") },
                        onDeviceFolders = { nav.navigate("devicefolders") },
                        onOpenFolder = { nav.navigate("playlist/$it") },
                        onNewFolder = { pendingNew = PendingNew(null, KIND_FOLDER) },
                        onNewPlaylist = { pendingNew = PendingNew(null, KIND_PLAYLIST) },
                        onStats = { nav.navigate("stats") },
                        onEqualizer = { nav.navigate("eq") },
                        onSleep = { showSleep = true },
                        onDuplicates = { nav.navigate("duplicates") },
                        onRescan = vm::rescan,
                    )
                }
                composable("devicefolders") {
                    DeviceFoldersScreen(
                        folders = index.folders,
                        onBack = { nav.popBackStack() },
                        onFolder = { nav.navigate("folder/$it") },
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        settings = settings,
                        trackCount = index.tracks.size,
                        scanning = scanning,
                        onChange = container.settings::update,
                        onBack = { nav.popBackStack() },
                        onTheme = { nav.navigate("theme") },
                        onStats = { nav.navigate("stats") },
                        onEqualizer = { nav.navigate("eq") },
                        onSleep = { showSleep = true },
                        onDuplicates = { nav.navigate("duplicates") },
                        onRescan = vm::rescan,
                        onAbout = { nav.navigate("about") },
                    )
                }
                composable("theme") {
                    ThemeScreen(
                        settings = settings,
                        onChange = container.settings::update,
                        onBack = { nav.popBackStack() },
                    )
                }
                composable(
                    "now",
                    enterTransition = { slideInVertically(tween(300)) { it } },
                    exitTransition = { slideOutVertically(tween(260)) { it } },
                    popEnterTransition = { slideInVertically(tween(300)) { it } },
                    popExitTransition = { slideOutVertically(tween(260)) { it } },
                ) {
                    NowPlayingScreen(
                        state = state,
                        player = player,
                        isFavorite = state.currentId?.toLongOrNull()?.let { it in favoriteSet } ?: false,
                        onToggleFavorite = { state.currentId?.toLongOrNull()?.let(vm::toggleFavorite) },
                        onAddToPlaylist = {
                            state.currentId?.toLongOrNull()?.let { id ->
                                index.tracksFor(listOf(id)).firstOrNull()?.let { addTarget = it }
                            }
                        },
                        onBack = { nav.popBackStack() },
                        onQueue = { nav.navigate("queue") },
                        onEqualizer = { nav.navigate("eq") },
                        lyrics = container.lyrics,
                    )
                }
                composable("duplicates") {
                    val groups = remember(index) { findDuplicates(index.tracks) }
                    Padded { DuplicatesScreen(groups = groups, onDelete = deleteTracks, onBack = { nav.popBackStack() }) }
                }
                composable("stats") {
                    Padded { StatsScreen(vm = vm, index = index, onBack = { nav.popBackStack() }) }
                }
                composable("about") {
                    Padded { AboutScreen(onBack = { nav.popBackStack() }) }
                }
                composable("eq") {
                    Padded { EqualizerScreen(controller = container.equalizer, onBack = { nav.popBackStack() }) }
                }
                composable("queue") {
                    Padded { QueueScreen(state = state, player = player, onBack = { nav.popBackStack() }) }
                }
            }
        }

        // Floating mini player and nav bar, drawn over the pages.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = navBarInset + 8.dp),
        ) {
            AnimatedVisibility(
                visible = showMini,
                enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
            ) {
                MiniPlayer(
                    state = state,
                    player = player,
                    onOpen = { nav.navigate("now") { launchSingleTop = true } },
                    modifier = Modifier.padding(bottom = if (onMain) 8.dp else 0.dp),
                )
            }
            AnimatedVisibility(
                visible = onMain,
                enter = slideInVertically(tween(220)) { it } + fadeIn(tween(220)),
                exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(180)),
            ) {
                FloatingNavBar(
                    tabs = MAIN_TABS,
                    selected = mainTab,
                    onSelect = { mainTab = it },
                )
            }
        }
    }

    addTarget?.let { track ->
        AddToPlaylistDialog(
            playlists = playlists,
            onPick = { vm.addToPlaylist(it.id, track.id); addTarget = null },
            onCreate = { pendingNew = PendingNew(track, KIND_PLAYLIST); addTarget = null },
            onDismiss = { addTarget = null },
        )
    }
    pendingNew?.let { request ->
        val label = if (request.kind == KIND_FOLDER) "folder" else "playlist"
        NameDialog(
            title = "New $label",
            confirmLabel = "Create",
            onConfirm = { vm.createPlaylist(it, request.track?.id, request.kind); pendingNew = null },
            onDismiss = { pendingNew = null },
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
}

/** Gives older full-screen pages the room the floating bars take, so nothing hides underneath. */
@Composable
private fun Padded(content: @Composable () -> Unit) {
    val inset = LocalBottomInset.current
    val imeHeight = with(LocalDensity.current) { WindowInsets.ime.getBottom(this).toDp() }
    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = if (imeHeight > inset) imeHeight else inset),
    ) { content() }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("xPlayer", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.padding(top = 12.dp))
        Text(
            "xPlayer plays the music already on your phone. It needs permission to read your audio files. Nothing ever leaves your device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.padding(top = 24.dp))
        Button(onClick = onGrant) { Text("Allow access to music") }
    }
}
