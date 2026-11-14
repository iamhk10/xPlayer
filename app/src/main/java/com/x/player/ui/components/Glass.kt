package com.x.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x.player.ui.theme.LocalGlass
import com.x.player.ui.theme.LocalIsDark

/**
 * Floating surface used by the nav bar, mini player, search pill and round buttons.
 * Normal mode: a calm, nearly solid card. Liquid glass mode: translucent with a light sheen and a
 * bright edge, so whatever scrolls underneath shows through.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalGlass.current
    val dark = LocalIsDark.current
    val scheme = MaterialTheme.colorScheme
    val base = scheme.surfaceContainerHigh
    val look = if (glass) {
        Modifier
            .background(base.copy(alpha = if (dark) 0.55f else 0.60f), shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.16f else 0.55f),
                        Color.White.copy(alpha = 0.03f),
                    ),
                ),
                shape,
            )
            .border(
                0.8.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.55f else 0.95f),
                        Color.White.copy(alpha = 0.06f),
                    ),
                ),
                shape,
            )
    } else {
        Modifier
            .shadow(8.dp, shape, clip = false)
            .background(base.copy(alpha = 0.97f), shape)
            .border(0.5.dp, scheme.outlineVariant.copy(alpha = 0.5f), shape)
    }
    Box(modifier.then(look).clip(shape)) {
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface) { content() }
    }
}

/** Round icon button on a [GlassSurface]. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    GlassSurface(modifier.size(size), CircleShape) {
        Box(
            Modifier
                .size(size)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/**
 * Apple-Music-like backdrop: the base colour with a soft glow of the accent colour at the top.
 * Drawn in one pass, no blur, so it costs almost nothing while scrolling.
 */
@Composable
fun Modifier.appBackground(): Modifier {
    val bg = MaterialTheme.colorScheme.background
    val tint = MaterialTheme.colorScheme.primary
    val dark = LocalIsDark.current
    val strength = if (dark) 0.34f else 0.24f
    return this.drawBehind {
        drawRect(bg)
        drawRect(
            Brush.radialGradient(
                colors = listOf(tint.copy(alpha = strength), Color.Transparent),
                center = Offset(size.width * 0.5f, 0f),
                radius = size.width * 1.15f,
            ),
        )
    }
}
