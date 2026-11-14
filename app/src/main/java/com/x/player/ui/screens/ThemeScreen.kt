package com.x.player.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.x.player.data.AppSettings
import com.x.player.data.ThemeMode
import com.x.player.ui.components.GlassSurface
import com.x.player.ui.components.ScreenFrame
import com.x.player.ui.components.SectionLabel
import com.x.player.ui.theme.ACCENTS
import com.x.player.ui.theme.onColorFor

private enum class ColourSource { ART, WALLPAPER, CUSTOM }

@Composable
fun ThemeScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    val wallpaperOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val source = when {
        settings.dynamicColor && wallpaperOk -> ColourSource.WALLPAPER
        settings.adaptToArt -> ColourSource.ART
        else -> ColourSource.CUSTOM
    }

    ScreenFrame(title = "Theme", onBack = onBack) {
        SectionLabel("Mode")
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.values().forEach { mode ->
                FilterChip(
                    selected = settings.themeMode == mode,
                    onClick = { onChange { it.copy(themeMode = mode) } },
                    label = { Text(mode.label) },
                )
            }
        }

        SectionLabel("Colours")
        SourceRow(
            title = "Follow the song",
            subtitle = "The app takes the colour of the artwork that is playing",
            selected = source == ColourSource.ART,
            enabled = true,
        ) { onChange { it.copy(adaptToArt = true, dynamicColor = false) } }
        SourceRow(
            title = "Wallpaper colours",
            subtitle = if (wallpaperOk) "Material You colours from your wallpaper" else "Needs Android 12 or newer",
            selected = source == ColourSource.WALLPAPER,
            enabled = wallpaperOk,
        ) { onChange { it.copy(adaptToArt = false, dynamicColor = true) } }
        SourceRow(
            title = "Pick an accent",
            subtitle = "One fixed colour, always",
            selected = source == ColourSource.CUSTOM,
            enabled = true,
        ) { onChange { it.copy(adaptToArt = false, dynamicColor = false) } }

        if (source == ColourSource.CUSTOM) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ACCENTS.forEachIndexed { i, accent ->
                    val chosen = settings.accentIndex == i
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accent.color)
                            .then(if (chosen) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { onChange { it.copy(accentIndex = i) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (chosen) Icon(Icons.Rounded.Check, contentDescription = accent.name, tint = onColorFor(accent.color))
                    }
                }
            }
        }

        SectionLabel("Liquid glass")
        val cardShape = RoundedCornerShape(22.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f), cardShape),
        ) {
            SwitchRow(
                icon = Icons.Rounded.BlurOn,
                title = "Liquid glass look",
                subtitle = "Translucent bars and buttons with a shiny edge",
                checked = settings.glass,
                onChecked = { v -> onChange { it.copy(glass = v) } },
            )
            // A bright backdrop so the see-through effect is easy to judge.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, Color(0xFFFF6FB5)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                GlassSurface(Modifier.width(210.dp).height(52.dp), RoundedCornerShape(26.dp)) {
                    Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                        Text("Preview", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SourceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = Modifier.padding(horizontal = 8.dp))
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
