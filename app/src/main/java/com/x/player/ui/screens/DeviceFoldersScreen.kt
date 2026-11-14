package com.x.player.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.x.player.domain.Folder
import com.x.player.ui.components.EmptyState
import com.x.player.ui.components.ScreenFrame
import com.x.player.ui.theme.LocalBottomInset
import com.x.player.util.formatCount

/** The real folders on the phone that hold music. */
@Composable
fun DeviceFoldersScreen(
    folders: List<Folder>,
    onBack: () -> Unit,
    onFolder: (Int) -> Unit,
) {
    ScreenFrame(title = "Device folders", onBack = onBack, scroll = false) {
        if (folders.isEmpty()) {
            EmptyState("No folders yet", "Music folders show up here once your library is scanned.")
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = LocalBottomInset.current + 16.dp),
            ) {
                items(folders, key = { it.key }) { folder ->
                    ListRow(
                        title = folder.name,
                        subtitle = formatCount(folder.trackCount, "song") + " · " + folder.path,
                        onClick = { onFolder(folder.key) },
                    ) { Icon(Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}
