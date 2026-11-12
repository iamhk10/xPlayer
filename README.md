# xPlayer — offline local music player

Kotlin + Jetpack Compose (Material 3) music player. Plays only the music on your phone. No `INTERNET` permission.

## Phase 1 (this build)
- Library scan from MediaStore (tracks under 30 s are skipped), cached in Room
- Songs / Albums / Artists / Folders tabs, sort, search
- Media3 playback service: background play, notification, lock-screen and Bluetooth controls, gapless
- Now Playing screen, mini player, queue (play next, add to queue, remove)
- Shuffle and repeat (off / all / one)

## Build the APK with GitHub Actions
1. Create a new empty GitHub repo.
2. Push this folder to it (branch `main`).
3. Open the repo → **Actions** → *Build xPlayer APK* (it also runs on every push).
4. When it is green, open the run and download the artifact **xPlayer-debug-apk** → unzip → `xPlayer.apk`.
5. Copy `xPlayer.apk` to your phone and install it (allow "install unknown apps").

If the build fails, open the failed step, copy the red error lines and send them back.

## Layout
```
app/src/main/java/com/x/player/
  data/      MediaScanner, LibraryRepository, Room (db/)
  domain/    models, LibraryIndex (grouping and search)
  service/   PlaybackService, PlayerController, PlayerState
  di/        AppContainer (manual DI)
  ui/        theme, components, screens, XRoot (navigation)
  util/      formatting helpers
```

## Next phases
- Phase 2: playlists, favourites, 5-band equaliser, sleep timer, palette theming, folder explorer extras
- Phase 3: synced .lrc lyrics, vinyl/cassette now-playing, duplicate finder, ringtone cutter, biometric vault, crossfade
