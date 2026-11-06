package com.just_sri.suvidha.radar.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Device-local saved signal IDs shared by the radar sheet and Settings. */
object SavedRadarSignals {
    private const val PREFS_NAME = "saved_radar_signals"
    private const val KEY_IDS = "signal_ids"

    private val mutableIds = MutableStateFlow<Set<String>>(emptySet())
    val ids: StateFlow<Set<String>> = mutableIds.asStateFlow()
    @Volatile private var initialized = false

    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        val stored = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_IDS, emptySet())
            .orEmpty()
            .toSet()
        mutableIds.value = stored
        initialized = true
    }

    @Synchronized
    fun toggle(context: Context, signalId: String) {
        initialize(context)
        val updated = mutableIds.value.toMutableSet().apply {
            if (!add(signalId)) remove(signalId)
        }.toSet()
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_IDS, updated)
            .apply()
        mutableIds.value = updated
    }
}
