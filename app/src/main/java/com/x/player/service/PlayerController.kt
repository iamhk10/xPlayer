package com.x.player.service

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.x.player.domain.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UI-facing wrapper around a Media3 [MediaController] connected to [PlaybackService].
 * All calls must happen on the main thread.
 */
class PlayerController(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var future: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var ticker: Job? = null
    private val pending = ArrayList<(MediaController) -> Unit>()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh()
            updateTicker()
        }
    }

    fun connect() {
        if (future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener({
            val c = try {
                f.get()
            } catch (e: Exception) {
                future = null
                return@addListener
            }
            controller = c
            c.addListener(listener)
            pending.forEach { it(c) }
            pending.clear()
            refresh()
            updateTicker()
        }, ContextCompat.getMainExecutor(context))
    }

    fun disconnect() {
        ticker?.cancel()
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        future = null
        controller = null
        _state.value = PlayerState()
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else {
            pending.add(block)
            connect()
        }
    }

    private fun refresh() {
        val c = controller ?: return
        val items = ArrayList<MediaItem>(c.mediaItemCount)
        for (i in 0 until c.mediaItemCount) items.add(c.getMediaItemAt(i))
        val extraDuration = c.currentMediaItem?.mediaMetadata?.extras?.getLong(EXTRA_DURATION, 0L) ?: 0L
        val duration = if (c.duration > 0) c.duration else extraDuration
        _state.value = PlayerState(
            connected = true,
            current = c.currentMediaItem,
            queue = items,
            currentIndex = c.currentMediaItemIndex,
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            durationMs = duration.coerceAtLeast(0L),
            shuffle = c.shuffleModeEnabled,
            repeat = c.repeatMode,
        )
    }

    private fun updateTicker() {
        val playing = controller?.isPlaying == true
        if (playing && ticker?.isActive != true) {
            ticker = scope.launch {
                while (isActive) {
                    val c = controller ?: break
                    _state.value = _state.value.copy(positionMs = c.currentPosition.coerceAtLeast(0L))
                    delay(400)
                }
            }
        } else if (!playing) {
            ticker?.cancel()
            ticker = null
        }
    }

    // ---- commands -------------------------------------------------------------------------

    fun play(tracks: List<Track>, startIndex: Int, shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val items = tracks.map { it.toMediaItem() }
        withController { c ->
            c.shuffleModeEnabled = shuffle
            c.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    fun shuffleAll(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        play(tracks, tracks.indices.random(), shuffle = true)
    }

    fun playNext(track: Track) = withController { c ->
        val index = if (c.mediaItemCount == 0) 0 else c.currentMediaItemIndex + 1
        c.addMediaItem(index, track.toMediaItem())
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
    }

    fun addToQueue(track: Track) = withController { c ->
        c.addMediaItem(track.toMediaItem())
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
    }

    fun togglePlayPause() = withController { c ->
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = withController { it.seekToNextMediaItem() }

    fun previous() = withController { c ->
        // Behaves like most players: restart the song first, go back only near its start.
        if (c.currentPosition > 3_000L) c.seekTo(0L) else c.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) = withController { c ->
        c.seekTo(positionMs)
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    fun skipTo(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) {
            c.seekTo(index, 0L)
            c.play()
        }
    }

    fun removeFromQueue(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}
