package com.taurotechnologies.godseye.nativeapp.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Native overlays that sit above the screen stack. Add a case here before opening a new kind of overlay. */
enum class NativeOverlay { ALERT_LIST, ALERT_DETAIL }

/**
 * Single back stack for overlays, mirroring the web OverlayHistoryManager ordering: the newest overlay
 * closes first, opening an overlay that is already open does nothing, and a control close removes only
 * that overlay. When [back] returns null the caller falls through to screen navigation.
 */
class OverlayBackStack<T : Any> {
    private val mutableStack = MutableStateFlow<List<T>>(emptyList())

    /** Open overlays, oldest first. The last element is the one on screen. */
    val stack: StateFlow<List<T>> = mutableStack.asStateFlow()

    fun top(): T? = mutableStack.value.lastOrNull()

    fun contains(id: T): Boolean = id in mutableStack.value

    /** Returns true when the overlay was added; false when it was already open. */
    fun open(id: T): Boolean {
        if (contains(id)) return false
        mutableStack.value = mutableStack.value + id
        return true
    }

    /** Closes [id] from a control (not from Back). Other overlays stay open. */
    fun close(id: T) {
        mutableStack.value = mutableStack.value.filterNot { it == id }
    }

    /** Handles the system Back gesture: removes and returns the newest overlay, or null when none is open. */
    fun back(): T? {
        val current = mutableStack.value
        val closing = current.lastOrNull() ?: return null
        mutableStack.value = current.dropLast(1)
        return closing
    }

    fun clear() {
        mutableStack.value = emptyList()
    }
}
