package com.x.player.data

import androidx.room.withTransaction
import com.x.player.data.db.FavoriteEntity
import com.x.player.data.db.PlaylistEntity
import com.x.player.data.db.PlaylistTrackEntity
import com.x.player.data.db.XDatabase
import com.x.player.data.db.toTrack
import com.x.player.domain.Playlist
import com.x.player.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LibraryRepository(
    private val db: XDatabase,
    private val scanner: MediaScanner,
) {
    private val dao = db.trackDao()
    private val pdao = db.playlistDao()

    val tracks: Flow<List<Track>> = dao.observeAll().map { list -> list.map { it.toTrack() } }

    val favoriteIds: Flow<List<Long>> = pdao.observeFavoriteIds()

    val playlists: Flow<List<Playlist>> =
        combine(pdao.observePlaylists(), pdao.observePlaylistTracks()) { lists, links ->
            val grouped = links.groupBy({ it.playlistId }, { it.trackId })
            lists.map { Playlist(it.id, it.name, grouped[it.id].orEmpty()) }
        }

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

    suspend fun toggleFavorite(trackId: Long) {
        if (pdao.favoriteCount(trackId) > 0) {
            pdao.removeFavorite(trackId)
        } else {
            pdao.addFavorite(FavoriteEntity(trackId, System.currentTimeMillis()))
        }
    }

    suspend fun createPlaylist(name: String, firstTrackId: Long?): Long {
        val id = pdao.insertPlaylist(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))
        if (firstTrackId != null) {
            pdao.addToPlaylist(PlaylistTrackEntity(id, firstTrackId, System.currentTimeMillis()))
        }
        return id
    }

    suspend fun renamePlaylist(id: Long, name: String) = pdao.renamePlaylist(id, name)

    suspend fun deletePlaylist(id: Long) {
        db.withTransaction {
            pdao.clearPlaylist(id)
            pdao.deletePlaylist(id)
        }
    }

    suspend fun addToPlaylist(playlistId: Long, trackId: Long) =
        pdao.addToPlaylist(PlaylistTrackEntity(playlistId, trackId, System.currentTimeMillis()))

    suspend fun removeFromPlaylist(playlistId: Long, trackId: Long) =
        pdao.removeFromPlaylist(playlistId, trackId)
}
