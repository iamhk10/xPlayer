package com.x.player.data

import androidx.room.withTransaction
import com.x.player.data.db.XDatabase
import com.x.player.data.db.toTrack
import com.x.player.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LibraryRepository(
    private val db: XDatabase,
    private val scanner: MediaScanner,
) {
    private val dao = db.trackDao()

    val tracks: Flow<List<Track>> = dao.observeAll().map { list -> list.map { it.toTrack() } }

    /** Re-reads MediaStore and syncs the Room cache. Returns the number of tracks found. */
    suspend fun rescan(): Int = withContext(Dispatchers.IO) {
        val found = scanner.scan()
        val foundIds = found.map { it.id }.toSet()
        val removed = dao.allIds().filter { it !in foundIds }
        db.withTransaction {
            found.chunked(500).forEach { dao.upsert(it) }
            removed.chunked(500).forEach { dao.deleteByIds(it) }
        }
        found.size
    }
}
