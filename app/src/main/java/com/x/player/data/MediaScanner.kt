package com.x.player.data

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.x.player.data.db.TrackEntity
import java.io.File

/** Reads the device's music from MediaStore. Skips anything shorter than [MIN_DURATION_MS]. */
class MediaScanner(private val context: Context) {

    fun scan(minDurationMs: Long = MIN_DURATION_MS): List<TrackEntity> {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val args = arrayOf(minDurationMs.toString())

        val cursor = try {
            context.contentResolver.query(collection, projection, selection, args, null)
        } catch (e: SecurityException) {
            null
        } ?: return emptyList()

        val result = ArrayList<TrackEntity>()
        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

            while (c.moveToNext()) {
                val path = c.getString(dataCol).orEmpty()
                val file = File(path)
                result.add(
                    TrackEntity(
                        id = c.getLong(idCol),
                        title = clean(c.getString(titleCol), file.nameWithoutExtension.ifBlank { "Unknown title" }),
                        artist = clean(c.getString(artistCol), "Unknown artist"),
                        album = clean(c.getString(albumCol), "Unknown album"),
                        albumId = c.getLong(albumIdCol),
                        durationMs = c.getLong(durCol),
                        path = path,
                        folder = file.parent.orEmpty(),
                        // MediaStore stores disc*1000 + track
                        trackNumber = c.getInt(trackCol) % 1000,
                        year = c.getInt(yearCol),
                        dateAdded = c.getLong(addedCol),
                        size = c.getLong(sizeCol),
                    )
                )
            }
        }
        return result
    }

    private fun clean(value: String?, fallback: String): String {
        val v = value?.trim().orEmpty()
        return if (v.isEmpty() || v == "<unknown>") fallback else v
    }

    companion object {
        const val MIN_DURATION_MS = 30_000L
    }
}
