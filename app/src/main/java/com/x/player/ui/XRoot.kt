package com.x.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.Button
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
import com.x.player.ui.components.EmptyState
import com.x.player.ui.components.MiniPlayer
import com.x.player.ui.screens.DetailScreen
import com.x.player.ui.screens.HomeScreen
import com.x.player.ui.screens.NowPlayingScreen
import com.x.player.ui.screens.QueueScreen
import com.x.player.ui.screens.SearchScreen
import com.x.player.util.formatCount
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign

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
            val state by player.state.collectAsState()
            val route = nav.currentBackStackEntryAsState().value?.destination?.route

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
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            query = query,
                            results = results,
                            state = state,
                            player = player,
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
                            onBack = { nav.popBackStack() },
                        )
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
                            onBack = { nav.popBackStack() },
                            onQueue = { nav.navigate("queue") },
                        )
                    }
                    composable("queue") {
                        QueueScreen(state = state, player = player, onBack = { nav.popBackStack() })
                    }
                }
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
