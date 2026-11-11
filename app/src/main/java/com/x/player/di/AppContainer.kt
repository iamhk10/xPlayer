package com.x.player.di

import android.content.Context
import androidx.room.Room
import com.x.player.data.LibraryRepository
import com.x.player.data.LyricsRepository
import com.x.player.data.MediaScanner
import com.x.player.data.db.XDatabase
import com.x.player.service.EqualizerController
import com.x.player.service.PlayerController

/** Hand-rolled dependency container. Lives as long as the process. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: XDatabase = Room.databaseBuilder(appContext, XDatabase::class.java, "x.db")
        // v1 only held the re-scannable track cache, so rebuilding it is safe. Later versions need real migrations.
        .fallbackToDestructiveMigrationFrom(1)
        .build()

    val library: LibraryRepository = LibraryRepository(database, MediaScanner(appContext))
    val player: PlayerController = PlayerController(appContext)
    val equalizer: EqualizerController = EqualizerController(appContext)
    val lyrics: LyricsRepository = LyricsRepository(appContext)
}
