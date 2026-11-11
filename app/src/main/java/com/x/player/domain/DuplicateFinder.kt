package com.x.player.domain

/**
 * Groups songs that look like the same recording: same title and artist (ignoring case) and a
 * length within 2 seconds. Inside each group the largest file comes first (the one to keep).
 */
fun findDuplicates(tracks: List<Track>): List<List<Track>> {
    val result = ArrayList<List<Track>>()
    val byName = tracks.groupBy { it.title.trim().lowercase() + "|" + it.artist.trim().lowercase() }
    for (group in byName.values) {
        if (group.size < 2) continue
        val sorted = group.sortedBy { it.durationMs }
        var cluster = ArrayList<Track>()
        for (track in sorted) {
            if (cluster.isNotEmpty() && track.durationMs - cluster.last().durationMs > 2_000L) {
                if (cluster.size > 1) result.add(cluster)
                cluster = ArrayList()
            }
            cluster.add(track)
        }
        if (cluster.size > 1) result.add(cluster)
    }
    return result
        .map { cluster -> cluster.sortedByDescending { it.size } }
        .sortedBy { it.first().title.lowercase() }
}
