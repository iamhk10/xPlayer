package com.x.player.util

data class LyricLine(val timeMs: Long, val text: String)

private val LRC_TAG = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?\]""")

/** Parses standard synced lyrics: `[mm:ss.xx] text`. Lines may carry several time tags. */
fun parseLrc(text: String): List<LyricLine> {
    val out = ArrayList<LyricLine>()
    for (raw in text.lineSequence()) {
        val line = raw.trim().removePrefix("﻿")
        val tags = LRC_TAG.findAll(line).toList()
        if (tags.isEmpty()) continue
        val lyric = line.substring(tags.last().range.last + 1).trim()
        for (m in tags) {
            val minutes = m.groupValues[1].toLong()
            val seconds = m.groupValues[2].toLong()
            val frac = m.groupValues[3]
            val millis = when (frac.length) {
                0 -> 0L
                1 -> frac.toLong() * 100
                2 -> frac.toLong() * 10
                else -> frac.take(3).toLong()
            }
            out.add(LyricLine(minutes * 60_000L + seconds * 1_000L + millis, lyric))
        }
    }
    return out.sortedBy { it.timeMs }
}
