package com.x.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.x.player.data.db.StatsRow
import com.x.player.data.db.TopRow
import com.x.player.domain.LibraryIndex
import com.x.player.ui.XViewModel
import com.x.player.util.StatsWindows
import com.x.player.util.formatCount
import com.x.player.util.formatListening

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(vm: XViewModel, index: LibraryIndex, onBack: () -> Unit) {
    val windows = remember { StatsWindows.now() }
    val empty = StatsRow(0, 0L)
    val day by remember { vm.statsSince(windows.day) }.collectAsState(initial = empty)
    val week by remember { vm.statsSince(windows.week) }.collectAsState(initial = empty)
    val month by remember { vm.statsSince(windows.month) }.collectAsState(initial = empty)
    val year by remember { vm.statsSince(windows.year) }.collectAsState(initial = empty)
    val all by remember { vm.statsSince(0L) }.collectAsState(initial = empty)
    val top by vm.topTracks.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Listening stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "day") { StatCard("Today", day) }
            item(key = "week") { StatCard("This week", week) }
            item(key = "month") { StatCard("This month", month) }
            item(key = "year") { StatCard("This year", year) }
            item(key = "all") { StatCard("All time", all) }
            item(key = "note") {
                Text(
                    "A song counts as played after 30 seconds of listening. Time is real listening time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (top.isNotEmpty()) {
                item(key = "top-h") {
                    Text("Most played", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                }
                items(top, key = { "t" + it.trackId }) { row -> TopRowItem(row, index) }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, row: StatsRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text(formatCount(row.plays, "song") + " played", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                formatListening(row.ms),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TopRowItem(row: TopRow, index: LibraryIndex) {
    val track = index.tracksFor(listOf(row.trackId)).firstOrNull()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                track?.title ?: "Removed song",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                track?.artist ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            formatCount(row.plays, "play"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
