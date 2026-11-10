package com.x.player.domain

import java.io.File

/** Immutable, pre-grouped view of the whole library. Built off the main thread. */
class LibraryIndex(val tracks: List<Track> = emptyList()) {

    private val byAlbum: Map<Long, List<Track>> = tracks
        .groupBy { it.albumId }
        .mapValues { (_, list) ->
            list.sortedWith(
                compareBy<Track>({ it.trackNumber == 0 }, { it.trackNumber }, { it.title.lowercase() })
            )
        }

    private val byArtist: Map<Int, List<Track>> = tracks.groupBy { it.artistKey }

    private val byFolder: Map<Int, List<Track>> = tracks.groupBy { it.folder.hashCode() }

    val albums: List<Album> = byAlbum.map { (id, list) ->
        val artists = list.map { it.artist }.distinct()
        Album(
            id = id,
            name = list.first().album,
            artist = if (artists.size > 1) "Various artists" else artists.first(),
            trackCount = list.size,
            year = list.maxOf { it.year },
        )
    }.sortedBy { it.name.lowercase() }

    val artists: List<Artist> = byArtist.map { (key, list) ->
        Artist(
            key = key,
            name = list.first().artist,
            trackCount = list.size,
            albumCount = list.map { it.albumId }.distinct().size,
        )
    }.sortedBy { it.name.lowercase() }

    val folders: List<Folder> = byFolder.map { (key, list) ->
        val path = list.first().folder
        Folder(
            key = key,
            path = path,
            name = File(path).name.ifBlank { "Unknown folder" },
            trackCount = list.size,
        )
    }.sortedBy { it.name.lowercase() }

    fun album(id: Long): Album? = albums.firstOrNull { it.id == id }
    fun artist(key: Int): Artist? = artists.firstOrNull { it.key == key }
    fun folder(key: Int): Folder? = folders.firstOrNull { it.key == key }

    fun albumTracks(id: Long): List<Track> = byAlbum[id].orEmpty()
    fun artistTracks(key: Int): List<Track> =
        byArtist[key].orEmpty().sortedWith(compareBy<Track>({ it.album.lowercase() }, { it.trackNumber }, { it.title.lowercase() }))
    fun folderTracks(key: Int): List<Track> =
        byFolder[key].orEmpty().sortedBy { it.title.lowercase() }

    fun search(query: String): SearchResults {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return SearchResults()
        return SearchResults(
            songs = tracks.filter {
                it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
            }.take(200),
            albums = albums.filter { it.name.lowercase().contains(q) || it.artist.lowercase().contains(q) }.take(50),
            artists = artists.filter { it.name.lowercase().contains(q) }.take(50),
        )
    }
}
