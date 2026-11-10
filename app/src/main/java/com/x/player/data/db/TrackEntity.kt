package com.x.player.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.x.player.domain.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
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
)

fun TrackEntity.toTrack(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    path = path,
    folder = folder,
    trackNumber = trackNumber,
    year = year,
    dateAdded = dateAdded,
    size = size,
)
