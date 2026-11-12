package com.x.player.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.x.player.data.LibraryRepository
import com.x.player.data.LyricsRepository
import com.x.player.data.MediaScanner
import com.x.player.data.db.XDatabase
import com.x.player.service.EqualizerController
import com.x.player.service.PlayRecorder
import com.x.player.service.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** v2 -> v3: adds the listening log. Existing favourites and playlists are untouched. */
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS play_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "trackId INTEGER NOT NULL, " +
                "playedAt INTEGER NOT NULL, " +
                "playCount INTEGER NOT NULL, " +
                "listenedMs INTEGER NOT NULL)"
        )
    }
}

/** Hand-rolled dependency container. Lives as long as the process. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: XDatabase = Room.databaseBuilder(appContext, XDatabase::class.java, "x.db")
        // v1 only held the re-scannable track cache, so rebuilding it is safe. Later versions need real migrations.
        .fallbackToDestructiveMigrationFrom(1)
        .addMigrations(MIGRATION_2_3)
        .build()

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val library: LibraryRepository = LibraryRepository(database, MediaScanner(appContext))

    private val recorder = object : PlayRecorder {
        override fun played(trackId: Long) {
            ioScope.launch { library.logPlay(trackId) }
        }

        override fun listened(trackId: Long, ms: Long) {
            ioScope.launch { library.logListening(trackId, ms) }
        }
    }

    val player: PlayerController = PlayerController(appContext, recorder)
    val equalizer: EqualizerController = EqualizerController(appContext)
    val lyrics: LyricsRepository = LyricsRepository(appContext)
}
