package com.x.player.domain

data class Playlist(
    val id: Long,
    val name: String,
    val trackIds: List<Long>,
)

/** Pseudo playlist id used by the navigation route for the Favourites list. */
const val FAVORITES_ID = -1L
