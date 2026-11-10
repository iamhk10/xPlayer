package com.x.player.domain

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

private val ALBUM_ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")

/**
 * Android 10+ can only load album art through ContentResolver.loadThumbnail() on the album URI
 * (handled by AlbumArtFetcher / AlbumBitmapLoader). Older versions use the legacy albumart URI.
 */
fun albumArtUri(albumId: Long): Uri =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
    } else {
        ContentUris.withAppendedId(ALBUM_ART_BASE, albumId)
    }

fun isAlbumUri(uri: Uri): Boolean {
    val seg = uri.pathSegments
    return uri.scheme == "content" && seg.size >= 3 && seg[seg.size - 2] == "albums" && seg[seg.size - 3] == "audio"
}

/** Stable key used to group tracks by artist name (case-insensitive). */
fun artistKey(name: String): Int = name.trim().lowercase().hashCode()

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val path: String,
    val folder: String,
    val trackNumber: Int,
    val year: Int,
    val dateAdded: Long,
    val size: Long,
) {
    val uri: Uri get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    val artworkUri: Uri get() = albumArtUri(albumId)
    val artistKey: Int get() = com.x.player.domain.artistKey(artist)
}

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val trackCount: Int,
    val year: Int,
) {
    val artworkUri: Uri get() = albumArtUri(id)
}

data class Artist(
    val key: Int,
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
)

data class Folder(
    val key: Int,
    val path: String,
    val name: String,
    val trackCount: Int,
)

enum class SongSort(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    RECENT("Recently added"),
}

fun List<Track>.applySort(sort: SongSort): List<Track> = when (sort) {
    SongSort.TITLE -> sortedBy { it.title.lowercase() }
    SongSort.ARTIST -> sortedWith(compareBy<Track>({ it.artist.lowercase() }, { it.album.lowercase() }, { it.trackNumber }))
    SongSort.ALBUM -> sortedWith(compareBy<Track>({ it.album.lowercase() }, { it.trackNumber }, { it.title.lowercase() }))
    SongSort.RECENT -> sortedByDescending { it.dateAdded }
}

data class SearchResults(
    val songs: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
}
