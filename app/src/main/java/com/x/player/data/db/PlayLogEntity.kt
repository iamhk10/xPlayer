package com.x.player.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per event. A "play" row has playCount = 1 and listenedMs = 0; a "listening" row has
 * playCount = 0 and the milliseconds listened since the last flush.
 */
@Entity(tableName = "play_log")
data class PlayLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: Long,
    val playedAt: Long,
    val playCount: Int,
    val listenedMs: Long,
)

data class StatsRow(val plays: Int, val ms: Long)

data class TopRow(val trackId: Long, val plays: Int)
