package com.x.player.di

import android.content.Context
import androidx.room.Room
import com.x.player.data.LibraryRepository
import com.x.player.data.MediaScanner
import com.x.player.data.db.XDatabase
import com.x.player.service.PlayerController

/** Hand-rolled dependency container. Lives as long as the process. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: XDatabase = Room.databaseBuilder(appContext, XDatabase::class.java, "x.db")
        .fallbackToDestructiveMigration()
        .build()

    val library: LibraryRepository = LibraryRepository(database, MediaScanner(appContext))
    val player: PlayerController = PlayerController(appContext)
}
