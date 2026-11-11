package com.x.player.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.x.player.domain.Track
import com.x.player.ui.components.ConfirmDialog
import com.x.player.ui.components.EmptyState
import com.x.player.util.formatTime

private fun sizeLabel(bytes: Long): String = "%.1f MB".format(bytes / 1_048_576.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    groups: List<List<Track>>,
    onDelete: (List<Track>) -> Unit,
    onBack: () -> Unit,
) {
    var pending by remember { mutableStateOf<List<Track>?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Duplicates · " + groups.size) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (groups.isEmpty()) {
            EmptyState("No duplicates", "No two songs share the same title, artist and length.", Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            itemsIndexed(groups, key = { _, g -> g.first().id }) { _, group ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        group.first().title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        group.first().artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    group.forEachIndexed { i, track ->
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Row {
                                Text(
                                    if (i == 0) "Keep · " else "Extra · ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (i == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    sizeLabel(track.size) + " · " + formatTime(track.durationMs),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Text(
                                track.path,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { pending = group.drop(1) }) {
                        Text("Delete " + (group.size - 1) + " extra")
                    }
                }
            }
        }
    }

    pending?.let { extras ->
        ConfirmDialog(
            title = "Delete extra copies?",
            message = "Android will ask you once more. The largest file of each song is kept.",
            confirmLabel = "Delete",
            onConfirm = { onDelete(extras); pending = null },
            onDismiss = { pending = null },
        )
    }
}
