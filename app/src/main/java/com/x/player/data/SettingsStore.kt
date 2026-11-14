package com.x.player.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED black"),
}

/** Everything the user can change in Settings and Theme. Immutable; replaced as a whole on change. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Use the phone's wallpaper colours (Android 12+). Ignored on older versions. */
    val dynamicColor: Boolean = false,
    /** Index into the accent palette list in Theme.kt. Used when neither dynamic nor art colour applies. */
    val accentIndex: Int = 0,
    /** Tint the app with the colour of the artwork that is playing. */
    val adaptToArt: Boolean = true,
    /** Translucent "liquid glass" look for the nav bar, mini player and cards. */
    val glass: Boolean = false,
    val showRecent: Boolean = true,
    val showMostPlayed: Boolean = true,
    val showFavorites: Boolean = true,
    val showPlaylists: Boolean = true,
    val showAlbums: Boolean = true,
    val showArtists: Boolean = true,
)

/** Small SharedPreferences-backed store. Reads are in-memory through [settings]. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("xplayer_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings {
        val d = AppSettings()
        val mode = prefs.getString("themeMode", d.themeMode.name)
        return AppSettings(
            themeMode = ThemeMode.values().firstOrNull { it.name == mode } ?: d.themeMode,
            dynamicColor = prefs.getBoolean("dynamicColor", d.dynamicColor),
            accentIndex = prefs.getInt("accentIndex", d.accentIndex),
            adaptToArt = prefs.getBoolean("adaptToArt", d.adaptToArt),
            glass = prefs.getBoolean("glass", d.glass),
            showRecent = prefs.getBoolean("showRecent", d.showRecent),
            showMostPlayed = prefs.getBoolean("showMostPlayed", d.showMostPlayed),
            showFavorites = prefs.getBoolean("showFavorites", d.showFavorites),
            showPlaylists = prefs.getBoolean("showPlaylists", d.showPlaylists),
            showAlbums = prefs.getBoolean("showAlbums", d.showAlbums),
            showArtists = prefs.getBoolean("showArtists", d.showArtists),
        )
    }

    fun update(change: (AppSettings) -> AppSettings) {
        val s = change(_settings.value)
        _settings.value = s
        prefs.edit()
            .putString("themeMode", s.themeMode.name)
            .putBoolean("dynamicColor", s.dynamicColor)
            .putInt("accentIndex", s.accentIndex)
            .putBoolean("adaptToArt", s.adaptToArt)
            .putBoolean("glass", s.glass)
            .putBoolean("showRecent", s.showRecent)
            .putBoolean("showMostPlayed", s.showMostPlayed)
            .putBoolean("showFavorites", s.showFavorites)
            .putBoolean("showPlaylists", s.showPlaylists)
            .putBoolean("showAlbums", s.showAlbums)
            .putBoolean("showArtists", s.showArtists)
            .apply()
    }
}
