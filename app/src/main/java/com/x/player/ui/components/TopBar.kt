package com.x.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.x.player.ui.theme.onColorFor
import kotlinx.coroutines.delay

/** Search pill in the middle, the hub button and the settings button on the right. */
@Composable
fun MainTopBar(
    onSearch: () -> Unit,
    onHub: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchPill(onSearch, Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        HubButton(onHub)
        Spacer(Modifier.width(8.dp))
        CircleIconButton(Icons.Rounded.Settings, "Settings", onSettings)
    }
}

private val HINTS = listOf("xPlayer", "Search songs", "Search artists", "Search albums", "Search playlists")

/** Google-style rounded search bar whose hint rolls through "xPlayer" and what you can search for. */
@Composable
private fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var hint by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2600)
            hint = (hint + 1) % HINTS.size
        }
    }
    val scheme = MaterialTheme.colorScheme
    GlassSurface(modifier.height(48.dp), RoundedCornerShape(24.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = scheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                AnimatedContent(
                    targetState = hint,
                    transitionSpec = {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                    },
                    label = "searchHint",
                ) { i ->
                    Text(
                        HINTS[i],
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (i == 0) scheme.primary else scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            // Brand mark where Google puts its microphone.
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(scheme.primary, scheme.secondary))),
                contentAlignment = Alignment.Center,
            ) {
                Text("x", color = onColorFor(scheme.primary), fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}

/** A squircle with a gradient fill, so it stands apart from the round settings button. */
@Composable
private fun HubButton(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.secondary)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Widgets, contentDescription = "Hub", tint = onColorFor(scheme.primary), modifier = Modifier.size(24.dp))
    }
}
