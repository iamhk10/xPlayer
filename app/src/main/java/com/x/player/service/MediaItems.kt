package com.x.player.service

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.x.player.domain.Track

const val EXTRA_DURATION = "x_duration_ms"
const val EXTRA_PATH = "x_path"
const val EXTRA_SIZE = "x_size"

fun Track.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putLong(EXTRA_DURATION, durationMs)
        putString(EXTRA_PATH, path)
        putLong(EXTRA_SIZE, size)
    }
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artworkUri)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .setExtras(extras)
                .build()
        )
        .build()
}
