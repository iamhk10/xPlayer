package com.x.player.data

import android.content.Context
import android.net.Uri
import com.x.player.util.LyricLine
import com.x.player.util.parseLrc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Finds synced lyrics for a track. Order: a file the user imported (stored privately per track),
 * then a `.lrc` next to the audio file when the system lets us read it.
 */
class LyricsRepository(private val context: Context) {

    private val dir = File(context.filesDir, "lyrics").apply { mkdirs() }

    suspend fun load(trackId: Long, audioPath: String): List<LyricLine> = withContext(Dispatchers.IO) {
        val imported = File(dir, "$trackId.lrc")
        val text: String? = try {
            if (imported.exists()) {
                imported.readText()
            } else if (audioPath.contains('.')) {
                val sidecar = File(audioPath.substringBeforeLast('.') + ".lrc")
                if (sidecar.canRead()) sidecar.readText() else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
        if (text == null) emptyList() else parseLrc(text)
    }

    /** Copies a user-picked .lrc into app storage. Returns false if it holds no synced lines. */
    suspend fun import(trackId: Long, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (text != null && parseLrc(text).isNotEmpty()) {
                File(dir, "$trackId.lrc").writeText(text)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
