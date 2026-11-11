package com.x.player.service

import android.content.Context
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val minLevel: Int = -1500,
    val maxLevel: Int = 1500,
    val freqsHz: List<Int> = emptyList(),
    val levels: List<Int> = emptyList(),
    val preset: String = "Flat",
)

class EqPreset(val name: String, val shape: List<Float>)

val EQ_PRESETS = listOf(
    EqPreset("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    EqPreset("Bass boost", listOf(0.8f, 0.5f, 0f, 0f, 0f)),
    EqPreset("Treble boost", listOf(0f, 0f, 0f, 0.5f, 0.8f)),
    EqPreset("Vocal", listOf(-0.3f, 0.2f, 0.6f, 0.4f, -0.1f)),
    EqPreset("Rock", listOf(0.6f, 0.3f, -0.2f, 0.3f, 0.6f)),
    EqPreset("Pop", listOf(-0.2f, 0.3f, 0.6f, 0.3f, -0.2f)),
)

/**
 * Wraps android.media.audiofx.Equalizer. PlaybackService calls [attach] with the ExoPlayer audio
 * session id; the UI reads [state] and calls the setters. Both live in the same process.
 */
class EqualizerController(context: Context) {

    private val prefs = context.getSharedPreferences("x_eq", Context.MODE_PRIVATE)
    private var eq: Equalizer? = null
    private val _state = MutableStateFlow(EqState())
    val state: StateFlow<EqState> = _state.asStateFlow()

    fun attach(sessionId: Int) {
        if (sessionId == 0) return
        release()
        try {
            val e = Equalizer(0, sessionId)
            val bands = e.numberOfBands.toInt()
            val range = e.bandLevelRange
            val min = range[0].toInt()
            val max = range[1].toInt()
            val freqs = (0 until bands).map { e.getCenterFreq(it.toShort()) / 1000 }
            val saved = prefs.getString(KEY_LEVELS, null)?.split(",")?.mapNotNull { it.toIntOrNull() }
            val levels = if (saved != null && saved.size == bands) saved else List(bands) { 0 }
            val enabled = prefs.getBoolean(KEY_ENABLED, false)
            val preset = prefs.getString(KEY_PRESET, "Flat") ?: "Flat"

            levels.forEachIndexed { i, l -> e.setBandLevel(i.toShort(), l.coerceIn(min, max).toShort()) }
            e.setEnabled(enabled)
            eq = e
            _state.value = EqState(
                available = true,
                enabled = enabled,
                minLevel = min,
                maxLevel = max,
                freqsHz = freqs,
                levels = levels,
                preset = preset,
            )
        } catch (ex: Exception) {
            eq = null
            _state.value = EqState(available = false)
        }
    }

    fun release() {
        try {
            eq?.release()
        } catch (ex: Exception) {
            // ignore
        }
        eq = null
    }

    fun setEnabled(enabled: Boolean) {
        val e = eq ?: return
        try {
            e.setEnabled(enabled)
        } catch (ex: Exception) {
            return
        }
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _state.value = _state.value.copy(enabled = enabled)
    }

    fun setLevel(band: Int, level: Int) {
        val e = eq ?: return
        val s = _state.value
        if (band !in s.levels.indices) return
        val clamped = level.coerceIn(s.minLevel, s.maxLevel)
        try {
            e.setBandLevel(band.toShort(), clamped.toShort())
        } catch (ex: Exception) {
            return
        }
        val levels = s.levels.toMutableList().also { it[band] = clamped }
        _state.value = s.copy(levels = levels, preset = "Custom")
        save(levels, "Custom")
    }

    fun applyPreset(preset: EqPreset) {
        val e = eq ?: return
        val s = _state.value
        val bands = s.levels.size
        if (bands == 0) return
        val levels = List(bands) { i ->
            val j = if (bands == 1) 0 else Math.round(i * (preset.shape.size - 1).toFloat() / (bands - 1))
            (preset.shape[j] * s.maxLevel * 0.6f).toInt().coerceIn(s.minLevel, s.maxLevel)
        }
        try {
            levels.forEachIndexed { i, l -> e.setBandLevel(i.toShort(), l.toShort()) }
        } catch (ex: Exception) {
            return
        }
        _state.value = s.copy(levels = levels, preset = preset.name)
        save(levels, preset.name)
    }

    private fun save(levels: List<Int>, preset: String) {
        prefs.edit()
            .putString(KEY_LEVELS, levels.joinToString(","))
            .putString(KEY_PRESET, preset)
            .apply()
    }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_LEVELS = "levels"
        const val KEY_PRESET = "preset"
    }
}
