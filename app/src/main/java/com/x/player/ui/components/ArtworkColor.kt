package com.x.player.ui.components

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Picks a vivid colour from the artwork, or null while loading / when there is no art. */
@Composable
fun rememberArtworkColor(uri: Uri?): Color? {
    val context = LocalContext.current
    var color by remember(uri) { mutableStateOf<Color?>(null) }
    LaunchedEffect(uri) {
        color = null
        if (uri == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(128)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)
        val bitmap = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
        if (bitmap != null) {
            val rgb = withContext(Dispatchers.Default) {
                val palette = Palette.from(bitmap).maximumColorCount(16).generate()
                palette.getVibrantColor(palette.getMutedColor(palette.getDominantColor(0)))
            }
            if (rgb != 0) color = Color(rgb)
        }
    }
    return color
}
