package com.x.player.domain

const val KIND_PLAYLIST = 0
const val KIND_FOLDER = 1

data class Playlist(
    val id: Long,
    val name: String,
    val trackIds: List<Long>,
    val kind: Int = KIND_PLAYLIST,
)

/** Pseudo playlist id used by the navigation route for the Favourites list. */
const val FAVORITES_ID = -1L
