package com.taurotechnologies.godseye.nativeapp.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Persist event snapshots so a saved live event remains available after it leaves the feed. */
object SavedRadarSignals {
    private const val PREFS_NAME = "saved_radar_signals"
    private const val KEY_SNAPSHOTS = "snapshots"
    private val json = Json { ignoreUnknownKeys = true }
    private val mutableSignals = MutableStateFlow<List<RadarSignal>>(emptyList())
    val signals: StateFlow<List<RadarSignal>> = mutableSignals.asStateFlow()
    private val mutableIds = MutableStateFlow<Set<String>>(emptySet())
    val ids: StateFlow<Set<String>> = mutableIds.asStateFlow()
    @Volatile private var initialized = false

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_SNAPSHOTS, null)
        val stored = if (raw != null) runCatching { json.decodeFromString<List<RadarSignal>>(raw) }.getOrDefault(emptyList())
            else MockRadarApi.signals().filter { it.id in prefs.getStringSet("signal_ids", emptySet()).orEmpty() }
        publish(stored)
        initialized = true
    }

    @Synchronized
    fun toggle(context: Context, signal: RadarSignal) {
        initialize(context)
        val current = mutableSignals.value
        val updated = if (current.any { it.id == signal.id }) current.filterNot { it.id == signal.id } else current + signal
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SNAPSHOTS, json.encodeToString(updated)).remove("signal_ids").apply()
        publish(updated)
    }

    private fun publish(signals: List<RadarSignal>) {
        mutableSignals.value = signals.sortedByDescending { it.observedAt }
        mutableIds.value = signals.map { it.id }.toSet()
    }
}
