package com.x.player.service

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

data class PlayerState(
    val connected: Boolean = false,
    val current: MediaItem? = null,
    val queue: List<MediaItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
) {
    val hasMedia: Boolean get() = current != null
    val currentId: String? get() = current?.mediaId
    val title: String get() = current?.mediaMetadata?.title?.toString().orEmpty()
    val artist: String get() = current?.mediaMetadata?.artist?.toString().orEmpty()
    val album: String get() = current?.mediaMetadata?.albumTitle?.toString().orEmpty()
    val artworkUri: Uri? get() = current?.mediaMetadata?.artworkUri
}
