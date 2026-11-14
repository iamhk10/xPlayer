package com.x.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x.player.data.AppSettings
import com.x.player.ui.components.ScreenFrame
import com.x.player.ui.components.SectionLabel
import com.x.player.util.formatCount

@Composable
fun SettingsScreen(
    settings: AppSettings,
    trackCount: Int,
    scanning: Boolean,
    speedLabel: String,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
    onTheme: () -> Unit,
    onStats: () -> Unit,
    onEqualizer: () -> Unit,
    onSleep: () -> Unit,
    onSpeed: () -> Unit,
    onDuplicates: () -> Unit,
    onRescan: () -> Unit,
    onAbout: () -> Unit,
) {
    ScreenFrame(title = "Settings", onBack = onBack) {
        SectionLabel("Appearance")
        Card {
            NavRow(Icons.Rounded.Palette, "Theme, colours & fonts", settings.themeMode.label + " · " + settings.fontOption.label + " font", onTheme)
            SwitchRow(
                icon = Icons.Rounded.BlurOn,
                title = "Liquid glass",
                subtitle = "Translucent nav bar, mini player and buttons",
                checked = settings.glass,
                onChecked = { v -> onChange { it.copy(glass = v) } },
            )
        }

        SectionLabel("Start screen")
        Card {
            Text(
                "Open the app on",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MAIN_TABS.forEachIndexed { i, tab ->
                    FilterChip(
                        selected = settings.startTab == i,
                        onClick = { onChange { it.copy(startTab = i) } },
                        label = { Text(tab.label) },
                    )
                }
            }
            SwitchRow(
                icon = null,
                title = "Keep screen on in Now Playing",
                subtitle = "Handy for lyrics",
                checked = settings.keepScreenOn,
                onChecked = { v -> onChange { it.copy(keepScreenOn = v) } },
            )
        }

        SectionLabel("Home sections")
        Card {
            SwitchRow(null, "Recently played", null, settings.showRecent) { v -> onChange { it.copy(showRecent = v) } }
            SwitchRow(null, "Most played", null, settings.showMostPlayed) { v -> onChange { it.copy(showMostPlayed = v) } }
            SwitchRow(null, "Favourites", null, settings.showFavorites) { v -> onChange { it.copy(showFavorites = v) } }
            SwitchRow(null, "Playlists", null, settings.showPlaylists) { v -> onChange { it.copy(showPlaylists = v) } }
            SwitchRow(null, "Albums", null, settings.showAlbums) { v -> onChange { it.copy(showAlbums = v) } }
            SwitchRow(null, "Artists", null, settings.showArtists) { v -> onChange { it.copy(showArtists = v) } }
        }

        SectionLabel("Library")
        Card {
            NavRow(
                Icons.Rounded.Refresh,
                "Rescan library",
                if (scanning) "Scanning…" else formatCount(trackCount, "song") + " found",
                onRescan,
            )
            NavRow(Icons.Rounded.ContentCopy, "Find duplicates", "Spot and delete repeated songs", onDuplicates)
        }

        SectionLabel("Playback and tools")
        Card {
            NavRow(Icons.Rounded.Equalizer, "Equalizer", "5-band with presets", onEqualizer)
            NavRow(Icons.Rounded.Timer, "Sleep timer", "Stop the music after a while", onSleep)
            NavRow(Icons.Rounded.Speed, "Playback speed", speedLabel, onSpeed)
            NavRow(Icons.Rounded.BarChart, "Listening stats", "How much you listened", onStats)
        }

        SectionLabel("About")
        Card {
            NavRow(Icons.Rounded.Info, "About xPlayer", "Version and developer", onAbout)
        }
    }
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f), shape),
        content = content,
    )
}

@Composable
private fun IconBubble(icon: ImageVector) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RowTexts(title: String, subtitle: String?, modifier: Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon)
        Spacer(Modifier.width(12.dp))
        RowTexts(title, subtitle, Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SwitchRow(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconBubble(icon)
            Spacer(Modifier.width(12.dp))
        }
        RowTexts(title, subtitle, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
