package com.x.player.service

/** Receives listening events from [PlayerController]; implemented by the data layer. */
interface PlayRecorder {
    /** The track has now been listened to long enough to count as one play. */
    fun played(trackId: Long)

    /** [ms] milliseconds of real listening time on [trackId]. */
    fun listened(trackId: Long, ms: Long)
}
