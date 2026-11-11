package com.x.player.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.x.player.service.EQ_PRESETS
import com.x.player.service.EqualizerController
import com.x.player.ui.components.EmptyState

private fun freqLabel(hz: Int): String =
    if (hz >= 1000) "%.1f kHz".format(hz / 1000f) else "$hz Hz"

private fun dbLabel(mB: Int): String {
    val db = mB / 100f
    return (if (db > 0) "+" else "") + "%.1f dB".format(db)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(controller: EqualizerController, onBack: () -> Unit) {
    val state by controller.state.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Equalizer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.available) {
                        Switch(
                            checked = state.enabled,
                            onCheckedChange = { controller.setEnabled(it) },
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (!state.available) {
            EmptyState(
                "Equalizer not ready",
                "Play a song first, then open the equalizer again. Some devices do not support it.",
                Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
        ) {
            item(key = "presets") {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(EQ_PRESETS, key = { it.name }) { preset ->
                        FilterChip(
                            selected = state.preset == preset.name,
                            onClick = { controller.applyPreset(preset) },
                            label = { Text(preset.name) },
                            enabled = state.enabled,
                        )
                    }
                }
            }
            items(state.levels.size, key = { it }) { i ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(freqLabel(state.freqsHz.getOrElse(i) { 0 }), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            dbLabel(state.levels[i]),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Slider(
                        value = state.levels[i].toFloat(),
                        onValueChange = { controller.setLevel(i, it.toInt()) },
                        valueRange = state.minLevel.toFloat()..state.maxLevel.toFloat(),
                        enabled = state.enabled,
                    )
                }
            }
        }
    }
}
