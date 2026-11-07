package com.taurotechnologies.godseye.nativeapp.notifications

/** Inbox contents plus the ids the user dismissed, so a later push cannot bring them back. */
data class AlertInboxState(
    val items: List<NativeAlert> = emptyList(),
    val dismissedIds: List<String> = emptyList(),
) {
    fun unreadCount(nowMillis: Long): Int = AlertInboxRules.visible(this, nowMillis).count { it.readAtMillis == null }
}

/** Pure inbox transitions, ported from NotificationStore in src/services/notification-store.ts. */
object AlertInboxRules {
    const val MAX_ITEMS: Int = 100
    const val MAX_DISMISSED: Int = 500

    /** Items inside the retention window, newest first. */
    fun visible(state: AlertInboxState, nowMillis: Long): List<NativeAlert> = state.items
        .filter { it.createdAtMillis >= nowMillis - AlertPayload.RETENTION_MILLIS }
        .sortedByDescending { it.createdAtMillis }

    /**
     * Adds or refreshes an alert. Returns the state unchanged when the id was dismissed or the
     * same content is already stored, so a repeated tap or feed refresh never creates a second unread item.
     */
    fun add(state: AlertInboxState, alert: NativeAlert, nowMillis: Long): AlertInboxState {
        if (alert.id in state.dismissedIds) return state
        val existing = state.items.firstOrNull { it.id == alert.id }
        val updated: List<NativeAlert> = when {
            existing == null -> listOf(alert) + state.items
            existing.title == alert.title && existing.body == alert.body && existing.severity == alert.severity -> return state
            else -> state.items.map { if (it.id == alert.id) alert.copy(readAtMillis = existing.readAtMillis) else it }
        }
        return state.copy(items = visible(state.copy(items = updated), nowMillis).take(MAX_ITEMS))
    }

    fun markRead(state: AlertInboxState, id: String, nowMillis: Long): AlertInboxState = state.copy(
        items = state.items.map { if (it.id == id && it.readAtMillis == null) it.copy(readAtMillis = nowMillis) else it },
    )

    fun markAllRead(state: AlertInboxState, nowMillis: Long): AlertInboxState = state.copy(
        items = state.items.map { if (it.readAtMillis == null) it.copy(readAtMillis = nowMillis) else it },
    )

    fun dismiss(state: AlertInboxState, id: String): AlertInboxState = state.copy(
        items = state.items.filterNot { it.id == id },
        dismissedIds = (state.dismissedIds + id).distinct().takeLast(MAX_DISMISSED),
    )

    fun clearRead(state: AlertInboxState): AlertInboxState {
        val readIds = state.items.filter { it.readAtMillis != null }.map { it.id }
        return state.copy(
            items = state.items.filter { it.readAtMillis == null },
            dismissedIds = (state.dismissedIds + readIds).distinct().takeLast(MAX_DISMISSED),
        )
    }
}
